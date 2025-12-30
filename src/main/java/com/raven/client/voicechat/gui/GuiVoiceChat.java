package com.raven.client.voicechat.gui;

import com.raven.client.gui.components.RenderUtils;
import com.raven.client.voicechat.VoiceChatManager;
import com.raven.client.voicechat.model.VoiceRoom;
import com.raven.client.voicechat.model.VoiceUser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Voice Chat GUI - main interface for voice chat controls
 */
public class GuiVoiceChat extends GuiScreen {
    
    private static final int PANEL_X = 50;
    private static final int PANEL_Y = 30;
    
    private VoiceChatManager voiceManager;
    
    // Current view mode
    private String viewMode = "main"; // "main", "rooms", "settings", "create_room"
    
    // GUI components
    private GuiTextField roomNameField;
    private GuiTextField authTokenField;
    
    // Room list
    private List<RoomListEntry> availableRooms = new ArrayList<>();
    private int roomListScroll = 0;
    
    // Status messages
    private String statusMessage = "";
    private long statusMessageTime = 0;
    
    @Override
    public void initGui() {
        super.initGui();
        
        voiceManager = VoiceChatManager.getInstance();
        
        // Initialize voice chat if not already
        if (!voiceManager.isInitialized()) {
            voiceManager.initialize();
        }
        
        // Create text fields
        roomNameField = new GuiTextField(100, this.fontRendererObj, PANEL_X + 120, PANEL_Y + 180, 200, 20);
        roomNameField.setMaxStringLength(32);
        
        authTokenField = new GuiTextField(101, this.fontRendererObj, PANEL_X + 120, PANEL_Y + 100, 250, 20);
        authTokenField.setMaxStringLength(64);
        if (voiceManager.getAuthToken() != null) {
            authTokenField.setText(voiceManager.getAuthToken());
        }
        
        // Add buttons
        this.buttonList.clear();
        
        int buttonY = PANEL_Y + 50;
        
        // Main control buttons
        this.buttonList.add(new GuiButton(1, PANEL_X + 10, buttonY, 100, 20, 
            voiceManager.isConnected() ? "Disconnect" : "Connect"));
        this.buttonList.add(new GuiButton(2, PANEL_X + 120, buttonY, 100, 20, "Browse Rooms"));
        this.buttonList.add(new GuiButton(3, PANEL_X + 230, buttonY, 100, 20, "Settings"));
        
        // Voice control buttons
        buttonY += 30;
        this.buttonList.add(new GuiButton(10, PANEL_X + 10, buttonY, 80, 20, 
            voiceManager.isMuted() ? "Unmute" : "Mute"));
        this.buttonList.add(new GuiButton(11, PANEL_X + 100, buttonY, 80, 20, 
            voiceManager.isDeafened() ? "Undeafen" : "Deafen"));
        this.buttonList.add(new GuiButton(12, PANEL_X + 190, buttonY, 80, 20, "Leave Room"));
        
        // Room browser buttons (shown in rooms view)
        this.buttonList.add(new GuiButton(20, PANEL_X + 10, PANEL_Y + 250, 100, 20, "Create Room"));
        this.buttonList.add(new GuiButton(21, PANEL_X + 120, PANEL_Y + 250, 100, 20, "Refresh"));
        this.buttonList.add(new GuiButton(22, PANEL_X + 230, PANEL_Y + 250, 100, 20, "Back"));
        
        // Create room buttons
        this.buttonList.add(new GuiButton(30, PANEL_X + 120, PANEL_Y + 220, 100, 20, "Create"));
        this.buttonList.add(new GuiButton(31, PANEL_X + 230, PANEL_Y + 220, 100, 20, "Cancel"));
        
        // Settings buttons
        this.buttonList.add(new GuiButton(40, PANEL_X + 10, PANEL_Y + 250, 100, 20, "Save Token"));
        this.buttonList.add(new GuiButton(41, PANEL_X + 120, PANEL_Y + 250, 100, 20, 
            voiceManager.isUsePushToTalk() ? "PTT: ON" : "PTT: OFF"));
        this.buttonList.add(new GuiButton(42, PANEL_X + 230, PANEL_Y + 250, 100, 20, "Back"));
        
        updateButtonVisibility();
    }
    
