package com.raven.client.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DirectoryManager {

    private static final File BASE_DIR = new File("config/RavenYSC Configs/MusicLibrary");
    public static final File PLAYLISTS_DIR = new File(BASE_DIR, "playlists");
    public static final File SONGS_DIR = BASE_DIR;
    public static final File VERSION_FILE = new File(PLAYLISTS_DIR, "playlists_version.txt");
    public static final File USER_PLAYLISTS_FILE = new File(PLAYLISTS_DIR, "user_playlists.json");

    public static void ensureDirectoriesExist() {
        createDir(BASE_DIR);
        createDir(PLAYLISTS_DIR);
    }

    private static void createDir(File dir) {
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("[DirectoryManager] Created: " + dir.getAbsolutePath());
            } else {
                System.err.println("[DirectoryManager] Failed to create: " + dir.getAbsolutePath());
            }
        }
    }

    public static String readVersion() {
        if (!VERSION_FILE.exists()) return "";
        try {
            return new String(Files.readAllBytes(VERSION_FILE.toPath())).trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void saveVersion(String version) {
        try {
            Files.write(VERSION_FILE.toPath(), version.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
