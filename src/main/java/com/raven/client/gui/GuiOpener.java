package com.raven.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class GuiOpener {

    private static GuiScreen pendingGui = null;
    private static boolean shouldOpen = false;

    public static void openGuiNextTick(GuiScreen gui) {
        pendingGui = gui;
        shouldOpen = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        // Don't process GUI opens when not in a world (prevents main menu flickering)
        if (mc.theWorld == null) {
            shouldOpen = false;
            pendingGui = null;
            return;
        }
        
        if (shouldOpen) {
            try {
                mc.displayGuiScreen(pendingGui);
                shouldOpen = false;
                pendingGui = null;
            } catch (Exception e) {
                System.err.println("[GuiOpener] Error: " + e.getMessage());
                shouldOpen = false;
                pendingGui = null;
            }
        }
    }
}
