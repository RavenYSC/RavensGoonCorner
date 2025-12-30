package com.raven.client.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.opengl.GL11;

import com.raven.client.gui.GuiOpener;

import java.util.*;

public class MusicGUI extends GuiScreen {

    private Minecraft mc;
    private int guiX, guiY, guiWidth, guiHeight;
    
    private Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }
    
    private int selectedTab = 0; // 0 = Now Playing, 1 = Playlist, 2 = Settings
    private int playlistScroll = 0;
    private int maxPlaylistScroll = 0;
    
    private int tabButtonWidth = 80;
    private int tabButtonHeight = 30;
    private int contentPaddingX = 20;
    private int contentPaddingY = 65;

    @Override
    public void initGui() {
        // Work in raw display coordinates (independent of MC GUI scale)
        int displayWidth = this.width;
        int displayHeight = this.height;
        
        // Calculate unscaled dimensions (will be rendered at 2x)
        guiWidth = (int)(displayWidth * 0.45);  // 90% / 2x scale
        guiHeight = (int)(displayHeight * 0.425); // 85% / 2x scale
        guiX = (displayWidth - guiWidth * 2) / 4;  // Center accounting for 2x
        guiY = (displayHeight - guiHeight * 2) / 4; // Center accounting for 2x

        this.buttonList.clear();
    }

    @Override
    protected void actionPerformed(net.minecraft.client.gui.GuiButton button) {
        // No button actions - all handled in mouseClicked
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (Exception e) {
            // Ignore
        }
        
        // Convert raw mouse position to GUI coordinate space (accounting for 2x scale)
        int scaledMouseX = mouseX / 2;
        int scaledMouseY = mouseY / 2;
        
        // Tab buttons - centered horizontally
        int tabSpacing = 10;
        int totalTabWidth = tabButtonWidth * 3 + tabSpacing * 2;
        int tabStartX = guiX + (guiWidth - totalTabWidth) / 2;
        
        // Check tab clicks
        for (int i = 0; i < 3; i++) {
            int tabX = tabStartX + (tabButtonWidth + tabSpacing) * i;
            int tabY = guiY + 12;
            
            if (scaledMouseX >= tabX && scaledMouseX <= tabX + tabButtonWidth &&
                scaledMouseY >= tabY && scaledMouseY <= tabY + tabButtonHeight) {
                selectedTab = i;
                return;
            }
        }
        
        // Exit button - top right corner
        int exitX = guiX + guiWidth - 35;
        int exitY = guiY + 12;
        int exitWidth = 25;
        int exitHeight = tabButtonHeight;
        
        if (scaledMouseX >= exitX && scaledMouseX <= exitX + exitWidth &&
            scaledMouseY >= exitY && scaledMouseY <= exitY + exitHeight) {
            GuiOpener.openGuiNextTick(null);
            return;
        }
        
        // Playlist button clicks
        if (selectedTab == 1) {
            handlePlaylistClicks(scaledMouseX, scaledMouseY);
        }
    }
    
    private void handlePlaylistClicks(int scaledMouseX, int scaledMouseY) {
        int contentY = guiY + contentPaddingY;
        List<java.io.File> tracks = MusicManager.getPlaylist();
        int itemHeight = 30;
        
        for (int i = 0; i < tracks.size(); i++) {
            int itemX = guiX + contentPaddingX;
            int itemY = contentY + (itemHeight + 3) * i;
            int itemWidth = guiWidth - contentPaddingX * 2;
            
            if (scaledMouseX >= itemX && scaledMouseX <= itemX + itemWidth &&
                scaledMouseY >= itemY && scaledMouseY <= itemY + itemHeight) {
                // Play selected track
                // Note: MusicManager doesn't have direct track index control, so this would need extension
                break;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // Apply 2x scaling with GL matrix
        GL11.glPushMatrix();
        GL11.glScalef(2.0f, 2.0f, 2.0f);
        
        // Convert mouse to GUI coordinate space
        int scaledMouseX = mouseX / 2;
        int scaledMouseY = mouseY / 2;
        
        // Main background box
        drawRect(guiX, guiY, guiX + guiWidth, guiY + guiHeight, 0xCC1E1E1E);
        
        // Draw tab backgrounds with styling
        drawTabBar(scaledMouseX, scaledMouseY);
        
        // Separator line below tabs
        drawRect(guiX, guiY + 50, guiX + guiWidth, guiY + 52, 0xFF00AA00);
        
        // Draw content based on tab
        if (selectedTab == 0) {
            drawNowPlayingTab(scaledMouseX, scaledMouseY);
        } else if (selectedTab == 1) {
            drawPlaylistTab();
        } else if (selectedTab == 2) {
            drawSettingsTab(scaledMouseX, scaledMouseY);
        }
        
        GL11.glPopMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawTabBar(int scaledMouseX, int scaledMouseY) {
        Minecraft mc = getMc();
        // Tab buttons - centered
        int tabSpacing = 10;
        int totalTabWidth = tabButtonWidth * 3 + tabSpacing * 2;
        int tabStartX = guiX + (guiWidth - totalTabWidth) / 2;
        
        String[] tabNames = {"Now Playing", "Playlist", "Settings"};
        
        for (int i = 0; i < 3; i++) {
            int tabX = tabStartX + (tabButtonWidth + tabSpacing) * i;
            int tabY = guiY + 12;
            
            // Highlight active tab
            if (selectedTab == i) {
                drawRect(tabX, tabY, tabX + tabButtonWidth, tabY + tabButtonHeight, 0xFF1A5A3A);
                drawRect(tabX, tabY + tabButtonHeight - 2, tabX + tabButtonWidth, tabY + tabButtonHeight, 0xFF00AA00);
            } else {
                drawRect(tabX, tabY, tabX + tabButtonWidth, tabY + tabButtonHeight, 0xFF2A2A2A);
            }
            
            // Tab border
            drawRect(tabX, tabY, tabX + 1, tabY + tabButtonHeight, 0xFF444444);
            drawRect(tabX + tabButtonWidth - 1, tabY, tabX + tabButtonWidth, tabY + tabButtonHeight, 0xFF444444);
            
            // Tab text
            int textX = tabX + (tabButtonWidth - mc.fontRendererObj.getStringWidth(tabNames[i])) / 2;
            int textY = tabY + (tabButtonHeight - 8) / 2;
            mc.fontRendererObj.drawString(tabNames[i], textX, textY, 0xFFFFFF);
        }
        
        // Exit button - top right corner
        int exitX = guiX + guiWidth - 35;
        int exitY = guiY + 12;
        int exitWidth = 25;
        
        drawRect(exitX, exitY, exitX + exitWidth, exitY + tabButtonHeight, 0xFF2A2A2A);
        drawRect(exitX, exitY, exitX + exitWidth, exitY + tabButtonHeight - 2, 0xFF444444);
        drawRect(exitX, exitY, exitX + 1, exitY + tabButtonHeight, 0xFF444444);
        drawRect(exitX + exitWidth - 1, exitY, exitX + exitWidth, exitY + tabButtonHeight, 0xFF444444);
        
        // Exit text (X)
        int textX = exitX + (exitWidth - mc.fontRendererObj.getStringWidth("X")) / 2;
        int textY = exitY + (tabButtonHeight - 8) / 2;
        mc.fontRendererObj.drawString("X", textX, textY, 0xFFFFFF);
    }
    
    private void drawNowPlayingTab(int scaledMouseX, int scaledMouseY) {
        Minecraft mc = getMc();
        int contentY = guiY + contentPaddingY;
        int contentWidth = guiWidth - contentPaddingX * 2;
        
        // Current track display
        String currentTrack = MusicManager.getCurrentTrackName();
        boolean isPlaying = MusicManager.isPlaying();
        
        drawRect(guiX + contentPaddingX, contentY, guiX + contentPaddingX + contentWidth, contentY + 60, 0xFF2A2A2A);
        drawRect(guiX + contentPaddingX, contentY, guiX + contentPaddingX + contentWidth, contentY + 3, 0xFF00AA00);
        
        mc.fontRendererObj.drawString("Now Playing", guiX + contentPaddingX + 12, contentY + 12, 0xFFFFFF);
        mc.fontRendererObj.drawString(currentTrack, guiX + contentPaddingX + 12, contentY + 28, 0xAAAAAA);
        mc.fontRendererObj.drawString("Status: " + (isPlaying ? "Playing" : "Stopped"), guiX + contentPaddingX + 12, contentY + 42, 0xFF00AA00);
        
        // Playback controls
        int controlY = contentY + 80;
        int btnWidth = 70;
        int btnHeight = 25;
        int btnSpacing = 10;
        int centerX = guiX + contentPaddingX + (contentWidth - (btnWidth * 3 + btnSpacing * 2)) / 2;
        
        // Previous button
        int prevX = centerX;
        boolean prevHover = scaledMouseX >= prevX && scaledMouseX <= prevX + btnWidth &&
                           scaledMouseY >= controlY && scaledMouseY <= controlY + btnHeight;
        drawButton(prevX, controlY, btnWidth, btnHeight, "Previous", prevHover);
        
        // Play/Pause button
        int playX = centerX + btnWidth + btnSpacing;
        boolean playHover = scaledMouseX >= playX && scaledMouseX <= playX + btnWidth &&
                           scaledMouseY >= controlY && scaledMouseY <= controlY + btnHeight;
        drawButton(playX, controlY, btnWidth, btnHeight, isPlaying ? "Pause" : "Play", playHover);
        if (playHover && mouseClicked) {
            if (isPlaying) MusicManager.stop();
            else MusicManager.playCurrent();
        }
        
        // Next button
        int nextX = centerX + (btnWidth + btnSpacing) * 2;
        boolean nextHover = scaledMouseX >= nextX && scaledMouseX <= nextX + btnWidth &&
                           scaledMouseY >= controlY && scaledMouseY <= controlY + btnHeight;
        drawButton(nextX, controlY, btnWidth, btnHeight, "Next", nextHover);
        
        // Volume display
        int volumeDisplayY = controlY + btnHeight + 30;
        int volumePercent = Math.round(MusicManager.getVolume() * 100);
        mc.fontRendererObj.drawString("Volume: " + volumePercent + "%", guiX + contentPaddingX + 12, volumeDisplayY, 0xFFFFFF);
    }
    
    private void drawPlaylistTab() {
        Minecraft mc = getMc();
        int contentY = guiY + contentPaddingY;
        int contentWidth = guiWidth - contentPaddingX * 2;
        int itemHeight = 30;
        
        // Playlist header
        mc.fontRendererObj.drawString("Tracks in Playlist", guiX + contentPaddingX + 12, contentY, 0xFFFFFF);
        
        List<java.io.File> tracks = MusicManager.getPlaylist();
        
        if (tracks.isEmpty()) {
            drawRect(guiX + contentPaddingX, contentY + 20, guiX + contentPaddingX + contentWidth, contentY + 60, 0xFF2A2A2A);
            mc.fontRendererObj.drawString("No tracks loaded", guiX + contentPaddingX + 12, contentY + 35, 0xAAAAAA);
            return;
        }
        
        int listY = contentY + 25;
        for (int i = 0; i < Math.min(tracks.size(), 8); i++) {
            java.io.File track = tracks.get(i);
            int itemX = guiX + contentPaddingX;
            int itemY = listY + (itemHeight + 3) * i;
            
            drawRect(itemX, itemY, itemX + contentWidth, itemY + itemHeight, 0xFF2A2A2A);
            drawRect(itemX, itemY, itemX + contentWidth, itemY + 2, 0xFF00AA00);
            
            String trackName = track.getName().replace(".mp3", "");
            if (trackName.length() > 40) trackName = trackName.substring(0, 37) + "...";
            mc.fontRendererObj.drawString(trackName, itemX + 12, itemY + 10, 0xFFFFFF);
        }
        
        if (tracks.size() > 8) {
            mc.fontRendererObj.drawString("... and " + (tracks.size() - 8) + " more", guiX + contentPaddingX + 12, listY + (itemHeight + 3) * 8 + 5, 0xAAAAAA);
        }
    }
    
    private void drawSettingsTab(int scaledMouseX, int scaledMouseY) {
        Minecraft mc = getMc();
        int contentY = guiY + contentPaddingY;
        int contentWidth = guiWidth - contentPaddingX * 2;
        
        // Volume slider
        int sliderY = contentY + 20;
        mc.fontRendererObj.drawString("Volume Control", guiX + contentPaddingX + 12, sliderY, 0xFFFFFF);
        
        int sliderX = guiX + contentPaddingX + 12;
        int sliderWidth = contentWidth - 24;
        int sliderHeight = 15;
        float volume = MusicManager.getVolume();
        int fillWidth = (int)(sliderWidth * volume);
        
        drawRect(sliderX, sliderY + 20, sliderX + sliderWidth, sliderY + 20 + sliderHeight, 0xFF2A2A2A);
        drawRect(sliderX, sliderY + 20, sliderX + fillWidth, sliderY + 20 + sliderHeight, 0xFF00AA00);
        drawRect(sliderX, sliderY + 20, sliderX + sliderWidth, sliderY + 22, 0xFF444444);
        
        int volumePercent = Math.round(volume * 100);
        mc.fontRendererObj.drawString(volumePercent + "%", sliderX + sliderWidth + 5, sliderY + 20, 0xFFFFFF);
        
        // Shuffle toggle
        int shuffleY = sliderY + 50;
        boolean shuffle = MusicManager.isShuffle();
        mc.fontRendererObj.drawString("Shuffle: " + (shuffle ? "ON" : "OFF"), guiX + contentPaddingX + 12, shuffleY, shuffle ? 0xFF00AA00 : 0xAAAAAA);
        
        // Track reload button
        int reloadY = shuffleY + 30;
        int btnWidth = 100;
        int btnHeight = 20;
        boolean reloadHover = scaledMouseX >= guiX + contentPaddingX + 12 && scaledMouseX <= guiX + contentPaddingX + 12 + btnWidth &&
                             scaledMouseY >= reloadY && scaledMouseY <= reloadY + btnHeight;
        drawButton(guiX + contentPaddingX + 12, reloadY, btnWidth, btnHeight, "Reload Tracks", reloadHover);
    }
    
    private void drawButton(int x, int y, int width, int height, String label, boolean hovered) {
        Minecraft mc = getMc();
        int bgColor = hovered ? 0xFF1A5A3A : 0xFF2A2A2A;
        drawRect(x, y, x + width, y + height, bgColor);
        drawRect(x, y, x + width, y + 2, 0xFF00AA00);
        drawRect(x, y, x + 2, y + height, 0xFF444444);
        
        int textX = x + (width - mc.fontRendererObj.getStringWidth(label)) / 2;
        int textY = y + (height - 8) / 2;
        mc.fontRendererObj.drawString(label, textX, textY, 0xFFFFFF);
    }
    
    private boolean mouseClicked = false;

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        mouseClicked = org.lwjgl.input.Mouse.isButtonDown(0);
    }
}

