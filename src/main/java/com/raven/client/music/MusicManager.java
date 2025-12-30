package com.raven.client.music;

import com.raven.client.utils.ConfigManager;
import com.raven.client.utils.DirectoryManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicManager {

    private static final List<File> trackList = new ArrayList<>();
    private static int currentTrackIndex = 0;

    private static javazoom.jl.player.advanced.AdvancedPlayer player;
    private static VolumeAudioDevice audioDevice;
    private static float volume = 1.0f; // 100%

    private static Thread playbackThread;
    private static boolean shuffle = false;

    public static void init() {
        loadTracks();

        // Load user config
        Object volumeObj = ConfigManager.get("music.volume", 1.0f);
        if (volumeObj instanceof Number) {
            volume = ((Number) volumeObj).floatValue();
        } else if (volumeObj instanceof Float) {
            volume = (Float) volumeObj;
        } else if (volumeObj instanceof Double) {
            volume = ((Double) volumeObj).floatValue();
        } else {
            volume = 1.0f;
        }
        shuffle = ConfigManager.get("music.shuffle", false);

        System.out.println("[MusicManager] Loaded " + trackList.size() + " tracks.");
        System.out.println("[MusicManager] Volume: " + (int)(volume * 100) + "% | Shuffle: " + shuffle);
    }


    public static void loadTracks() {
        trackList.clear();
        File dir = DirectoryManager.SONGS_DIR;

        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files != null) Collections.addAll(trackList, files);
    }

    public static void playCurrent() {
        stop();
        if (trackList.isEmpty()) return;

        File file = trackList.get(currentTrackIndex);

        playbackThread = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                audioDevice = new VolumeAudioDevice();
                audioDevice.setVolume(volume);
                player = new javazoom.jl.player.advanced.AdvancedPlayer(fis, audioDevice);

                System.out.println("[MusicManager] Now playing: " + file.getName());
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        playbackThread.start();
    }

    public static void stop() {
        try {
            if (player != null) player.close();
            if (playbackThread != null && playbackThread.isAlive()) {
                playbackThread.interrupt();
            }
        } catch (Exception ignored) {}
    }

    public static void next() {
        if (trackList.isEmpty()) return;
        currentTrackIndex = shuffle
                ? (int) (Math.random() * trackList.size())
                : (currentTrackIndex + 1) % trackList.size();
        playCurrent();
    }

    public static void previous() {
        if (trackList.isEmpty()) return;
        currentTrackIndex = (currentTrackIndex - 1 + trackList.size()) % trackList.size();
        playCurrent();
    }

    public static void addTrack(File file) {
        if (!trackList.contains(file)) {
            trackList.add(file);
            System.out.println("[MusicManager] Track added: " + file.getName());
        }
    }

    public static String getCurrentTrackName() {
        return trackList.isEmpty() ? "No track loaded" : trackList.get(currentTrackIndex).getName();
    }

    public static void toggleShuffle() {
        shuffle = !shuffle;
        ConfigManager.set("music.shuffle", shuffle);
        ConfigManager.save();
    }

    public static boolean isShuffle() {
        return shuffle;
    }

    public static boolean isPlaying() {
        return playbackThread != null && playbackThread.isAlive();
    }

    public static List<File> getPlaylist() {
        return new ArrayList<>(trackList);
    }
    
    public static void setVolume(float percent) {
        volume = Math.max(0f, Math.min(1f, percent)); // clamp 0.0–1.0
        ConfigManager.set("music.volume", volume);
        ConfigManager.save();

        if (audioDevice != null) {
            audioDevice.setVolume(volume);
        }
    }

    public static float getVolume() {
        return volume;
    }
}