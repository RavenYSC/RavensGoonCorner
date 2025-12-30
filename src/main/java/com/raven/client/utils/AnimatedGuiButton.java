package com.raven.client.utils;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class AnimatedGuiButton {
    private Minecraft mc;
    
    private Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }

    private final int x, y, width, height;
    private final String label;
    private boolean hovered = false;
    private boolean toggled = false;
    private float animationProgress = 0f;
    private final float textScale = 1.8f;

    public AnimatedGuiButton(int x, int y, int width, int height, String label) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
    }

    public void renderRaw(int rawMouseX, int rawMouseY) {
        Minecraft mc = getMc();
        hovered = isHovered(rawMouseX, rawMouseY);

        if (hovered && animationProgress < 1.0f) {
            animationProgress += 0.1f;
        } else if (!hovered && animationProgress > 0.0f) {
            animationProgress -= 0.1f;
        }

        int bgColor = toggled ? 0xFF2ECC71 : 0xFF444444;
        int borderColor = hovered ? 0xFFFFFFFF : 0xFF888888;

        GlStateManager.disableTexture2D();
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        Gui.drawRect(x, y, x + width, y + height, bgColor);
        GlStateManager.enableTexture2D();

        int textColor = 0xFFFFFF;
        drawScaledString(label,
                x + (width / 2) - mc.fontRendererObj.getStringWidth(label) / 2,
                y + (height / 2) - (mc.fontRendererObj.FONT_HEIGHT / 2), textColor, true);
    }
    
    private void drawScaledString(String text, int x, int y, int color, boolean centered) {
        Minecraft mc = getMc();
        GL11.glPushMatrix();
		GL11.glScalef(textScale, textScale, 1.0f);
        if (centered) {
            mc.fontRendererObj.drawString(text, (int) ((x / textScale) - (mc.fontRendererObj.getStringWidth(text) / 2)), (int) (y / textScale), color);
        } else {
            mc.fontRendererObj.drawString(text, (int) (x / textScale), (int) (y / textScale), color);
        }
        GL11.glPopMatrix();
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void toggle() {
        toggled = !toggled;
    }

    public boolean isToggled() {
        return toggled;
    }
}