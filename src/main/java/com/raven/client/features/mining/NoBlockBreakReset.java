package com.raven.client.features.mining;

import com.raven.client.features.Feature;
import com.raven.client.features.FeatureCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

public class NoBlockBreakReset extends Feature {

    private final Minecraft mc = Minecraft.getMinecraft();
    private ItemStack lastHeldItem = null;
    private float lastBlockDamage = 0.0f;

    public NoBlockBreakReset() {
        super("No Block Reset", FeatureCategory.MINING);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        ItemStack currentItem = mc.thePlayer.getHeldItem();

        if (lastHeldItem != null && currentItem != null && !ItemStack.areItemStacksEqual(lastHeldItem, currentItem)) {
            try {
                PlayerControllerMP controller = mc.playerController;
                Field damageField = PlayerControllerMP.class.getDeclaredField("curBlockDamageMP");
                damageField.setAccessible(true);

                float currentDamage = (float) damageField.get(controller);

                // If there's meaningful progress and it was reset, restore it
                if (currentDamage == 0.0f && lastBlockDamage > 0.0f) {
                    damageField.set(controller, lastBlockDamage);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Update last state
        lastHeldItem = currentItem != null ? currentItem.copy() : null;

        try {
            Field damageField = PlayerControllerMP.class.getDeclaredField("curBlockDamageMP");
            damageField.setAccessible(true);
            lastBlockDamage = (float) damageField.get(mc.playerController);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}