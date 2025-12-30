package com.raven.client.license;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;

public class GuiLicenseKey extends GuiScreen {
    
    private GuiTextField keyField;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private boolean isValidating = false;
    private boolean validationComplete = false;
    private long animationTick = 0;
    
    // UI Constants
    private int panelWidth = 350;
    private int panelHeight = 200;
    private int panelX;
    private int panelY;
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        
        // License key text field
        keyField = new GuiTextField(0, fontRendererObj, panelX + 25, panelY + 80, panelWidth - 50, 20);
        keyField.setMaxStringLength(50);
        keyField.setFocused(true);
        
        // Check if we have a stored key - auto-validate it
        LicenseManager license = LicenseManager.getInstance();
        if (license.hasStoredKey()) {
            keyField.setText(license.getStoredKey());
            statusMessage = "Validating stored key...";
            statusColor = 0xFFFF55;
            validateKeyAsync(license.getStoredKey());
        }
        
        buttonList.clear();
        
        // Validate button
        buttonList.add(new GuiButton(0, panelX + 25, panelY + 115, 145, 20, "Validate Key"));
        
        // Exit button
        buttonList.add(new GuiButton(1, panelX + panelWidth - 170, panelY + 115, 145, 20, "Exit Game"));
    }
    
    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        animationTick++;
        
        // Reset GL state at start
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Dark background
        drawDefaultBackground();
        
        // Reset color after background
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Panel shadow
        drawRect(panelX + 4, panelY + 4, panelX + panelWidth + 4, panelY + panelHeight + 4, 0x80000000);
        
        // Panel background
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1a1a2e);
        
        // Panel border
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF4a9eff);
        drawRect(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, 0xFF4a9eff);
        drawRect(panelX, panelY, panelX + 2, panelY + panelHeight, 0xFF4a9eff);
        drawRect(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF4a9eff);
        
        // Header
        drawRect(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 40, 0xFF2a2a4e);
        
        // Reset color before text
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Title
        String title = "\u00A7l\u00A761:1 Client \u00A7f\u00A7lLicense Verification";
        drawCenteredString(fontRendererObj, title, width / 2, panelY + 15, 0xFFFFFF);
        
        // Instructions
        drawCenteredString(fontRendererObj, "Enter your license key to continue", width / 2, panelY + 55, 0xAAAAAA);
        drawCenteredString(fontRendererObj, "Get a key from our Discord bot: /getkey", width / 2, panelY + 67, 0x888888);
        
        // Reset color before text field
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Draw text field
        keyField.drawTextBox();
        
        // Status message with animation
        if (!statusMessage.isEmpty()) {
            String displayMsg = statusMessage;
            if (isValidating) {
                // Animated dots
                int dots = (int)((animationTick / 10) % 4);
                displayMsg = statusMessage;
                for (int i = 0; i < dots; i++) {
                    displayMsg += ".";
                }
            }
            drawCenteredString(fontRendererObj, displayMsg, width / 2, panelY + 145, statusColor);
        }
        
        // Footer info
        drawCenteredString(fontRendererObj, "\u00A77Unauthorized distribution is prohibited", width / 2, panelY + panelHeight - 20, 0x666666);
        
        // Reset GL state before super call
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            // Validate button
            if (!isValidating) {
                String key = keyField.getText().trim();
                if (key.isEmpty()) {
                    statusMessage = "Please enter a license key";
                    statusColor = 0xFF5555;
                } else {
                    validateKeyAsync(key);
                }
            }
        } else if (button.id == 1) {
            // Exit button
            Minecraft.getMinecraft().shutdown();
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        keyField.textboxKeyTyped(typedChar, keyCode);
        
        // Enter key to validate
        if (keyCode == Keyboard.KEY_RETURN && !isValidating) {
            String key = keyField.getText().trim();
            if (!key.isEmpty()) {
                validateKeyAsync(key);
            }
        }
        
        // Don't allow escape to close this GUI
        if (keyCode != Keyboard.KEY_ESCAPE) {
            super.keyTyped(typedChar, keyCode);
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        keyField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    private void validateKeyAsync(String key) {
        isValidating = true;
        statusMessage = "Validating";
        statusColor = 0xFFFF55;
        
        // Run validation in a separate thread to not freeze the game
        new Thread(() -> {
            boolean valid = LicenseManager.getInstance().validateKey(key);
            
            // Update UI on main thread
            Minecraft.getMinecraft().addScheduledTask(() -> {
                isValidating = false;
                
                if (valid) {
                    statusMessage = "\u00A7aLicense validated! Welcome, " + LicenseManager.getInstance().getLicensedUsername();
                    statusColor = 0x55FF55;
                    validationComplete = true;
                    
                    // Close this GUI after a short delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Minecraft.getMinecraft().addScheduledTask(() -> {
                                // Return to main menu
                                Minecraft.getMinecraft().displayGuiScreen(null);
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    statusMessage = LicenseManager.getInstance().getValidationError();
                    if (statusMessage == null || statusMessage.isEmpty()) {
                        statusMessage = "Invalid license key";
                    }
                    statusColor = 0xFF5555;
                }
            });
        }).start();
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
