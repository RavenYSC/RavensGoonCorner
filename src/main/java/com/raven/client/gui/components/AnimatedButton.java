package com.raven.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

public class AnimatedButton extends GuiButton {
    
    public static interface IClickListener {
        void onClick(AnimatedButton button);
    }
    
    private IClickListener clickListener;
    private float hoverProgress = 0f;
    private int backgroundColor;
    private int hoverColor;
    private boolean hovering = false;
    
    public AnimatedButton(int id, int x, int y, int width, int height, String label, 
                         int bgColor, int hoverCol) {
        super(id, x, y, width, height, label);
        this.backgroundColor = bgColor;
        this.hoverColor = hoverCol;
    }
    
    public void setClickListener(IClickListener listener) {
        this.clickListener = listener;
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        
        this.hovering = mouseX >= this.xPosition && mouseY >= this.yPosition && 
                       mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
        
        // Smooth hover animation
        if (hovering && hoverProgress < 1f) {
            hoverProgress += 0.1f;
        } else if (!hovering && hoverProgress > 0f) {
            hoverProgress -= 0.1f;
        }
        hoverProgress = Math.max(0f, Math.min(1f, hoverProgress));
        
        // Interpolate color
        int r1 = (backgroundColor >> 16) & 0xFF;
        int g1 = (backgroundColor >> 8) & 0xFF;
        int b1 = backgroundColor & 0xFF;
        int a1 = (backgroundColor >> 24) & 0xFF;
        
        int r2 = (hoverColor >> 16) & 0xFF;
        int g2 = (hoverColor >> 8) & 0xFF;
        int b2 = hoverColor & 0xFF;
        int a2 = (hoverColor >> 24) & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * hoverProgress);
        int g = (int) (g1 + (g2 - g1) * hoverProgress);
        int b = (int) (b1 + (b2 - b1) * hoverProgress);
        int a = (int) (a1 + (a2 - a1) * hoverProgress);
        
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        
        // Draw button with slight elevation on hover
        int elevation = (int) (hoverProgress * 4);
        drawCustomRect(this.xPosition - elevation, this.yPosition - elevation, 
                this.xPosition + this.width + elevation, this.yPosition + this.height + elevation, 0x22000000);
        
        drawCustomRect(this.xPosition, this.yPosition, this.xPosition + this.width, 
                this.yPosition + this.height, color);
        
        // Draw text
        int textColor = 0xFFFFFF;
        int strWidth = mc.fontRendererObj.getStringWidth(this.displayString);
        mc.fontRendererObj.drawString(this.displayString, 
            this.xPosition + (this.width - strWidth) / 2, 
            this.yPosition + (this.height - 8) / 2, 
            textColor);
    }
    
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            if (clickListener != null) {
                clickListener.onClick(this);
            }
            return true;
        }
        return false;
    }
    
    private void drawCustomRect(int x1, int y1, int x2, int y2, int color) {
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
