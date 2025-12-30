package com.raven.client.updater;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import com.raven.client.RavenClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class GuiUpdateNotification extends GuiScreen {
    
    private final GuiScreen parentScreen;
    private final UpdateChecker updateChecker;
    private boolean downloadStarted = false;
    private boolean downloadComplete = false;
    private String statusMessage = "";
    
    private int panelWidth = 400;
    private int panelHeight = 220;
    private int panelX;
    private int panelY;
    
    public GuiUpdateNotification(GuiScreen parent) {
        this.parentScreen = parent;
        this.updateChecker = UpdateChecker.getInstance();
    }
    
    @Override
    public void initGui() {
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        
        buttonList.clear();
        
        // Update Now button
        buttonList.add(new GuiButton(0, panelX + 20, panelY + panelHeight - 50, 170, 20, "Update Now"));
        
        // Later button
        buttonList.add(new GuiButton(1, panelX + panelWidth - 190, panelY + panelHeight - 50, 170, 20, "Remind Me Later"));
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dim background
        drawDefaultBackground();
        
        // Reset GL state
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Panel shadow
        drawRect(panelX + 4, panelY + 4, panelX + panelWidth + 4, panelY + panelHeight + 4, 0x80000000);
        
        // Panel background
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1a1a2e);
        
        // Panel border
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF55FF55);
        drawRect(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, 0xFF55FF55);
        drawRect(panelX, panelY, panelX + 2, panelY + panelHeight, 0xFF55FF55);
        drawRect(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF55FF55);
        
        // Header
        drawRect(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 40, 0xFF2a2a4e);
        
        // Reset color for text
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Title
        String title = "\u00A7a\u00A7lUpdate Available!";
        drawCenteredString(fontRendererObj, title, width / 2, panelY + 15, 0xFFFFFF);
        
        // Version info
        String currentVer = "Current Version: \u00A7c" + RavenClient.VERSION;
        String latestVer = "Latest Version: \u00A7a" + updateChecker.getLatestVersion();
        
        drawCenteredString(fontRendererObj, currentVer, width / 2, panelY + 55, 0xFFFFFF);
        drawCenteredString(fontRendererObj, latestVer, width / 2, panelY + 70, 0xFFFFFF);
        
        // Release notes preview
        String notes = updateChecker.getReleaseNotes();
        if (notes != null && !notes.isEmpty()) {
            drawString(fontRendererObj, "\u00A7nRelease Notes:", panelX + 20, panelY + 95, 0xAAAAAA);
            
            // Truncate notes if too long
            if (notes.length() > 100) {
                notes = notes.substring(0, 97) + "...";
            }
            
            // Word wrap
            int y = panelY + 110;
            String[] words = notes.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (fontRendererObj.getStringWidth(line + word) > panelWidth - 40) {
                    drawString(fontRendererObj, line.toString(), panelX + 20, y, 0x888888);
                    y += 10;
                    line = new StringBuilder();
                    if (y > panelY + 145) break;
                }
                line.append(word).append(" ");
            }
            if (line.length() > 0 && y <= panelY + 145) {
                drawString(fontRendererObj, line.toString(), panelX + 20, y, 0x888888);
            }
        }
        
        // Download progress or status
        if (downloadStarted) {
            if (updateChecker.isDownloading()) {
                int progress = updateChecker.getDownloadProgress();
                statusMessage = "Downloading... " + progress + "%";
                
                // Progress bar
                int barX = panelX + 20;
                int barY = panelY + panelHeight - 75;
                int barWidth = panelWidth - 40;
                int barHeight = 15;
                
                drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
                drawRect(barX + 1, barY + 1, barX + 1 + (int)((barWidth - 2) * progress / 100.0), barY + barHeight - 1, 0xFF55FF55);
            } else if (downloadComplete) {
                statusMessage = "\u00A7aDownload complete! Restart Minecraft to apply the update.";
            }
            
            drawCenteredString(fontRendererObj, statusMessage, width / 2, panelY + panelHeight - 75, 0xFFFFFF);
        }
        
        // Reset color before buttons
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Update button text based on state
        if (downloadStarted && downloadComplete) {
            buttonList.get(0).displayString = "Restart Minecraft";
        } else if (downloadStarted && updateChecker.isDownloading()) {
            buttonList.get(0).enabled = false;
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            // Update Now / Restart
            if (downloadComplete) {
                // Close Minecraft to apply update
                Minecraft.getMinecraft().shutdown();
            } else if (!downloadStarted) {
                downloadStarted = true;
                new Thread(() -> {
                    boolean success = updateChecker.downloadUpdate();
                    downloadComplete = success;
                    if (!success) {
                        statusMessage = "\u00A7cDownload failed. Check console for details.";
                    }
                }, "RavenClient-Downloader").start();
            }
        } else if (button.id == 1) {
            // Later - return to parent screen
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && !updateChecker.isDownloading()) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
