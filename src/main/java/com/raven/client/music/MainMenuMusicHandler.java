package com.raven.client.music;

import com.raven.client.utils.ConfigManager;
import com.raven.client.utils.DirectoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles custom music playback on the main menu/login screen.
 * Plays music from a dedicated "menu" folder or falls back to general music.
 * Includes bundled default tracks that are extracted on first run.
 */
public class MainMenuMusicHandler {
    
    private static MainMenuMusicHandler instance;
    
    // List of bundled default tracks (in resources/assets/ravenclient/music/menu/)
    private static final String[] BUNDLED_TRACKS = {
        "menu_chill_1.mp3",      // Noir - Slowed
        "menu_ambient_1.mp3",    // Telescope
        "menu_classical_1.mp3"   // Moonlight Sonata
    };
    
    private javazoom.jl.player.advanced.AdvancedPlayer player;
    private VolumeAudioDevice audioDevice;
    private Thread playbackThread;
    
    private List<File> menuTracks = new ArrayList<>();
    private int currentTrackIndex = 0;
    private boolean isPlaying = false;
    private boolean wasOnMainMenu = false;
    
    private float volume = 0.5f;
    private boolean enabled = true;
    private boolean shuffle = true;
    private boolean fadeVanillaMusic = true;
    
    private MainMenuMusicHandler() {
        loadSettings();
        extractBundledMusic();
        loadMenuTracks();
    }
    
