package com.raven.client.features.Stockholder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raven.client.features.Feature;
import com.raven.client.features.FeatureCategory;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BazaarDataManager extends Feature {

    private static Minecraft mc;
    private static final String API_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private boolean fetchedThisMinute = false;
    
    private static Minecraft getMc() {
        if (mc == null) mc = Minecraft.getMinecraft();
        return mc;
    }
    
    private static final File snapshotFile = new File(getMc().mcDataDir, "bazaar_snapshots.txt");
    private static final java.text.SimpleDateFormat timestampFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static final Map<String, BazaarItem> watchedItems = new HashMap<>();
    private static final Set<String> trackedItemIds = new HashSet<>();
    private static final File configFile = new File(getMc().mcDataDir, "stockholder_items.txt");

    public BazaarDataManager() {
        super("Bazaar Data Updater", FeatureCategory.STOCKHOLDER);
        this.setEnabled(false);
    }

    @Override
    public void onEnable() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        loadTrackedItems();
    }

    @Override
    public void onDisable() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = getMc();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        long currentMillis = System.currentTimeMillis();
        long seconds = (currentMillis / 1000) % 60;

        if (seconds == 5 && !fetchedThisMinute) {
            fetchedThisMinute = true;
            new Thread(this::updateData).start();
        } else if (seconds != 5) {
            fetchedThisMinute = false;
        }
    }

    private void updateData() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            JsonObject root = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
            if (!root.get("success").getAsBoolean()) return;

            JsonObject products = root.getAsJsonObject("products");
            watchedItems.clear();

            for (String id : trackedItemIds) {
                if (!products.has(id)) continue;

                JsonObject obj = products.getAsJsonObject(id);
                JsonObject quickStatus = obj.getAsJsonObject("quick_status");

                double sell = quickStatus.get("sellPrice").getAsDouble();
                double buy = quickStatus.get("buyPrice").getAsDouble();
                double sellVolume = quickStatus.get("sellVolume").getAsDouble();
                double buyVolume = quickStatus.get("buyVolume").getAsDouble();

                BazaarItem item = new BazaarItem(id, buy, sell, buyVolume, sellVolume);
                watchedItems.put(id, item);
                saveSnapshot(id, item);
            }

            System.out.println("[Stockholder] Bazaar data updated at :05. " + watchedItems.size() + " items loaded.");

        } catch (Exception e) {
            System.err.println("[Stockholder] Failed to fetch bazaar data.");
            e.printStackTrace();
        }
    }

    public static BazaarItem getItem(String id) {
        return watchedItems.get(id);
    }

    public static Map<String, BazaarItem> getWatchedItems() {
        return watchedItems;
    }

    public static void watchItem(String id) {
        trackedItemIds.add(id.toUpperCase());
    }

    public static void unwatchItem(String id) {
        trackedItemIds.remove(id.toUpperCase());
    }

    public static void loadTrackedItems() {
        trackedItemIds.clear();
        if (!configFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    trackedItemIds.add(line.trim().toUpperCase());
                }
            }
            System.out.println("[Stockholder] Loaded " + trackedItemIds.size() + " tracked items.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveTrackedItems() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (String id : trackedItemIds) {
                writer.write(id);
                writer.newLine();
            }
            System.out.println("[Stockholder] Saved tracked items.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static Set<String> getTrackedItemIds() {
        return trackedItemIds;
    }
    
    private void saveSnapshot(String itemId, BazaarItem item) {
        String timestamp = timestampFormat.format(new java.util.Date());
        String line = itemId + " | " + timestamp +
                      " | Buy: " + item.buyPrice +
                      " | Sell: " + item.sellPrice +
                      " | BuyVol: " + item.buyVolume +
                      " | SellVol: " + item.sellVolume;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(snapshotFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[Stockholder] Failed to write snapshot for: " + itemId);
            e.printStackTrace();
        }
    }
}
