package com.raven.client.gui;

import net.minecraft.client.gui.GuiScreen;

public class GuiTerminalInterceptor extends GuiScreen {
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Custom Terminal UI", width / 2, height / 2 - 10, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
