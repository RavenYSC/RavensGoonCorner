package com.raven.client.features.dungeons.chatmessages;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.raven.client.features.Feature;
import com.raven.client.features.FeatureCategory;
import com.raven.client.features.dungeons.AutoKick.AutoKick;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class PositionalMessages extends Feature {

    private final Minecraft mc = Minecraft.getMinecraft();
    private TriggerConfig config;

    public PositionalMessages() {
        super("Positional Messages", FeatureCategory.DUNGEONS);
        MinecraftForge.EVENT_BUS.register(this);
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File("config/RavenYSC Configs/chat_triggers.json");

        try {
            String rawJson = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
            this.config = new Gson().fromJson(rawJson, TriggerConfig.class);

            // Optional: default thresholds for AutoKick (unrelated to coordinate/chat triggers)
            JsonObject defaults = new JsonObject();
            JsonObject floorDefaults = new JsonObject();
            floorDefaults.addProperty("F7", 420);
            floorDefaults.addProperty("F6", 360);
            floorDefaults.addProperty("M7", 540);
            defaults.add("floorThresholds", floorDefaults);

            for (Map.Entry<String, JsonElement> entry : floorDefaults.entrySet()) {
                AutoKick.floorThresholds.put(entry.getKey().toUpperCase(), entry.getValue().getAsInt());
            }

            if (config.coordinateTriggers != null) {
                for (TriggerConfig.CoordinateTrigger trig : config.coordinateTriggers) {
                    trig.normalize();
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to load chat_triggers.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled() || config == null || config.chatTriggers == null) return;
        // Skip when not in a world (main menu)
        if (mc.theWorld == null || mc.thePlayer == null) return;

        String msg = event.message.getUnformattedText();
        for (TriggerConfig.ChatTrigger chatTrigger : config.chatTriggers) {
            if (msg.contains(chatTrigger.contains)) {
                mc.thePlayer.sendChatMessage(chatTrigger.response);
                System.out.println("[Trigger] Chat matched: " + chatTrigger.contains);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || config == null || config.coordinateTriggers == null) return;

        BlockPos pos = mc.thePlayer.getPosition();
        for (TriggerConfig.CoordinateTrigger trigger : config.coordinateTriggers) {
            if (trigger.shouldTrigger(pos)) {
                mc.thePlayer.sendChatMessage(trigger.message);
                System.out.println("[Trigger] Entered region: " + trigger.name);
            }
        }
    }

    public static class TriggerConfig {
        public List<CoordinateTrigger> coordinateTriggers;
        public List<ChatTrigger> chatTriggers;

        public static class CoordinateTrigger {
            public String name;
            public int[] min;
            public int[] max;
            public String message;

            private boolean wasInside = false;

            public void normalize() {
                for (int i = 0; i < 3; i++) {
                    if (min[i] > max[i]) {
                        int temp = min[i];
                        min[i] = max[i];
                        max[i] = temp;
                    }
                }
            }

            public boolean isWithin(BlockPos pos) {
                return pos.getX() >= min[0] && pos.getX() <= max[0] &&
                       pos.getY() >= min[1] && pos.getY() <= max[1] &&
                       pos.getZ() >= min[2] && pos.getZ() <= max[2];
            }

            public boolean shouldTrigger(BlockPos pos) {
                boolean isInside = isWithin(pos);
                if (isInside && !wasInside) {
                    wasInside = true;
                    return true;
                } else if (!isInside) {
                    wasInside = false;
                }
                return false;
            }
        }

        public static class ChatTrigger {
            public String contains;
            public String response;
        }
    }
}
