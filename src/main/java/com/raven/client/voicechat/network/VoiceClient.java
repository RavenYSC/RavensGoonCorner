package com.raven.client.voicechat.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raven.client.voicechat.VoiceChatManager;
import com.raven.client.voicechat.model.VoiceRoom;
import com.raven.client.voicechat.model.VoiceUser;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles network communication with the voice server.
 * Uses WebSocket for control messages and UDP for audio.
 */
public class VoiceClient {
    
    private final VoiceChatManager manager;
    private final Gson gson = new Gson();
    
    // WebSocket connection (simulated with TCP socket for simplicity)
    private Socket controlSocket;
    private BufferedReader controlIn;
    private PrintWriter controlOut;
    
    // UDP connection for audio
    private DatagramSocket audioSocket;
    private InetAddress serverAddress;
    private int serverUdpPort;
    
    // Threading
    private ExecutorService executor;
    private ScheduledExecutorService heartbeatScheduler;
    private ScheduledFuture<?> heartbeatTask;
    private AtomicBoolean running = new AtomicBoolean(false);
    
    // Heartbeat interval (20 seconds)
    private static final int HEARTBEAT_INTERVAL = 20000;
    
    // Session
    private String sessionId;
    private UUID localUserId;
    
    // Packet types
    private static final byte PACKET_AUDIO = 0x01;
    private static final byte PACKET_TALKING_START = 0x02;
    private static final byte PACKET_TALKING_STOP = 0x03;
    
    public VoiceClient(VoiceChatManager manager) {
        this.manager = manager;
    }
    
    /**
     * Connect to the voice server
     */
    public void connect(String host, int wsPort, int udpPort, String authToken) {
        if (running.get()) {
            disconnect();
        }
        
        executor = Executors.newFixedThreadPool(3);
        
        executor.submit(() -> {
            try {
                // Get local player UUID
                if (Minecraft.getMinecraft().thePlayer != null) {
                    localUserId = Minecraft.getMinecraft().thePlayer.getUniqueID();
                } else {
                    localUserId = UUID.randomUUID();
                }
                
                // Connect control socket (TCP)
                serverAddress = InetAddress.getByName(host);
                controlSocket = new Socket(serverAddress, wsPort);
                controlSocket.setSoTimeout(0); // No timeout - we use heartbeats
                controlSocket.setKeepAlive(true);
                controlIn = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
                controlOut = new PrintWriter(controlSocket.getOutputStream(), true);
                
                // Connect UDP socket
                audioSocket = new DatagramSocket();
                audioSocket.setSoTimeout(100); // Short timeout for non-blocking receives
                serverUdpPort = udpPort;
                
                running.set(true);
                
                // Send authentication
                sendAuth(authToken);
                
                // Start receive threads
                executor.submit(this::controlReceiveLoop);
                executor.submit(this::audioReceiveLoop);
                
                // Start heartbeat
                startHeartbeat();
                
                manager.onConnectionStateChanged(true);
                
            } catch (Exception e) {
                System.err.println("[VoiceChat] Failed to connect: " + e.getMessage());
                e.printStackTrace();
                disconnect();
            }
        });
    }
    
