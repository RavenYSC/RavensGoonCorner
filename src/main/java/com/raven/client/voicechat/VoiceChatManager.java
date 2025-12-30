package com.raven.client.voicechat;

import com.raven.client.voicechat.audio.AudioCapture;
import com.raven.client.voicechat.audio.AudioPlayback;
import com.raven.client.voicechat.audio.OpusCodec;
import com.raven.client.voicechat.model.VoiceRoom;
import com.raven.client.voicechat.model.VoiceUser;
import com.raven.client.voicechat.network.VoiceClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for the voice chat system.
 * Coordinates audio capture/playback, network communication, and user management.
 */
public class VoiceChatManager {
    
    private static VoiceChatManager instance;
    
    // Components
    private VoiceClient voiceClient;
    private AudioCapture audioCapture;
    private AudioPlayback audioPlayback;
    private OpusCodec opusCodec;
    
    // Connection state
    private boolean initialized = false;
    private boolean connected = false;
    private boolean connecting = false;
    
    // User state
    private boolean muted = false;
    private boolean deafened = false;
    private boolean pushToTalkActive = false;
    
    // Settings
    private boolean usePushToTalk = true;
    private int pushToTalkKey = 47; // V key
    private float microphoneVolume = 1.0f;
    private float outputVolume = 1.0f;
    private float voiceActivationThreshold = 0.02f;
    
    // Authentication
    private String authToken;
    private String verifiedDiscordName;
    private String displayNickname;
    
    // Room state
    private VoiceRoom currentRoom;
    private Map<String, VoiceUser> usersInRoom = new ConcurrentHashMap<>();
    private List<VoiceRoom> availableRooms = new ArrayList<>();
    
    // Server configuration
    private String serverHost = "100.42.184.35";
    private int serverUdpPort = 25566;
    private int serverTcpPort = 25567;
    
    // Listeners
    private List<VoiceChatListener> listeners = new ArrayList<>();
    
    private VoiceChatManager() {
        // Private constructor for singleton
    }
    
    public static VoiceChatManager getInstance() {
        if (instance == null) {
            instance = new VoiceChatManager();
        }
        return instance;
    }
    
