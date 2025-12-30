package com.raven.client.updater;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Handles showing update notifications when an update is available
 */
public class UpdateHandler {
    
    private boolean hasShownNotification = false;
    private int tickCounter = 0;
    private static final int DELAY_TICKS = 100; // Wait 5 seconds after login
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        // Only show once player is in world
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        
        // Don't show if already shown
        if (hasShownNotification) {
            return;
        }
        
        // Wait a bit after joining world
        tickCounter++;
        if (tickCounter < DELAY_TICKS) {
            return;
        }
        
        // Check if update is available
        UpdateChecker checker = UpdateChecker.getInstance();
        if (checker.isUpdateAvailable()) {
            hasShownNotification = true;
            
            // Show notification GUI
            mc.addScheduledTask(() -> {
                mc.displayGuiScreen(new GuiUpdateNotification(mc.currentScreen));
            });
        }
    }
}