    private void updateButtonVisibility() {
        for (GuiButton btn : this.buttonList) {
            boolean visible = false;
            
            switch (viewMode) {
                case "main":
                    visible = btn.id >= 1 && btn.id <= 12;
                    break;
                case "rooms":
                    visible = btn.id >= 20 && btn.id <= 22;
                    break;
                case "create_room":
                    visible = btn.id >= 30 && btn.id <= 31;
                    break;
                case "settings":
                    visible = btn.id >= 40 && btn.id <= 42;
                    break;
            }
            
            btn.visible = visible;
        }
        
        // Update button labels
        for (GuiButton btn : this.buttonList) {
            if (btn.id == 1) {
                btn.displayString = voiceManager.isConnected() ? "Disconnect" : "Connect";
            } else if (btn.id == 10) {
                btn.displayString = voiceManager.isMuted() ? "Unmute" : "Mute";
            } else if (btn.id == 11) {
                btn.displayString = voiceManager.isDeafened() ? "Undeafen" : "Deafen";
            } else if (btn.id == 41) {
                btn.displayString = voiceManager.isUsePushToTalk() ? "PTT: ON" : "PTT: OFF";
            }
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw background
        this.drawDefaultBackground();
        
        int panelWidth = this.width - 100;
        int panelHeight = this.height - 60;
        
        // Main panel
        RenderUtils.drawBox(PANEL_X, PANEL_Y, PANEL_X + panelWidth, PANEL_Y + panelHeight, 
            0xFF1A1A2E, 0xFF333344, 1);
        
        // Header
        RenderUtils.drawRect(PANEL_X, PANEL_Y, PANEL_X + panelWidth, PANEL_Y + 40, 0xFF5865F2);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Voice Chat", PANEL_X + 15, PANEL_Y + 15, 0xFFFFFF);
        
        // Connection status indicator
        int statusColor = voiceManager.isConnected() ? 0xFF55FF55 : 0xFFFF5555;
        String statusText = voiceManager.isConnected() ? "Connected" : "Disconnected";
        int statusWidth = this.fontRendererObj.getStringWidth(statusText);
        RenderUtils.drawRect(PANEL_X + panelWidth - statusWidth - 30, PANEL_Y + 10, 
            PANEL_X + panelWidth - 10, PANEL_Y + 30, 0xFF2a2a3a);
        this.fontRendererObj.drawString(statusText, PANEL_X + panelWidth - statusWidth - 20, PANEL_Y + 16, statusColor);
        
        // Draw current view
        switch (viewMode) {
            case "main":
                drawMainView(mouseX, mouseY, panelWidth, panelHeight);
                break;
            case "rooms":
                drawRoomsView(mouseX, mouseY, panelWidth, panelHeight);
                break;
            case "create_room":
                drawCreateRoomView(mouseX, mouseY, panelWidth, panelHeight);
                break;
            case "settings":
                drawSettingsView(mouseX, mouseY, panelWidth, panelHeight);
                break;
        }
        
        // Draw status message
        if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusMessageTime < 3000) {
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int msgWidth = this.fontRendererObj.getStringWidth(statusMessage);
            this.fontRendererObj.drawString(statusMessage, PANEL_X + (panelWidth - msgWidth) / 2, 
                PANEL_Y + panelHeight - 25, 0xFFFF55);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawMainView(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        int contentY = PANEL_Y + 110;
        
        // Current room info
        VoiceRoom currentRoom = voiceManager.getCurrentRoom();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        if (currentRoom != null) {
            this.fontRendererObj.drawString("Current Room: " + currentRoom.getName(), 
                PANEL_X + 15, contentY, 0xFFFFFF);
            contentY += 15;
            
            if (currentRoom.isLinkedToParty()) {
                this.fontRendererObj.drawString("(Linked to Party)", PANEL_X + 15, contentY, 0x88FF88);
                contentY += 15;
            }
            
            // Users in room
            contentY += 10;
            this.fontRendererObj.drawString("Users in Room:", PANEL_X + 15, contentY, 0xAAAAAA);
            contentY += 15;
            
            for (VoiceUser user : voiceManager.getUsersInRoom().values()) {
                // User entry
                int userBgColor = user.isTalking() ? 0xFF3a5a3a : 0xFF2a2a3a;
                RenderUtils.drawRect(PANEL_X + 15, contentY, PANEL_X + panelWidth - 30, contentY + 25, userBgColor);
                
                // Talking indicator
                if (user.isTalking()) {
                    RenderUtils.drawRect(PANEL_X + 17, contentY + 2, PANEL_X + 22, contentY + 23, 0xFF55FF55);
                }
                
                // User name
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                String displayName = user.getDisplayName();
                if (user.getDiscordName() != null) {
                    displayName += " (" + user.getDiscordName() + ")";
                }
                this.fontRendererObj.drawString(displayName, PANEL_X + 28, contentY + 8, 
                    user.isMuted() ? 0x888888 : 0xFFFFFF);
                
                // Mute/deafen icons
                if (user.isMuted()) {
                    this.fontRendererObj.drawString("[M]", PANEL_X + panelWidth - 60, contentY + 8, 0xFF5555);
                }
                if (user.isDeafened()) {
                    this.fontRendererObj.drawString("[D]", PANEL_X + panelWidth - 45, contentY + 8, 0xFF5555);
                }
                
                contentY += 28;
            }
        } else {
            this.fontRendererObj.drawString("Not in a room", PANEL_X + 15, contentY, 0x888888);
            contentY += 20;
            this.fontRendererObj.drawString("Click 'Browse Rooms' to join or create a room", 
                PANEL_X + 15, contentY, 0x666666);
        }
        
        // Voice activity meter
        if (voiceManager.isConnected() && currentRoom != null) {
            int meterY = PANEL_Y + panelHeight - 60;
            this.fontRendererObj.drawString("Mic Level:", PANEL_X + 15, meterY, 0xAAAAAA);
            
            // Meter background
            RenderUtils.drawRect(PANEL_X + 80, meterY - 2, PANEL_X + 280, meterY + 12, 0xFF1a1a2a);
            
            // Meter fill - use actual audio level from audio capture
            float level = voiceManager.getMicrophoneLevel();
            int fillWidth = (int) (200 * Math.min(1.0f, level * 5)); // Scale up for visibility
            
            // Color based on state: gray if muted, green if transmitting, yellow otherwise
            int meterColor;
            if (voiceManager.isMuted()) {
                meterColor = 0xFF555555;
            } else if (voiceManager.isPushToTalkActive() || (!voiceManager.isUsePushToTalk() && level > 0.02f)) {
                meterColor = 0xFF55FF55; // Bright green when transmitting
            } else {
                meterColor = 0xFF55AA55; // Normal green
            }
            RenderUtils.drawRect(PANEL_X + 80, meterY - 2, PANEL_X + 80 + fillWidth, meterY + 12, meterColor);
            
            // PTT indicator
            if (voiceManager.isUsePushToTalk()) {
                String pttText = voiceManager.isPushToTalkActive() ? "PTT Active" : "Press V to talk";
                this.fontRendererObj.drawString(pttText, PANEL_X + 290, meterY, 
                    voiceManager.isPushToTalkActive() ? 0x55FF55 : 0x888888);
            }
        }
    }
    
    private void drawRoomsView(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        int contentY = PANEL_Y + 60;
        
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Available Rooms", PANEL_X + 15, contentY, 0xFFFFFF);
        contentY += 20;
        
        // Room list
        if (availableRooms.isEmpty()) {
            this.fontRendererObj.drawString("No rooms available", PANEL_X + 15, contentY + 50, 0x888888);
            this.fontRendererObj.drawString("Create a new room or join via Party Finder", 
                PANEL_X + 15, contentY + 65, 0x666666);
        } else {
            for (int i = roomListScroll; i < Math.min(roomListScroll + 6, availableRooms.size()); i++) {
                RoomListEntry room = availableRooms.get(i);
                
                boolean hover = mouseX >= PANEL_X + 15 && mouseX < PANEL_X + panelWidth - 30 &&
                               mouseY >= contentY && mouseY < contentY + 30;
                
                int bgColor = hover ? 0xFF3a3a4a : 0xFF2a2a3a;
                RenderUtils.drawRect(PANEL_X + 15, contentY, PANEL_X + panelWidth - 30, contentY + 30, bgColor);
                
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.fontRendererObj.drawString(room.name, PANEL_X + 20, contentY + 5, 0xFFFFFF);
                this.fontRendererObj.drawString(room.userCount + "/" + room.maxUsers + " users", 
                    PANEL_X + 20, contentY + 17, 0x888888);
                
                if (room.isPartyRoom) {
                    this.fontRendererObj.drawString("[Party]", PANEL_X + panelWidth - 80, contentY + 10, 0x88FF88);
                }
                
                contentY += 35;
            }
        }
    }
    
    private void drawCreateRoomView(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        int contentY = PANEL_Y + 60;
        
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Create New Room", PANEL_X + 15, contentY, 0xFFFFFF);
        
        contentY += 40;
        this.fontRendererObj.drawString("Room Name:", PANEL_X + 15, contentY + 5, 0xAAAAAA);
        
        roomNameField.yPosition = contentY;
        roomNameField.drawTextBox();
    }
    
    private void drawSettingsView(int mouseX, int mouseY, int panelWidth, int panelHeight) {
        int contentY = PANEL_Y + 60;
        
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.fontRendererObj.drawString("Voice Chat Settings", PANEL_X + 15, contentY, 0xFFFFFF);
        
        // Auth token
        contentY += 30;
        this.fontRendererObj.drawString("Auth Token:", PANEL_X + 15, contentY + 5, 0xAAAAAA);
        authTokenField.yPosition = contentY;
        authTokenField.drawTextBox();
        
        contentY += 30;
        this.fontRendererObj.drawString("(Get this from the Discord bot)", PANEL_X + 15, contentY, 0x666666);
        
        // Verified Discord name
        if (voiceManager.getVerifiedDiscordName() != null) {
            contentY += 20;
            this.fontRendererObj.drawString("Verified as: " + voiceManager.getVerifiedDiscordName(), 
                PANEL_X + 15, contentY, 0x55FF55);
        }
        
        // Push to talk settings
        contentY += 40;
        this.fontRendererObj.drawString("Push to Talk Key: V", PANEL_X + 15, contentY, 0xAAAAAA);
        
        // Volume sliders (simplified - would need proper slider widgets)
        contentY += 30;
        this.fontRendererObj.drawString("Microphone Volume: " + (int)(voiceManager.getMicrophoneVolume() * 100) + "%", 
            PANEL_X + 15, contentY, 0xAAAAAA);
        
        contentY += 20;
        this.fontRendererObj.drawString("Output Volume: " + (int)(voiceManager.getOutputVolume() * 100) + "%", 
            PANEL_X + 15, contentY, 0xAAAAAA);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            // Main controls
            case 1: // Connect/Disconnect
                if (voiceManager.isConnected()) {
                    voiceManager.disconnect();
                } else {
                    if (voiceManager.getAuthToken() == null || voiceManager.getAuthToken().isEmpty()) {
                        setStatus("Please set auth token in Settings first");
                    } else {
                        voiceManager.connect();
                    }
                }
                break;
            case 2: // Browse Rooms
                viewMode = "rooms";
                break;
            case 3: // Settings
                viewMode = "settings";
                break;
                
            // Voice controls
            case 10: // Mute
                voiceManager.setMuted(!voiceManager.isMuted());
                break;
            case 11: // Deafen
                voiceManager.setDeafened(!voiceManager.isDeafened());
                break;
            case 12: // Leave Room
                voiceManager.leaveRoom();
                break;
                
            // Room browser
            case 20: // Create Room
                viewMode = "create_room";
                break;
            case 21: // Refresh
                // TODO: Request room list from server
                break;
            case 22: // Back
                viewMode = "main";
                break;
                
            // Create room
            case 30: // Create
                String roomName = roomNameField.getText().trim();
                if (!roomName.isEmpty()) {
                    voiceManager.createRoom(roomName, false);
                    viewMode = "main";
                    setStatus("Creating room: " + roomName);
                }
                break;
            case 31: // Cancel
                viewMode = "rooms";
                break;
                
            // Settings
            case 40: // Save Token
                String token = authTokenField.getText().trim();
                if (!token.isEmpty()) {
                    voiceManager.setAuthToken(token);
                    setStatus("Auth token saved");
                }
                break;
            case 41: // Toggle PTT
                voiceManager.setUsePushToTalk(!voiceManager.isUsePushToTalk());
                break;
            case 42: // Back
                viewMode = "main";
                break;
        }
        
        updateButtonVisibility();
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Handle text fields
        if (viewMode.equals("create_room")) {
            roomNameField.textboxKeyTyped(typedChar, keyCode);
        } else if (viewMode.equals("settings")) {
            authTokenField.textboxKeyTyped(typedChar, keyCode);
        }
        
        // PTT handling
        if (keyCode == voiceManager.getPushToTalkKey()) {
            voiceManager.setPushToTalkActive(true);
        }
        
        // Escape to close or go back
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (!viewMode.equals("main")) {
                viewMode = "main";
                updateButtonVisibility();
            } else {
                this.mc.displayGuiScreen(null);
            }
            return;
        }
        
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        if (viewMode.equals("create_room")) {
            roomNameField.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (viewMode.equals("settings")) {
            authTokenField.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (viewMode.equals("rooms")) {
            // Handle room list clicks
            int contentY = PANEL_Y + 80;
            for (int i = roomListScroll; i < Math.min(roomListScroll + 6, availableRooms.size()); i++) {
                if (mouseX >= PANEL_X + 15 && mouseX < PANEL_X + this.width - 130 &&
                    mouseY >= contentY && mouseY < contentY + 30) {
                    
                    RoomListEntry room = availableRooms.get(i);
                    voiceManager.joinRoom(room.id);
                    viewMode = "main";
                    setStatus("Joining room: " + room.name);
                    break;
                }
                contentY += 35;
            }
        }
    }
    
    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        
        // PTT release detection
        if (!Keyboard.getEventKeyState() && Keyboard.getEventKey() == voiceManager.getPushToTalkKey()) {
            voiceManager.setPushToTalkActive(false);
        }
    }
    
    private void setStatus(String message) {
        this.statusMessage = message;
        this.statusMessageTime = System.currentTimeMillis();
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    /**
     * Simple room list entry
     */
    private static class RoomListEntry {
        String id;
        String name;
        int userCount;
        int maxUsers;
        boolean isPartyRoom;
        
        RoomListEntry(String id, String name, int userCount, int maxUsers, boolean isPartyRoom) {
            this.id = id;
            this.name = name;
            this.userCount = userCount;
            this.maxUsers = maxUsers;
            this.isPartyRoom = isPartyRoom;
        }
    }
}
