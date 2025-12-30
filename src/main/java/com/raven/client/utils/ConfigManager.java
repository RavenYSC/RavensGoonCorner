package com.raven.client.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static final File CONFIG_FILE = new File("config/RavenYSC Configs/settings.json");
    private static final Gson gson = new Gson();
    private static Map<String, Object> settings = new HashMap<>();

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // create default file if not present
            return;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            settings = gson.fromJson(reader, type);
            if (settings == null) settings = new HashMap<>();
            System.out.println("[ConfigManager] Settings loaded.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(settings, writer);
            System.out.println("[ConfigManager] Settings saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void set(String key, Object value) {
        settings.put(key, value);
        // Don't save on every set() - callers should decide when to save
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defaultValue) {
        if (settings.containsKey(key)) {
            try {
                return (T) settings.get(key);
            } catch (ClassCastException e) {
                System.err.println("[ConfigManager] Type mismatch for key: " + key);
            }
        }
        return defaultValue;
    }
}
