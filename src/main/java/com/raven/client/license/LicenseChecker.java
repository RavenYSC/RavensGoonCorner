package com.raven.client.license;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class LicenseChecker {
    
    private static boolean hasShownLicenseScreen = false;
    private static boolean pendingLicenseScreen = false;
    private static int ticksWaited = 0;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        // When main menu opens and we haven't validated yet
        if (event.gui instanceof GuiMainMenu && !hasShownLicenseScreen) {
            LicenseManager license = LicenseManager.getInstance();
            
            // Check if we have a valid stored license
            if (!license.isValidated()) {
                // Schedule the license screen to open next tick (avoids GL state issues)
                pendingLicenseScreen = true;
                hasShownLicenseScreen = true;
            } else {
                // Already validated, allow normal flow
                hasShownLicenseScreen = true;
            }
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        final Minecraft mc = Minecraft.getMinecraft();
        
        // Handle pending license screen (scheduled from GuiOpenEvent)
        if (pendingLicenseScreen) {
            pendingLicenseScreen = false;
            mc.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    mc.displayGuiScreen(new GuiLicenseKey());
                }
            });
            return;
        }
        
        // If we haven't shown license screen yet and we're on main menu
        if (!hasShownLicenseScreen && mc.currentScreen instanceof GuiMainMenu) {
            LicenseManager license = LicenseManager.getInstance();
            
            if (!license.isValidated()) {
                hasShownLicenseScreen = true;
                mc.addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        mc.displayGuiScreen(new GuiLicenseKey());
                    }
                });
            } else {
                hasShownLicenseScreen = true;
            }
        }
        
        // If license screen was closed but license isn't valid, show it again
        if (hasShownLicenseScreen && mc.currentScreen == null && !LicenseManager.getInstance().isValidated()) {
            ticksWaited++;
            if (ticksWaited > 20) { // Wait 1 second before checking
                // Only redirect to license screen if not in-game
                if (mc.theWorld == null) {
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            mc.displayGuiScreen(new GuiLicenseKey());
                        }
                    });
                    ticksWaited = 0;
                }
            }
        } else {
            ticksWaited = 0;
        }
    }
    
    /**
     * Resets the license check state (for testing purposes)
     */
    public static void reset() {
        hasShownLicenseScreen = false;
        pendingLicenseScreen = false;
        ticksWaited = 0;
    }
}
