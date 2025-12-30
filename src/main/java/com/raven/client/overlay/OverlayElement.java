package com.raven.client.overlay;

import com.raven.client.utils.ChromaText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class OverlayElement {

    public String label;
    public int x, y;
    public float scale = 1.0f;

    private Minecraft mc;
    
    private Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }

    public OverlayElement(String label, int x, int y) {
        this.label = label;
        this.x = x;
        this.y = y;
    }

    public void render(int mouseX, int mouseY) {
        try {
            String displayText = getText();

            // Save GL state before any transformations
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(x, y, 0);
            
            if (scale != 1.0f) {
                net.minecraft.client.renderer.GlStateManager.scale(scale, scale, 1);
            }

            ChromaText.drawChromaString(displayText, 0, 0, true);

            // Restore GL state
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        } catch (Exception e) {
            System.err.println("[OverlayElement] Error rendering: " + e.getMessage());
            try {
                net.minecraft.client.renderer.GlStateManager.popMatrix();
            } catch (Exception ignored) {}
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        FontRenderer font = getMc().fontRendererObj;
        String displayText = getText();
        int width = (int) (font.getStringWidth(displayText) * scale);
        int height = (int) (font.FONT_HEIGHT * scale);

        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private String getText() {
        switch (label.toLowerCase()) {
            case "fps":
                return "FPS: " + Minecraft.getDebugFPS();
            case "ping":
                return "Ping: " + getPing() + "ms";
            default:
                return label;
        }
    }

    private int getPing() {
        try {
            Minecraft mc = getMc();
            return mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
        } catch (Exception e) {
            return -1;
        }
    }
}
