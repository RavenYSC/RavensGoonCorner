package com.raven.client.gui.partyfinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PartyFinderAPI {
    
    // API endpoint on VPS
    private static final String API_BASE_URL = "http://100.42.184.35:25580/partyfinder";
    
    /**
     * Create a new party listing
     */
    public static void createParty(PartyFinderCategory category, String note, int minLevel, int maxPlayers, 
                                   Map<String, Integer> filters, ResultCallback callback) {
        new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) {
                    callback.onError("Player not found");
                    return;
                }
                
                String playerName = mc.thePlayer.getName();
                String uuid = mc.thePlayer.getUniqueID().toString();
                
                JsonObject payload = new JsonObject();
                payload.addProperty("player_name", playerName);
                payload.addProperty("player_uuid", uuid);
                payload.addProperty("category", category.getFullPath());
                payload.addProperty("category_color", category.color);
                payload.addProperty("note", note);
                payload.addProperty("min_level", minLevel);
                payload.addProperty("max_players", maxPlayers);
                
                // Add filters
                if (filters != null && !filters.isEmpty()) {
                    JsonObject filtersObj = new JsonObject();
                    for (Map.Entry<String, Integer> entry : filters.entrySet()) {
                        filtersObj.addProperty(entry.getKey(), entry.getValue());
                    }
                    payload.add("filters", filtersObj);
                }
                
                HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE_URL + "/create").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    JsonObject response = new JsonParser().parse(
                        new InputStreamReader(conn.getInputStream())
                    ).getAsJsonObject();
                    
                    String partyId = response.has("party_id") ? response.get("party_id").getAsString() : "unknown";
                    System.out.println("[PartyFinder] Party created: " + partyId);
                    callback.onSuccess(partyId);
                } else {
                    String error = readErrorResponse(conn);
                    callback.onError(error);
                }
                
            } catch (Exception e) {
                System.err.println("[PartyFinder] Create error: " + e.getMessage());
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Create party with default filters (overload for backwards compatibility)
     */
    public static void createParty(PartyFinderCategory category, String note, int minLevel, int maxPlayers, ResultCallback callback) {
        createParty(category, note, minLevel, maxPlayers, null, callback);
    }
    
    /**
     * Get list of parties for a category
     */
    public static void getParties(PartyFinderCategory category, PartiesCallback callback) {
        new Thread(() -> {
            try {
                String categoryPath = category != null ? category.getFullPath() : "all";
                
                HttpURLConnection conn = (HttpURLConnection) new URL(
                    API_BASE_URL + "/list?category=" + java.net.URLEncoder.encode(categoryPath, "UTF-8")
                ).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    JsonObject response = new JsonParser().parse(
                        new InputStreamReader(conn.getInputStream())
                    ).getAsJsonObject();
                    
                    callback.onSuccess(response);
                } else {
                    String error = readErrorResponse(conn);
                    callback.onError(error);
                }
                
            } catch (Exception e) {
                System.err.println("[PartyFinder] List error: " + e.getMessage());
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Join an existing party
     */
    public static void joinParty(String partyId, ResultCallback callback) {
        new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) {
                    callback.onError("Player not found");
                    return;
                }
                
                String playerName = mc.thePlayer.getName();
                String uuid = mc.thePlayer.getUniqueID().toString();
                
                JsonObject payload = new JsonObject();
                payload.addProperty("party_id", partyId);
                payload.addProperty("player_name", playerName);
                payload.addProperty("player_uuid", uuid);
                
                HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE_URL + "/join").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    JsonObject response = new JsonParser().parse(
                        new InputStreamReader(conn.getInputStream())
                    ).getAsJsonObject();
                    
                    String message = response.has("message") ? response.get("message").getAsString() : "Joined party";
                    System.out.println("[PartyFinder] " + message);
                    callback.onSuccess(partyId);
                } else {
                    String error = readErrorResponse(conn);
                    callback.onError(error);
                }
                
            } catch (Exception e) {
                System.err.println("[PartyFinder] Join error: " + e.getMessage());
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Leave current party
     */
    public static void leaveParty(ResultCallback callback) {
        new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) {
                    callback.onError("Player not found");
                    return;
                }
                
                String uuid = mc.thePlayer.getUniqueID().toString();
                
                JsonObject payload = new JsonObject();
                payload.addProperty("player_uuid", uuid);
                
                HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE_URL + "/leave").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    System.out.println("[PartyFinder] Left party");
                    callback.onSuccess("left");
                } else {
                    String error = readErrorResponse(conn);
                    callback.onError(error);
                }
                
            } catch (Exception e) {
                System.err.println("[PartyFinder] Leave error: " + e.getMessage());
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Get current player's party
     */
    public static void getMyParty(PartyCallback callback) {
        new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) {
                    callback.onError("Player not found");
                    return;
                }
                
                String uuid = mc.thePlayer.getUniqueID().toString();
                
                HttpURLConnection conn = (HttpURLConnection) new URL(
                    API_BASE_URL + "/myparty?player_uuid=" + java.net.URLEncoder.encode(uuid, "UTF-8")
                ).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    JsonObject response = new JsonParser().parse(
                        new InputStreamReader(conn.getInputStream())
                    ).getAsJsonObject();
                    
                    if (response.has("party") && !response.get("party").isJsonNull()) {
                        callback.onSuccess(response.getAsJsonObject("party"));
                    } else {
                        callback.onSuccess(null);
                    }
                } else {
                    String error = readErrorResponse(conn);
                    callback.onError(error);
                }
                
            } catch (Exception e) {
                System.err.println("[PartyFinder] MyParty error: " + e.getMessage());
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Disband party (leader only)
     */
    public static void disbandParty(String partyId, ResultCallback callback) {
        new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) {
                    callback.onError("Player not found");
                    return;
                }
                
                String uuid = mc.thePlayer.getUniqueID().toString();
                
                JsonObject payload = new JsonObject();
                payload.addProperty("player_uuid", uuid);
                
                HttpURLConnection conn = (HttpURLConnection) new URL(
                    API_BASE_URL + "/party/" + partyId
                ).openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    System.out.println("[PartyFinder] Party disbanded");
                    callback.onSuccess("disbanded");
                } else {
                    String error = readErrorResponse(conn);
                    callback.onError(error);
                }
                
            } catch (Exception e) {
                System.err.println("[PartyFinder] Disband error: " + e.getMessage());
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Helper to read error response
     */
    private static String readErrorResponse(HttpURLConnection conn) {
        try {
            JsonObject error = new JsonParser().parse(
                new InputStreamReader(conn.getErrorStream())
            ).getAsJsonObject();
            return error.has("error") ? error.get("error").getAsString() : "Unknown error";
        } catch (Exception e) {
            try {
                return "Error " + conn.getResponseCode();
            } catch (Exception e2) {
                return "Unknown error";
            }
        }
    }
    
    // Callbacks
    public interface ResultCallback {
        void onSuccess(String result);
        void onError(String error);
    }
    
    public interface PartiesCallback {
        void onSuccess(JsonObject response);
        void onError(String error);
    }
    
    public interface PartyCallback {
        void onSuccess(JsonObject party); // party can be null if not in a party
        void onError(String error);
    }
}
