package com.raven.client;

import com.raven.client.features.dungeons.leapmenu.LeapMenu;
import com.raven.client.gui.GuiOpener;
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

                if (chestName.contains("Leap") || chestName.contains("Infinileap")) {
                    event.setCanceled(true); // Cancel default chest
                    GuiOpener.openGuiNextTick(new LeapMenu()); // Show custom leap menu
                }
            } catch (Exception e) {
                // Silently ignore errors in GUI interception
            }
        }
    }
}