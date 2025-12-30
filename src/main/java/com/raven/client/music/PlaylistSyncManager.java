package com.raven.client.music;

import com.google.gson.*;
import com.raven.client.utils.DirectoryManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PlaylistSyncManager {

    public static void syncPlaylistsFromAPI() {
        try {
            URL url = new URL("http://100.42.184.35:8081/playlists");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String json = reader.lines().reduce("", (a, b) -> a + b);
            reader.close();

            JsonObject response = new JsonParser().parse(json).getAsJsonObject();
            String apiVersion = response.get("version").getAsString();
            String currentVersion = DirectoryManager.readVersion();

            if (!apiVersion.equals(currentVersion)) {
                System.out.println("[PlaylistSyncManager] New version detected: " + apiVersion);

                JsonArray playlists = response.get("playlists").getAsJsonArray();
                List<String> ids = new ArrayList<>();

                for (JsonElement el : playlists) {
                    JsonObject obj = el.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    String name = obj.get("name").getAsString();
                    String image = obj.get("image").getAsString();

                    UserPlaylist playlist = new UserPlaylist(id, name, image, new ArrayList<>());
                    UserPlaylistManager.add(playlist);
                    ids.add(id);

                }

                UserPlaylistManager.save();
                DirectoryManager.saveVersion(apiVersion);
            } else {
                System.out.println("[PlaylistSyncManager] Playlist version up-to-date: " + apiVersion);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
