package com.raven.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

public class CustomButton extends GuiButton {
    
    private float hoverProgress = 0f;
    private int backgroundColor;
    private int hoverColor;
    private int textColor = 0xFFFFFF;
    private int borderColor = 0xFF555555;
    private boolean hovering = false;
    private int cornerRadius = 3;
    
    public CustomButton(int id, int x, int y, int width, int height, String label) {
        this(id, x, y, width, height, label, 0xFF2a2a2a, 0xFF3a3a3a);
    }
    
    public CustomButton(int id, int x, int y, int width, int height, String label, 
                       int bgColor, int hoverCol) {
        super(id, x, y, width, height, label);
        this.backgroundColor = bgColor;
        this.hoverColor = hoverCol;
    }
    
    public CustomButton setTextColor(int color) {
        this.textColor = color;
        return this;
    }
    
    public CustomButton setBorderColor(int color) {
        this.borderColor = color;
        return this;
    }
    
    public CustomButton setCornerRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        
        this.hovering = mouseX >= this.xPosition && mouseY >= this.yPosition && 
                       mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
        
        // Smooth hover animation
        if (hovering && hoverProgress < 1f) {
            hoverProgress += 0.15f;
        } else if (!hovering && hoverProgress > 0f) {
            hoverProgress -= 0.15f;
        }
        hoverProgress = Math.max(0f, Math.min(1f, hoverProgress));
        
        // Interpolate colors
        int bgColor = lerpColor(backgroundColor, hoverColor, hoverProgress);
        int borderCol = lerpColor(borderColor, 0xFF888888, hoverProgress);
        
        // Draw shadow on hover
        if (hoverProgress > 0.1f) {
            int shadowAlpha = (int) (40 * hoverProgress);
            drawRectCustom(this.xPosition - 1, this.yPosition - 1,
                    this.xPosition + this.width + 1, this.yPosition + this.height + 1,
                    ((shadowAlpha << 24) | 0x000000));
        }
        
        // Draw main button background
        drawRectCustom(this.xPosition, this.yPosition,
                this.xPosition + this.width, this.yPosition + this.height,
                bgColor);
        
        // Draw border
        drawBorder(this.xPosition, this.yPosition,
                this.xPosition + this.width, this.yPosition + this.height,
                borderCol, 1);
        
        // Draw text
        int strWidth = mc.fontRendererObj.getStringWidth(this.displayString);
        int textX = this.xPosition + (this.width - strWidth) / 2;
        int textY = this.yPosition + (this.height - 8) / 2;
        mc.fontRendererObj.drawString(this.displayString, textX, textY, textColor);
    }
    
    private int lerpColor(int color1, int color2, float progress) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        int a = (int) (a1 + (a2 - a1) * progress);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private void drawBorder(int x1, int y1, int x2, int y2, int color, int width) {
        drawRectCustom(x1, y1, x2, y1 + width, color);
        drawRectCustom(x1, y2 - width, x2, y2, color);
        drawRectCustom(x1, y1, x1 + width, y2, color);
        drawRectCustom(x2 - width, y1, x2, y2, color);
    }
    
    private void drawRectCustom(int x1, int y1, int x2, int y2, int color) {
        if (x1 < x2) {
            int i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            int j = y1;
            y1 = y2;
            y2 = j;
        }
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
