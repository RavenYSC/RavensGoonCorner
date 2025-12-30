package com.raven.client.features.dungeons.AutoKick;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raven.client.features.Feature;
import com.raven.client.features.FeatureCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoKick extends Feature {

    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final Map<String, Integer> floorThresholds = new HashMap<>();
    public static String currentFloor = "f7";

    static {
    	floorThresholds.put("f1", 360);
    	floorThresholds.put("f2", 360);
    	floorThresholds.put("f3", 360);
    	floorThresholds.put("f4", 360);
    	floorThresholds.put("f5", 360);
    	floorThresholds.put("f6", 360);
        floorThresholds.put("f7", 260);
        floorThresholds.put("m1", 360);
        floorThresholds.put("m2", 360);
        floorThresholds.put("m3", 360);
        floorThresholds.put("m4", 360);
        floorThresholds.put("m5", 360);
        floorThresholds.put("m6", 160);
        floorThresholds.put("m7", 360);
    }

    public AutoKick() {
        super("Auto Kick", FeatureCategory.QOL);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        // Skip when not in a world (main menu)
        if (net.minecraft.client.Minecraft.getMinecraft().theWorld == null) return;

        String msg = event.message.getUnformattedText();

        // Clean known formatting and symbols (rank brackets, arrows, color codes)
        String clean = msg.replaceAll("�.", "") // Minecraft color codes
                          .replaceAll("[<>\\[\\](){}]", "") // Remove brackets
                          .replaceAll("[^\\x00-\\x7F]", "") // Remove non-ASCII (stars, emojis)
                          .trim();

        // Match: "Party Finder ... <IGN> joined the dungeon group!"
        Pattern pattern = Pattern.compile("Party Finder .*? ([a-zA-Z0-9_]{3,16}) joined the dungeon group!");
        Matcher matcher = pattern.matcher(clean);

        if (matcher.find()) {
            String player = matcher.group(1);
            send("�7[�bAutoKick�7] �eMatched IGN: �b" + player);
            fetchAndCheckPlayerPB(player);
        }
    }

    private void fetchAndCheckPlayerPB(String player) {
        new Thread(() -> {
            try {
                String uuidJson = readUrl("https://api.mojang.com/users/profiles/minecraft/" + player);
                JsonObject uuidObj = new JsonParser().parse(uuidJson).getAsJsonObject();

                if (!uuidObj.has("id")) {
                    send("�cCould not retrieve UUID for �e" + player);
                    return;
                }

                String uuid = uuidObj.get("id").getAsString();
                String key = "1a9f6d56-e105-4dbe-921c-8ccc01e39ac8";
                String sbJson = readUrl("https://api.hypixel.net/v2/skyblock/profiles?key=" + key + "&uuid=" + uuid);
                JsonObject profilesObj = new JsonParser().parse(sbJson).getAsJsonObject();

                if (!profilesObj.has("profiles")) {
                    send("�cNo SkyBlock profiles found for �e" + player);
                    return;
                }

                JsonObject selectedProfile = null;
                for (JsonElement profileElement : profilesObj.getAsJsonArray("profiles")) {
                    JsonObject profile = profileElement.getAsJsonObject();
                    if (profile.has("selected") && profile.get("selected").getAsBoolean()) {
                        selectedProfile = profile;
                        break;
                    }
                }

                if (selectedProfile == null) {
                    send("�c[AutoKick] Could not determine latest SkyBlock profile.");
                    return;
                }

                JsonObject memberData = selectedProfile.getAsJsonObject("members").getAsJsonObject(uuid);

                int threshold = floorThresholds.getOrDefault(currentFloor, 9999);
                String floorKey = currentFloor.toLowerCase(); // "f7", "m7", etc.
                if (!floorKey.startsWith("f") && !floorKey.startsWith("m")) {
                    floorKey = "f" + floorKey; // fallback: assume it's a regular floor if no prefix
                }
                
                int pbTime = getFastestTime(memberData, floorKey);

                if (pbTime > threshold) {
                	mc.thePlayer.sendChatMessage("�cKicked �e" + player + " �c(PB " + pbTime + "s > " + threshold + "s)");
                    mc.thePlayer.sendChatMessage("/party kick " + player);
                } else {
                    mc.thePlayer.sendChatMessage("�aAccepted �e" + player + " �7(PB: " + pbTime + "s | REQ: " + threshold + "s)");
                }

            } catch (Exception e) {
                send("�c[AutoKick] Error processing �e" + player + "�c. See logs.");
                e.printStackTrace();
            }
        }).start();
    }

    private int getFastestTime(JsonObject memberData, String floorKey) {
        try {
            JsonObject dungeons = memberData.getAsJsonObject("dungeons");
            if (dungeons == null) return 9999;

            JsonObject dungeonTypes = dungeons.getAsJsonObject("dungeon_types");
            if (dungeonTypes == null) return 9999;

            String dungeonType = floorKey.toLowerCase().startsWith("m") ? "master_catacombs" : "catacombs";
            JsonObject dungeon = dungeonTypes.getAsJsonObject(dungeonType);
            if (dungeon == null) {
                send("�c[AutoKick] No data for dungeon type: " + dungeonType);
                return 9999;
            }

            JsonObject fastestTimes = dungeon.getAsJsonObject("fastest_time");
            if (fastestTimes == null) {
                send("�c[AutoKick] No fastest_time section in " + dungeonType);
                return 9999;
            }

            // Strip the 'f' or 'm' prefix to get the floor number
            String floorId = floorKey.substring(1);

            if (fastestTimes.has(floorId)) {
                int ms = fastestTimes.get(floorId).getAsInt();
                int seconds = ms / 1000;
                return seconds;
            } else {
                send("�c[AutoKick] No PB found for floor " + floorId + " in " + dungeonType);
            }

        } catch (Exception e) {
            send("�c[AutoKick] Exception during PB extraction.");
            e.printStackTrace();
        }

        return 9999;
    }

    private String readUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Increase timeouts here
        conn.setConnectTimeout(5000); // 5 seconds to connect
        conn.setReadTimeout(10000);   // 10 seconds to wait for data

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return result.toString();
    }

    private void send(String msg) {
        mc.thePlayer.addChatMessage(new ChatComponentText("�7[�bAutoKick�7] " + msg));
    }
}
