package com.raven.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.client.event.GuiOpenEvent;

public class GuiInterceptor {

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Skip when not in a world (main menu)
        if (net.minecraft.client.Minecraft.getMinecraft().theWorld == null) return;
        if (event == null || event.gui == null) return;
        GuiScreen gui = event.gui;

        if (gui instanceof GuiChest) {
            try {
                IInventory inv = ((ContainerChest) ((GuiChest) gui).inventorySlots).getLowerChestInventory();
                if (inv == null) return;
                String chestName = inv.getDisplayName().getUnformattedText();
                if (chestName == null) return;

                // Leap menu disabled for now
                // if (chestName.contains("Leap") || chestName.contains("Infinileap")) {
                //     event.setCanceled(true);
                //     GuiOpener.openGuiNextTick(new LeapMenu());
                // }
            } 
            catch (Exception e) {
                
            }
        }
    }
}