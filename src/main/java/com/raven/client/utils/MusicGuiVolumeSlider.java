package com.raven.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class MusicGuiVolumeSlider extends GuiButton {

    private float volume; // 0.0 to 1.0
    private boolean dragging = false;

    public MusicGuiVolumeSlider(int id, int x, int y, int width) {
        super(id, x, y, width, 20, "");
        this.volume = 1.0f;
        updateDisplayString();
    }

    public void setVolume(float value) {
        this.volume = Math.max(0f, Math.min(1f, value));
        updateDisplayString();
    }

    public float getVolume() {
        return volume;
    }

    private void updateDisplayString() {
        this.displayString = "Volume: " + (int)(volume * 100) + "%";
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible && dragging) {
            float rel = (float)(mouseX - this.xPosition) / (float)this.width;
            setVolume(rel);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            dragging = true;
            float rel = (float)(mouseX - this.xPosition) / (float)this.width;
            setVolume(rel);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        super.drawButton(mc, mouseX, mouseY);

        if (this.visible) {
            int sliderX = this.xPosition + (int)(volume * (this.width - 8));
            Gui.drawRect(sliderX, this.yPosition + 2, sliderX + 8, this.yPosition + this.height - 2, 0xFFFFFFFF);
        }
    }
}