    /**
     * Check if voice chat system is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Initialize the voice chat system.
     */
    public void initialize() {
        System.out.println("[VoiceChat] Initializing voice chat system...");
        
        try {
            // Initialize Opus codec
            opusCodec = new OpusCodec();
            opusCodec.initialize();
            
            // Initialize audio playback
            audioPlayback = new AudioPlayback(this);
            audioPlayback.initialize();
            
            // Initialize audio capture
            audioCapture = new AudioCapture(this);
            audioCapture.initialize();
            
            // Initialize voice client - it calls back to this manager directly
            voiceClient = new VoiceClient(this);
            
            initialized = true;
            System.out.println("[VoiceChat] Voice chat system initialized successfully");
        } catch (Exception e) {
            System.err.println("[VoiceChat] Failed to initialize voice chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Shutdown the voice chat system.
     */
    public void shutdown() {
        System.out.println("[VoiceChat] Shutting down voice chat system...");
        
        initialized = false;
        disconnect();
        
        if (audioCapture != null) {
            audioCapture.shutdown();
            audioCapture = null;
        }
        
        if (audioPlayback != null) {
            audioPlayback.shutdown();
            audioPlayback = null;
        }
        
        if (opusCodec != null) {
            opusCodec.shutdown();
            opusCodec = null;
        }
        
        voiceClient = null;
        
        System.out.println("[VoiceChat] Voice chat system shut down");
    }
    
    /**
     * Connect to the voice server.
     */
    public void connect() {
        if (connected || connecting) {
            System.out.println("[VoiceChat] Already connected or connecting");
            return;
        }
        
        if (authToken == null || authToken.isEmpty()) {
            System.err.println("[VoiceChat] No auth token set. Use /voicetoken in Discord to get one.");
            notifyError("No auth token. Use /voicetoken in Discord.");
            return;
        }
        
        connecting = true;
        System.out.println("[VoiceChat] Connecting to voice server...");
        
        voiceClient.connect(serverHost, serverTcpPort, serverUdpPort, authToken);
    }
    
    /**
     * Disconnect from the voice server.
     */
    public void disconnect() {
        if (!connected && !connecting) {
            return;
        }
        
        System.out.println("[VoiceChat] Disconnecting from voice server...");
        
        if (currentRoom != null) {
            leaveRoom();
        }
        
        if (voiceClient != null) {
            voiceClient.disconnect();
        }
        
        connected = false;
        connecting = false;
        
        // Stop audio
        if (audioCapture != null) {
            audioCapture.stopCapture();
        }
        
        notifyDisconnected();
    }
    
    /**
     * Join a voice room.
     */
    public void joinRoom(String roomId) {
        if (!connected) {
            System.err.println("[VoiceChat] Not connected to voice server");
            return;
        }
        
        if (currentRoom != null) {
            leaveRoom();
        }
        
        System.out.println("[VoiceChat] Joining room: " + roomId);
        voiceClient.sendJoinRoom(roomId);
    }
    
    /**
     * Create a new voice room.
     */
    public void createRoom(String roomName, boolean isPrivate) {
        if (!connected) {
            System.err.println("[VoiceChat] Not connected to voice server");
            return;
        }
        
        System.out.println("[VoiceChat] Creating room: " + roomName);
        voiceClient.sendCreateRoom(roomName, isPrivate);
    }
    
    /**
     * Leave the current voice room.
     */
    public void leaveRoom() {
        if (currentRoom == null) {
            return;
        }
        
        System.out.println("[VoiceChat] Leaving room: " + currentRoom.getName());
        voiceClient.sendLeaveRoom();
        
        // Stop audio capture
        if (audioCapture != null) {
            audioCapture.stopCapture();
        }
        
        // Clear room state
        currentRoom = null;
        usersInRoom.clear();
    }
    
    // ============================================================
    // Callbacks from VoiceClient - these are called by VoiceClient
    // ============================================================
    
    /**
     * Called by VoiceClient when connection state changes.
     */
    public void onConnectionStateChanged(boolean isConnected) {
        this.connected = isConnected;
        this.connecting = false;
        
        if (isConnected) {
            System.out.println("[VoiceChat] Connected to voice server");
            notifyConnected();
        } else {
            System.out.println("[VoiceChat] Disconnected from voice server");
            currentRoom = null;
            usersInRoom.clear();
            notifyDisconnected();
        }
    }
    
    /**
     * Called by VoiceClient when audio is received.
     */
    public void onAudioReceived(String odId, byte[] opusData) {
        if (deafened) {
            return;
        }
        
        VoiceUser user = usersInRoom.get(odId);
        if (user == null || user.isMuted()) {
            return;
        }
        
        // Decode and play
        byte[] pcmData = opusCodec.decode(opusData);
        if (pcmData != null) {
            audioPlayback.playAudio(odId, pcmData, user.getVolume() * outputVolume);
            
            // Update talking state
            if (!user.isTalking()) {
                user.setTalking(true);
                onUserTalkingChanged(odId, true);
            }
        }
    }
    
    /**
     * Called by VoiceClient when room is joined.
     */
    public void onRoomJoined(VoiceRoom room) {
        System.out.println("[VoiceChat] Joined room: " + room.getName());
        this.currentRoom = room;
        this.usersInRoom.clear();
        
        // Start audio capture
        if (audioCapture != null && !muted) {
            audioCapture.startCapture();
        }
        
        notifyRoomJoined(room);
    }
    
    /**
     * Called by VoiceClient when room is left.
     */
    public void onRoomLeft() {
        System.out.println("[VoiceChat] Left room");
        VoiceRoom oldRoom = currentRoom;
        currentRoom = null;
        usersInRoom.clear();
        
        if (audioCapture != null) {
            audioCapture.stopCapture();
        }
        
        notifyRoomLeft(oldRoom);
    }
    
    /**
     * Called by VoiceClient when a user joins the room.
     */
    public void onUserJoined(VoiceUser user) {
        System.out.println("[VoiceChat] User joined: " + user.getDisplayName());
        usersInRoom.put(user.getUserId(), user);
        notifyUserJoined(user);
    }
    
    /**
     * Called by VoiceClient when a user leaves the room.
     */
    public void onUserLeftById(String odId) {
        VoiceUser user = usersInRoom.remove(odId);
        if (user != null) {
            System.out.println("[VoiceChat] User left: " + user.getDisplayName());
            audioPlayback.stopUser(odId);
            notifyUserLeft(user);
        }
    }
    
    /**
     * Called by VoiceClient when a user's mute state changes.
     */
    public void onUserMuteChanged(String odId, boolean isMuted) {
        VoiceUser user = usersInRoom.get(odId);
        if (user != null) {
            user.setMuted(isMuted);
            notifyUserMuteChanged(user);
        }
    }
    
    /**
     * Called by VoiceClient when a user's deafen state changes.
     */
    public void onUserDeafenChanged(String odId, boolean isDeafened) {
        VoiceUser user = usersInRoom.get(odId);
        if (user != null) {
            user.setDeafened(isDeafened);
            notifyUserDeafenChanged(user);
        }
    }
    
    /**
     * Called when user talking state changes.
     */
    private void onUserTalkingChanged(String odId, boolean talking) {
        VoiceUser user = usersInRoom.get(odId);
        if (user != null) {
            user.setTalking(talking);
            notifyUserTalkingChanged(user);
        }
    }
    
    // ============================================================
    // Audio capture callback - called by AudioCapture
    // ============================================================
    
    /**
     * Called by AudioCapture when audio is captured from microphone.
     */
    public void onAudioCaptured(byte[] pcmData) {
        if (!connected || currentRoom == null || muted || deafened) {
            return;
        }
        
        // Check push-to-talk
        if (usePushToTalk && !pushToTalkActive) {
            return;
        }
        
        // Check voice activation if not using push-to-talk
        if (!usePushToTalk) {
            float level = calculateAudioLevel(pcmData);
            if (level < voiceActivationThreshold) {
                return;
            }
        }
        
        // Encode and send
        byte[] encoded = opusCodec.encode(pcmData);
        if (encoded != null) {
            voiceClient.sendAudio(encoded);
        }
    }
    
    // Helper methods
    
    private float calculateAudioLevel(byte[] pcmData) {
        long sum = 0;
        for (int i = 0; i < pcmData.length - 1; i += 2) {
            short sample = (short) ((pcmData[i + 1] << 8) | (pcmData[i] & 0xFF));
            sum += Math.abs(sample);
        }
        return (float) sum / (pcmData.length / 2) / 32768f;
    }
    
    // Listener management
    
    public void addListener(VoiceChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(VoiceChatListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyConnected() {
        for (VoiceChatListener listener : listeners) {
            listener.onConnected();
        }
    }
    
    private void notifyDisconnected() {
        for (VoiceChatListener listener : listeners) {
            listener.onDisconnected();
        }
    }
    
    private void notifyError(String error) {
        for (VoiceChatListener listener : listeners) {
            listener.onError(error);
        }
    }
    
    private void notifyRoomJoined(VoiceRoom room) {
        for (VoiceChatListener listener : listeners) {
            listener.onRoomJoined(room);
        }
    }
    
    private void notifyRoomLeft(VoiceRoom room) {
        for (VoiceChatListener listener : listeners) {
            listener.onRoomLeft(room);
        }
    }
    
    private void notifyRoomListUpdated(List<VoiceRoom> rooms) {
        for (VoiceChatListener listener : listeners) {
            listener.onRoomListUpdated(rooms);
        }
    }
    
    private void notifyUserJoined(VoiceUser user) {
        for (VoiceChatListener listener : listeners) {
            listener.onUserJoined(user);
        }
    }
    
    private void notifyUserLeft(VoiceUser user) {
        for (VoiceChatListener listener : listeners) {
            listener.onUserLeft(user);
        }
    }
    
    private void notifyUserMuteChanged(VoiceUser user) {
        for (VoiceChatListener listener : listeners) {
            listener.onUserMuteChanged(user);
        }
    }
    
    private void notifyUserDeafenChanged(VoiceUser user) {
        for (VoiceChatListener listener : listeners) {
            listener.onUserDeafenChanged(user);
        }
    }
    
    private void notifyUserTalkingChanged(VoiceUser user) {
        for (VoiceChatListener listener : listeners) {
            listener.onUserTalkingChanged(user);
        }
    }
    
    // Getters and setters
    
    public boolean isConnected() {
        return connected;
    }
    
    public boolean isConnecting() {
        return connecting;
    }
    
    public boolean isMuted() {
        return muted;
    }
    
    public void setMuted(boolean muted) {
        this.muted = muted;
        if (voiceClient != null && connected) {
            voiceClient.sendMuteState(muted);
        }
        if (muted && audioCapture != null) {
            audioCapture.stopCapture();
        } else if (!muted && currentRoom != null && audioCapture != null) {
            audioCapture.startCapture();
        }
    }
    
    public boolean isDeafened() {
        return deafened;
    }
    
    public void setDeafened(boolean deafened) {
        this.deafened = deafened;
        if (voiceClient != null && connected) {
            voiceClient.sendDeafenState(deafened);
        }
    }
    
    public boolean isUsePushToTalk() {
        return usePushToTalk;
    }
    
    public void setUsePushToTalk(boolean usePushToTalk) {
        this.usePushToTalk = usePushToTalk;
    }
    
    public int getPushToTalkKey() {
        return pushToTalkKey;
    }
    
    public void setPushToTalkKey(int pushToTalkKey) {
        this.pushToTalkKey = pushToTalkKey;
    }
    
    public boolean isPushToTalkActive() {
        return pushToTalkActive;
    }
    
    public void setPushToTalkActive(boolean active) {
        this.pushToTalkActive = active;
    }
    
    /**
     * Get the current microphone input level (0-1)
     */
    public float getMicrophoneLevel() {
        if (audioCapture != null && audioCapture.isCapturing()) {
            return audioCapture.getVoiceActivityLevel();
        }
        return 0.0f;
    }
    
    public float getMicrophoneVolume() {
        return microphoneVolume;
    }
    
    public void setMicrophoneVolume(float microphoneVolume) {
        this.microphoneVolume = Math.max(0, Math.min(2, microphoneVolume));
    }
    
    public float getOutputVolume() {
        return outputVolume;
    }
    
    public void setOutputVolume(float outputVolume) {
        this.outputVolume = Math.max(0, Math.min(2, outputVolume));
    }
    
    public float getVoiceActivationThreshold() {
        return voiceActivationThreshold;
    }
    
    public void setVoiceActivationThreshold(float threshold) {
        this.voiceActivationThreshold = Math.max(0, Math.min(1, threshold));
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    
    public String getVerifiedDiscordName() {
        return verifiedDiscordName;
    }
    
    public void setVerifiedDiscordName(String verifiedDiscordName) {
        this.verifiedDiscordName = verifiedDiscordName;
    }
    
    public String getDisplayNickname() {
        return displayNickname;
    }
    
    public void setDisplayNickname(String displayNickname) {
        this.displayNickname = displayNickname;
    }
    
    public VoiceRoom getCurrentRoom() {
        return currentRoom;
    }
    
    public Map<String, VoiceUser> getUsersInRoom() {
        return usersInRoom;
    }
    
    public List<VoiceRoom> getAvailableRooms() {
        return availableRooms;
    }
    
    public String getServerHost() {
        return serverHost;
    }
    
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }
    
    public int getServerUdpPort() {
        return serverUdpPort;
    }
    
    public void setServerUdpPort(int serverUdpPort) {
        this.serverUdpPort = serverUdpPort;
    }
    
    public int getServerTcpPort() {
        return serverTcpPort;
    }
    
    public void setServerTcpPort(int serverTcpPort) {
        this.serverTcpPort = serverTcpPort;
    }
    
    /**
     * Listener interface for voice chat events.
     */
    public interface VoiceChatListener {
        void onConnected();
        void onDisconnected();
        void onError(String error);
        void onRoomJoined(VoiceRoom room);
        void onRoomLeft(VoiceRoom room);
        void onRoomListUpdated(List<VoiceRoom> rooms);
        void onUserJoined(VoiceUser user);
        void onUserLeft(VoiceUser user);
        void onUserMuteChanged(VoiceUser user);
        void onUserDeafenChanged(VoiceUser user);
        void onUserTalkingChanged(VoiceUser user);
    }
}