    /**
     * Disconnect from the voice server
     */
    public void disconnect() {
        running.set(false);
        
        // Stop heartbeat
        stopHeartbeat();
        
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                sendControl("disconnect", new JsonObject());
                controlSocket.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        try {
            if (audioSocket != null && !audioSocket.isClosed()) {
                audioSocket.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        if (executor != null) {
            executor.shutdownNow();
        }
        
        controlSocket = null;
        audioSocket = null;
        sessionId = null;
        
        manager.onConnectionStateChanged(false);
    }
    
    /**
     * Send authentication to server
     */
    private void sendAuth(String authToken) {
        JsonObject data = new JsonObject();
        data.addProperty("token", authToken);
        data.addProperty("uuid", localUserId.toString());
        
        String mcName = Minecraft.getMinecraft().thePlayer != null ? 
                        Minecraft.getMinecraft().thePlayer.getName() : "Unknown";
        data.addProperty("minecraft_name", mcName);
        
        if (manager.getDisplayNickname() != null) {
            data.addProperty("nickname", manager.getDisplayNickname());
        }
        
        sendControl("auth", data);
    }
    
    /**
     * Send a control message to the server
     */
    private void sendControl(String type, JsonObject data) {
        if (controlOut == null) {
            return;
        }
        
        try {
            JsonObject message = new JsonObject();
            message.addProperty("type", type);
            message.add("data", data);
            
            controlOut.println(gson.toJson(message));
        } catch (Exception e) {
            System.err.println("[VoiceChat] Failed to send control message: " + e.getMessage());
        }
    }
    
    /**
     * Start heartbeat to keep connection alive
     */
    private void startHeartbeat() {
        if (heartbeatScheduler == null) {
            heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (running.get() && controlOut != null) {
                sendControl("heartbeat", new JsonObject());
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        
        System.out.println("[VoiceChat] Heartbeat started (every " + (HEARTBEAT_INTERVAL / 1000) + "s)");
    }
    
    /**
     * Stop heartbeat
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
    }
    
    /**
     * Join a room
     */
    public void sendJoinRoom(String roomId) {
        JsonObject data = new JsonObject();
        data.addProperty("room_id", roomId);
        sendControl("join_room", data);
    }
    
    /**
     * Create a new room
     */
    public void sendCreateRoom(String roomName, boolean isPrivate) {
        JsonObject data = new JsonObject();
        data.addProperty("name", roomName);
        data.addProperty("private", isPrivate);
        sendControl("create_room", data);
    }
    
    /**
     * Leave current room
     */
    public void sendLeaveRoom() {
        sendControl("leave_room", new JsonObject());
    }
    
    /**
     * Send mute state
     */
    public void sendMuteState(boolean muted) {
        JsonObject data = new JsonObject();
        data.addProperty("muted", muted);
        sendControl("mute_state", data);
    }
    
    /**
     * Send deafen state
     */
    public void sendDeafenState(boolean deafened) {
        JsonObject data = new JsonObject();
        data.addProperty("deafened", deafened);
        sendControl("deafen_state", data);
    }
    
    /**
     * Send encoded audio data
     */
    public void sendAudio(byte[] opusData) {
        if (audioSocket == null || sessionId == null) {
            return;
        }
        
        try {
            // Packet format: [type(1)] [session_id(36)] [audio_data(n)]
            ByteBuffer buffer = ByteBuffer.allocate(1 + 36 + opusData.length);
            buffer.put(PACKET_AUDIO);
            buffer.put(sessionId.getBytes());
            buffer.put(opusData);
            
            byte[] packet = buffer.array();
            DatagramPacket datagram = new DatagramPacket(packet, packet.length, serverAddress, serverUdpPort);
            audioSocket.send(datagram);
            
        } catch (Exception e) {
            System.err.println("[VoiceChat] Failed to send audio: " + e.getMessage());
        }
    }
    
    /**
     * Control message receive loop
     */
    private void controlReceiveLoop() {
        while (running.get()) {
            try {
                String line = controlIn.readLine();
                if (line == null) {
                    // Connection closed
                    if (running.get()) {
                        System.err.println("[VoiceChat] Control connection closed by server");
                        disconnect();
                    }
                    break;
                }
                
                handleControlMessage(line);
                
            } catch (SocketTimeoutException e) {
                // Normal timeout, continue
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[VoiceChat] Control receive error: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Handle incoming control message
     */
    private void handleControlMessage(String json) {
        try {
            JsonObject message = new JsonParser().parse(json).getAsJsonObject();
            String type = message.get("type").getAsString();
            
            switch (type) {
                case "connected":
                    // Initial connection established, session ID received
                    if (message.has("sessionId")) {
                        sessionId = message.get("sessionId").getAsString();
                        System.out.println("[VoiceChat] Session established: " + sessionId);
                    }
                    break;
                    
                case "auth_response":
                    boolean authSuccess = message.has("success") && message.get("success").getAsBoolean();
                    if (authSuccess) {
                        if (message.has("sessionId")) {
                            sessionId = message.get("sessionId").getAsString();
                        }
                        if (message.has("discordName")) {
                            manager.setVerifiedDiscordName(message.get("discordName").getAsString());
                        }
                        System.out.println("[VoiceChat] Authenticated successfully as " + 
                            (message.has("discordName") ? message.get("discordName").getAsString() : "unknown"));
                    } else {
                        String error = message.has("error") ? message.get("error").getAsString() : "Unknown error";
                        System.err.println("[VoiceChat] Authentication failed: " + error);
                        disconnect();
                    }
                    break;
                    
                case "create_room_response":
                    if (message.has("success") && message.get("success").getAsBoolean()) {
                        String roomId = message.has("roomId") ? message.get("roomId").getAsString() : null;
                        System.out.println("[VoiceChat] Room created: " + roomId);
                        // Server auto-joins us, so we'll get join_room_response next
                    } else {
                        String error = message.has("error") ? message.get("error").getAsString() : "Failed to create room";
                        System.err.println("[VoiceChat] " + error);
                    }
                    break;
                    
                case "join_room_response":
                    if (message.has("success") && message.get("success").getAsBoolean()) {
                        JsonObject roomData = message.has("room") ? message.getAsJsonObject("room") : null;
                        if (roomData != null) {
                            VoiceRoom room = new VoiceRoom(
                                roomData.get("id").getAsString(),
                                roomData.get("name").getAsString()
                            );
                            if (roomData.has("linkedPartyId") && !roomData.get("linkedPartyId").isJsonNull()) {
                                room.setLinkedPartyId(roomData.get("linkedPartyId").getAsString());
                            }
                            manager.onRoomJoined(room);
                            System.out.println("[VoiceChat] Joined room: " + room.getName());
                        }
                        
                        // Process existing users in room
                        if (message.has("users") && message.get("users").isJsonArray()) {
                            System.out.println("[VoiceChat] Processing " + message.getAsJsonArray("users").size() + " users in room");
                            for (com.google.gson.JsonElement userElement : message.getAsJsonArray("users")) {
                                JsonObject userData = userElement.getAsJsonObject();
                                System.out.println("[VoiceChat] User data: " + userData.toString());
                                VoiceUser user = parseVoiceUser(userData);
                                if (user != null) {
                                    manager.onUserJoined(user);
                                    System.out.println("[VoiceChat] Added user: " + user.getDisplayName() + " (ID: " + user.getUserId() + ")");
                                }
                            }
                        } else {
                            System.out.println("[VoiceChat] No users array in join response");
                        }
                    } else {
                        String error = message.has("error") ? message.get("error").getAsString() : "Failed to join room";
                        System.err.println("[VoiceChat] " + error);
                    }
                    break;
                    
                case "left_room":
                    manager.onRoomLeft();
                    System.out.println("[VoiceChat] Left room");
                    break;
                    
                case "user_joined":
                    VoiceUser newUser = parseVoiceUser(message);
                    if (newUser != null) {
                        manager.onUserJoined(newUser);
                        System.out.println("[VoiceChat] User joined: " + newUser.getDisplayName());
                    }
                    break;
                    
                case "user_left":
                    String leftUserId = message.has("userId") ? message.get("userId").getAsString() : null;
                    if (leftUserId != null) {
                        manager.onUserLeftById(leftUserId);
                        System.out.println("[VoiceChat] User left: " + leftUserId);
                    }
                    break;
                    
                case "user_mute_state":
                    if (message.has("userId") && message.has("muted")) {
                        String muteUserId = message.get("userId").getAsString();
                        boolean muted = message.get("muted").getAsBoolean();
                        manager.onUserMuteChanged(muteUserId, muted);
                    }
                    break;
                    
                case "user_deafen_state":
                    if (message.has("userId") && message.has("deafened")) {
                        String userIdDeafen = message.get("userId").getAsString();
                        boolean deafened = message.get("deafened").getAsBoolean();
                        manager.onUserDeafenChanged(userIdDeafen, deafened);
                    }
                    break;
                    
                case "room_list":
                    // Handle room list response
                    if (message.has("rooms") && message.get("rooms").isJsonArray()) {
                        System.out.println("[VoiceChat] Received room list");
                        // TODO: Pass to GUI
                    }
                    break;
                    
                case "heartbeat_ack":
                    // Heartbeat acknowledged, connection is alive
                    break;
                    
                case "error":
                    String error = message.has("message") ? message.get("message").getAsString() : "Unknown error";
                    System.err.println("[VoiceChat] Server error: " + error);
                    break;
                    
                default:
                    System.out.println("[VoiceChat] Unknown message type: " + type);
            }
            
        } catch (Exception e) {
            System.err.println("[VoiceChat] Failed to handle control message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse a VoiceUser from JSON
     */
    private VoiceUser parseVoiceUser(JsonObject data) {
        try {
            // Server sends odId, but also check userId and id for compatibility
            String odId = data.has("odId") ? data.get("odId").getAsString() :
                          (data.has("userId") ? data.get("userId").getAsString() : 
                           (data.has("id") ? data.get("id").getAsString() : null));
            if (odId == null) {
                System.err.println("[VoiceChat] parseVoiceUser: No user ID found in data: " + data.toString());
                return null;
            }
            
            String discordName = data.has("discordName") ? data.get("discordName").getAsString() : null;
            String displayName = data.has("displayName") ? data.get("displayName").getAsString() : discordName;
            
            VoiceUser user = new VoiceUser(odId, displayName, discordName);
            
            if (data.has("muted")) {
                user.setMuted(data.get("muted").getAsBoolean());
            }
            if (data.has("deafened")) {
                user.setDeafened(data.get("deafened").getAsBoolean());
            }
            
            return user;
        } catch (Exception e) {
            System.err.println("[VoiceChat] Failed to parse user: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Audio receive loop
     */
    private void audioReceiveLoop() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while (running.get()) {
            try {
                audioSocket.receive(packet);
                
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                
                handleAudioPacket(data);
                
            } catch (SocketTimeoutException e) {
                // Normal timeout, continue
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[VoiceChat] Audio receive error: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Handle incoming audio packet
     */
    private void handleAudioPacket(byte[] data) {
        if (data.length < 37) { // Minimum: type(1) + senderId(36)
            return;
        }
        
        byte packetType = data[0];
        
        if (packetType == PACKET_AUDIO) {
            // Extract sender ID (Discord ID from server)
            String senderId = new String(data, 1, 36).trim();
            
            // Extract audio data
            byte[] audioData = new byte[data.length - 37];
            System.arraycopy(data, 37, audioData, 0, audioData.length);
            
            manager.onAudioReceived(senderId, audioData);
        }
    }
    
    public boolean isConnected() {
        return running.get() && controlSocket != null && !controlSocket.isClosed();
    }
    
    public String getSessionId() {
        return sessionId;
    }
}
