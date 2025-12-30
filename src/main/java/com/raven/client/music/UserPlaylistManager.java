package com.raven.client.music;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.raven.client.utils.DirectoryManager;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserPlaylistManager {

    public static List<UserPlaylist> playlists = new ArrayList<>();
    private static final File SAVE_FILE = DirectoryManager.USER_PLAYLISTS_FILE;

    public static void add(UserPlaylist p) {
        for (UserPlaylist existing : playlists) {
            if (existing.id.equals(p.id)) return; // Prevent duplicate
        }
        playlists.add(p);
        save();
    }

    public static void remove(String id) {
        playlists.removeIf(p -> p.id.equals(id));
        save();
    }

    public static void save() {
        try {
            Gson gson = new Gson();
            FileWriter writer = new FileWriter(SAVE_FILE);
            gson.toJson(playlists, writer);
            writer.close();
            System.out.println("[UserPlaylistManager] Playlists saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!SAVE_FILE.exists()) return;

        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(SAVE_FILE);
            Type listType = new TypeToken<ArrayList<UserPlaylist>>(){}.getType();
            playlists = gson.fromJson(reader, listType);
            reader.close();
            System.out.println("[UserPlaylistManager] Playlists loaded: " + playlists.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
