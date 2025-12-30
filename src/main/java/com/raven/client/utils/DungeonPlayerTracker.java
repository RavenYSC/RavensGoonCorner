package com.raven.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class DungeonPlayerTracker {

    private static Minecraft mc;
    
    private static Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }

    // Maps clean IGN to their coordinates
    public static final Map<String, PlayerPosition> playerPositions = new HashMap<>();

    public static void init() {
        // Register this to the Forge event bus in your mod entry class
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = getMc();
        if (mc.theWorld == null || mc.thePlayer == null || !isInDungeon()) return;

        playerPositions.clear();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player instanceof EntityPlayerSP) continue;

            String displayName = player.getDisplayName().getFormattedText();
            String cleanName = stripRankColor(displayName);

            boolean isDead = player.isDead || player.getHealth() <= 0;

            playerPositions.put(cleanName, new PlayerPosition(
                player.posX, player.posY, player.posZ, isDead
            ));
        }
    }

    private boolean isInDungeon() {
        Minecraft mc = getMc();
        if (mc.theWorld == null) return false;

        ScoreObjective sidebar = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
        return sidebar != null && sidebar.getDisplayName().toLowerCase().contains("dungeon");
    }

    private String stripRankColor(String name) {
        // Remove ranks and formatting codes
        return name.replaceAll("�.", "").trim();
    }

    public static class PlayerPosition {
        public final double x, y, z;
        public final boolean isDead;

        public PlayerPosition(double x, double y, double z, boolean isDead) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.isDead = isDead;
        }
    }
}
