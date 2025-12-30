package com.raven.client.gui.components;

import org.lwjgl.opengl.GL11;

public class RenderUtils {
    
    public static void drawGradient(int x1, int y1, int x2, int y2, int color1, int color2) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        float a1 = (float)(color1 >> 24 & 255) / 255.0F;
        float r1 = (float)(color1 >> 16 & 255) / 255.0F;
        float g1 = (float)(color1 >> 8 & 255) / 255.0F;
        float b1 = (float)(color1 & 255) / 255.0F;
        
        float a2 = (float)(color2 >> 24 & 255) / 255.0F;
        float r2 = (float)(color2 >> 16 & 255) / 255.0F;
        float g2 = (float)(color2 >> 8 & 255) / 255.0F;
        float b2 = (float)(color2 & 255) / 255.0F;
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(r1, g1, b1, a1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glColor4f(r2, g2, b2, a2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        // Reset color to white for text rendering
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
    
    public static void drawRect(int x1, int y1, int x2, int y2, int color) {
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
        // Reset color to white for text rendering
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
    
    public static void drawBorder(int x1, int y1, int x2, int y2, int color, int width) {
        drawRect(x1, y1, x2, y1 + width, color);
        drawRect(x1, y2 - width, x2, y2, color);
        drawRect(x1, y1, x1 + width, y2, color);
        drawRect(x2 - width, y1, x2, y2, color);
    }
    
    public static void drawBox(int x1, int y1, int x2, int y2, int bgColor, int borderColor, int borderWidth) {
        drawRect(x1, y1, x2, y2, bgColor);
        drawBorder(x1, y1, x2, y2, borderColor, borderWidth);
    }
    
    public static void drawShadow(int x1, int y1, int x2, int y2, int alpha) {
        drawRect(x1 - 2, y1 - 2, x2 + 2, y2 + 2, (alpha << 24) | 0x000000);
    }
}
