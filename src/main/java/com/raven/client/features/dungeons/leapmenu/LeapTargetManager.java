package com.raven.client.features.dungeons.leapmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class LeapTargetManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static String getRecommendedLeap() {
        // Replace this with real logic: terminal status, role check, etc.
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!player.getName().equals(mc.thePlayer.getName())) {
                return player.getName();
            }
        }
        return null;
    }
}
