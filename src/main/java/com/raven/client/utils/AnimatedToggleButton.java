package com.raven.client.utils;

import com.raven.client.features.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class AnimatedToggleButton {

    private int x, y, width, height;
    private boolean state;
    private float animation = 0.0f;

    private final Feature feature;

    public AnimatedToggleButton(int x, int y, int width, int height, Feature feature) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.feature = feature;
        this.state = feature.isEnabled(); // Sync state
    }

    public void draw(Minecraft mc) {
        FontRenderer fr = mc.fontRendererObj;

        // Smooth animation (spring-like interpolation)
        float target = state ? 1.0f : 0.0f;
        animation += (target - animation) * 0.25f;

        // === Background (track)
        Gui.drawRect(x, y, x + width, y + height, 0xFF222222);

        // === Knob
        int knobSize = height;
        int knobX = x + (int) ((width - knobSize) * animation);
        int knobColor = state ? 0xFF00FF00 : 0xFF777777;
        Gui.drawRect(knobX, y, knobX + knobSize, y + height, knobColor);
    }

    public void handleClick(int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            feature.toggle();
            state = feature.isEnabled();
        }
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }

    // Layout setters
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }

    // Optional accessors
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean getState() { return state; }
}
