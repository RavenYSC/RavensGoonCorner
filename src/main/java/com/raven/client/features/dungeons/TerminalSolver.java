package com.raven.client.features.dungeons;

import com.raven.client.features.Feature;
import com.raven.client.features.FeatureCategory;
import com.raven.client.gui.GuiOpener;
import com.raven.client.gui.GuiTerminalInterceptor;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TerminalSolver extends Feature {

    public TerminalSolver() {
        super("Terminal Solver", FeatureCategory.DUNGEONS);
        this.setEnabled(true);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Skip when not in a world (main menu)
        if (net.minecraft.client.Minecraft.getMinecraft().theWorld == null) return;
        if (event == null || event.gui == null) return;
        if (!isEnabled()) return; // Don't intercept if feature is disabled
        
        if (event.gui instanceof GuiChest) {
            try {
                IInventory chest = ((ContainerChest) ((GuiChest) event.gui).inventorySlots).getLowerChestInventory();
                if (chest == null) return;
                String chestName = chest.getDisplayName().getUnformattedText();
                if (chestName == null) return;

                if (chestName.contains("Click in Order") ||
                    chestName.contains("Correct All") ||
                    chestName.contains("Maze") ||
                    chestName.contains("Numbers") ||
                    chestName.contains("Start Terminal")) {

                    GuiOpener.openGuiNextTick(new GuiTerminalInterceptor());
                    event.setCanceled(true);
                }
            } catch (Exception e) {
                // Silently ignore errors in GUI interception
            }
        }
    }
}