    /**
     * Extract bundled music from mod resources to the menu folder on first run.
     * Only extracts if the menu folder is empty (so user's custom music isn't overwritten).
     */
    private void extractBundledMusic() {
        File menuDir = new File(DirectoryManager.SONGS_DIR, "menu");
        if (!menuDir.exists()) {
            menuDir.mkdirs();
        }
        
        // Check if menu folder already has tracks
        File[] existingFiles = menuDir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".mp3") ||
            name.toLowerCase().endsWith(".wav") ||
            name.toLowerCase().endsWith(".ogg")
        );
        
        if (existingFiles != null && existingFiles.length > 0) {
            System.out.println("[MainMenuMusic] Found " + existingFiles.length + " existing tracks, skipping extraction");
            return;
        }
        
        // Extract bundled tracks
        System.out.println("[MainMenuMusic] Extracting bundled music...");
        int extracted = 0;
        
        for (String trackName : BUNDLED_TRACKS) {
            try {
                String resourcePath = "/assets/ravenclient/music/menu/" + trackName;
                InputStream in = getClass().getResourceAsStream(resourcePath);
                
                if (in == null) {
                    System.out.println("[MainMenuMusic] Bundled track not found: " + trackName);
                    continue;
                }
                
                File outFile = new File(menuDir, trackName);
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                in.close();
                
                extracted++;
                System.out.println("[MainMenuMusic] Extracted: " + trackName);
                
            } catch (Exception e) {
                System.err.println("[MainMenuMusic] Failed to extract " + trackName + ": " + e.getMessage());
            }
        }
        
        System.out.println("[MainMenuMusic] Extracted " + extracted + " bundled tracks");
    }
    
    public static MainMenuMusicHandler getInstance() {
        if (instance == null) {
            instance = new MainMenuMusicHandler();
        }
        return instance;
    }
    
    private void loadSettings() {
        Object volObj = ConfigManager.get("menuMusic.volume", 0.5f);
        if (volObj instanceof Number) {
            volume = ((Number) volObj).floatValue();
        }
        
        enabled = ConfigManager.get("menuMusic.enabled", true);
        shuffle = ConfigManager.get("menuMusic.shuffle", true);
        fadeVanillaMusic = ConfigManager.get("menuMusic.fadeVanilla", true);
    }
    
    private void saveSettings() {
        ConfigManager.set("menuMusic.volume", volume);
        ConfigManager.set("menuMusic.enabled", enabled);
        ConfigManager.set("menuMusic.shuffle", shuffle);
        ConfigManager.set("menuMusic.fadeVanilla", fadeVanillaMusic);
        ConfigManager.save();
    }
    
    /**
     * Load tracks from the menu music folder
     */
    public void loadMenuTracks() {
        menuTracks.clear();
        
        // First check for dedicated menu folder
        File menuDir = new File(DirectoryManager.SONGS_DIR, "menu");
        if (!menuDir.exists()) {
            menuDir.mkdirs();
            System.out.println("[MainMenuMusic] Created menu music folder: " + menuDir.getAbsolutePath());
        }
        
        File[] files = menuDir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".mp3") ||
            name.toLowerCase().endsWith(".wav") ||
            name.toLowerCase().endsWith(".ogg")
        );
        
        if (files != null && files.length > 0) {
            Collections.addAll(menuTracks, files);
        }
        
        // If no dedicated menu tracks, use general music library
        if (menuTracks.isEmpty()) {
            File[] generalFiles = DirectoryManager.SONGS_DIR.listFiles((d, name) -> 
                name.toLowerCase().endsWith(".mp3") && !new File(d, name).isDirectory()
            );
            if (generalFiles != null) {
                Collections.addAll(menuTracks, generalFiles);
            }
        }
        
        if (shuffle && !menuTracks.isEmpty()) {
            Collections.shuffle(menuTracks);
        }
        
        System.out.println("[MainMenuMusic] Loaded " + menuTracks.size() + " menu tracks");
    }
    
    /**
     * Start playing menu music
     */
    public void play() {
        if (!enabled || menuTracks.isEmpty()) return;
        
        stop();
        isPlaying = true;
        
        // Fade out vanilla Minecraft music
        if (fadeVanillaMusic) {
            try {
                Minecraft.getMinecraft().getSoundHandler().stopSounds();
            } catch (Exception ignored) {}
        }
        
        playCurrentTrack();
    }
    
    private void playCurrentTrack() {
        if (menuTracks.isEmpty() || !isPlaying) return;
        
        File track = menuTracks.get(currentTrackIndex);
        
        playbackThread = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(track)) {
                audioDevice = new VolumeAudioDevice();
                audioDevice.setVolume(volume);
                player = new javazoom.jl.player.advanced.AdvancedPlayer(fis, audioDevice);
                
                System.out.println("[MainMenuMusic] Playing: " + track.getName());
                player.play();
                
                // Track finished, play next if still on menu
                if (isPlaying) {
                    nextTrack();
                }
                
            } catch (Exception e) {
                if (isPlaying) {
                    System.err.println("[MainMenuMusic] Playback error: " + e.getMessage());
                    nextTrack();
                }
            }
        });
        playbackThread.setDaemon(true);
        playbackThread.start();
    }
    
    /**
     * Stop playback
     */
    public void stop() {
        isPlaying = false;
        
        try {
            if (player != null) {
                player.close();
                player = null;
            }
            if (playbackThread != null && playbackThread.isAlive()) {
                playbackThread.interrupt();
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Play next track
     */
    public void nextTrack() {
        if (menuTracks.isEmpty()) return;
        
        try {
            if (player != null) player.close();
        } catch (Exception ignored) {}
        
        currentTrackIndex = shuffle 
            ? (int)(Math.random() * menuTracks.size())
            : (currentTrackIndex + 1) % menuTracks.size();
        
        if (isPlaying) {
            playCurrentTrack();
        }
    }
    
    /**
     * Play previous track
     */
    public void previousTrack() {
        if (menuTracks.isEmpty()) return;
        
        try {
            if (player != null) player.close();
        } catch (Exception ignored) {}
        
        currentTrackIndex = (currentTrackIndex - 1 + menuTracks.size()) % menuTracks.size();
        
        if (isPlaying) {
            playCurrentTrack();
        }
    }
    
    /**
     * Called when a GUI opens - check if it's the main menu
     */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        boolean isMainMenu = event.gui instanceof GuiMainMenu;
        
        if (isMainMenu && !wasOnMainMenu && enabled) {
            // Entering main menu - start music
            play();
        } else if (!isMainMenu && wasOnMainMenu) {
            // Leaving main menu - stop music
            stop();
        }
        
        wasOnMainMenu = isMainMenu;
    }
    
    /**
     * Tick handler to keep music playing and handle vanilla music
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        // Keep vanilla music muted while on main menu with our music
        if (isPlaying && fadeVanillaMusic && mc.currentScreen instanceof GuiMainMenu) {
            try {
                // Stop any vanilla music that might try to play
                mc.getSoundHandler().stopSounds();
            } catch (Exception ignored) {}
        }
    }
    
    // Getters and setters
    
    public String getCurrentTrackName() {
        if (menuTracks.isEmpty()) return "No Track";
        return menuTracks.get(currentTrackIndex).getName().replaceAll("\\.mp3$|\\.wav$|\\.ogg$", "");
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stop();
        }
        saveSettings();
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        if (audioDevice != null) {
            audioDevice.setVolume(this.volume);
        }
        saveSettings();
    }
    
    public boolean isShuffle() {
        return shuffle;
    }
    
    public void toggleShuffle() {
        shuffle = !shuffle;
        if (shuffle && !menuTracks.isEmpty()) {
            Collections.shuffle(menuTracks);
        }
        saveSettings();
    }
    
    public boolean isFadeVanillaMusic() {
        return fadeVanillaMusic;
    }
    
    public void setFadeVanillaMusic(boolean fade) {
        this.fadeVanillaMusic = fade;
        saveSettings();
    }
    
    public List<File> getMenuTracks() {
        return new ArrayList<>(menuTracks);
    }
    
    public void reloadTracks() {
        loadMenuTracks();
    }
}
