package com.raven.client.music;

import com.raven.client.utils.ConfigManager;
import com.raven.client.utils.DirectoryManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Radio system with multiple channels for the mod.
 * Supports both streaming URLs and local file-based channels.
 */
public class RadioManager {
    
    private static RadioManager instance;
    
    // Radio channels - each channel has a name and either a stream URL or local folder
    private final List<RadioChannel> channels = new ArrayList<>();
    private int currentChannelIndex = 0;
    private boolean isPlaying = false;
    
    // Audio playback
    private javazoom.jl.player.advanced.AdvancedPlayer player;
    private VolumeAudioDevice audioDevice;
    private Thread playbackThread;
    private Thread streamThread;
    
    private float volume = 0.7f;
    private boolean enabled = true;
    
    // Current track info for local channels
    private List<File> currentChannelTracks = new ArrayList<>();
    private int currentTrackIndex = 0;
    private boolean shuffle = true;
    
    // Stream buffer for internet radio
    private InputStream currentStream;
    
    private RadioManager() {
        initializeChannels();
        loadSettings();
    }
    
    public static RadioManager getInstance() {
        if (instance == null) {
            instance = new RadioManager();
        }
        return instance;
    }
    
    private void initializeChannels() {
        // Add default radio channels
        // Local channels use the MusicLibrary folder
        channels.add(new RadioChannel("My Music", "local", ""));
        channels.add(new RadioChannel("Chill Vibes", "local", "chill"));
        channels.add(new RadioChannel("Gaming Beats", "local", "gaming"));
        channels.add(new RadioChannel("Lo-Fi Radio", "stream", "https://streams.ilovemusic.de/iloveradio17.mp3"));
        channels.add(new RadioChannel("Chillhop", "stream", "http://stream.chillhop.com/listen/chillhop/high.mp3"));
        channels.add(new RadioChannel("Synthwave FM", "stream", "https://stream.synthwave-radio.space/listen/synthwave_radio/radio.mp3"));
        channels.add(new RadioChannel("NightRide FM", "stream", "https://stream.nightride.fm/nightride.m4a"));
        channels.add(new RadioChannel("Plaza One", "stream", "https://radio.plaza.one/mp3"));
        
        System.out.println("[RadioManager] Initialized with " + channels.size() + " channels");
    }
    
    private void loadSettings() {
        Object volObj = ConfigManager.get("radio.volume", 0.7f);
        if (volObj instanceof Number) {
            volume = ((Number) volObj).floatValue();
        }
        
        enabled = ConfigManager.get("radio.enabled", true);
        
        // JSON stores integers as doubles, so we need to handle Number conversion
        Object channelObj = ConfigManager.get("radio.lastChannel", 0);
        if (channelObj instanceof Number) {
            currentChannelIndex = ((Number) channelObj).intValue();
        }
        
        shuffle = ConfigManager.get("radio.shuffle", true);
        
        // Clamp channel index
        if (currentChannelIndex < 0 || currentChannelIndex >= channels.size()) {
            currentChannelIndex = 0;
        }
    }
    
    private void saveSettings() {
        ConfigManager.set("radio.volume", volume);
        ConfigManager.set("radio.enabled", enabled);
        ConfigManager.set("radio.lastChannel", currentChannelIndex);
        ConfigManager.set("radio.shuffle", shuffle);
        ConfigManager.save();
    }
    
    /**
     * Start playing the current channel
     */
    public void play() {
        if (!enabled) return;
        
        stop();
        isPlaying = true;
        
        RadioChannel channel = getCurrentChannel();
        if (channel == null) return;
        
        if (channel.type.equals("stream")) {
            playStream(channel.source);
        } else {
            playLocalChannel(channel);
        }
    }
    
    /**
     * Play an internet radio stream
     */
    private void playStream(String streamUrl) {
        streamThread = new Thread(() -> {
            try {
                System.out.println("[RadioManager] Connecting to stream: " + streamUrl);
                
                URL url = new URL(streamUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                
                currentStream = connection.getInputStream();
                
                audioDevice = new VolumeAudioDevice();
                audioDevice.setVolume(volume);
                player = new javazoom.jl.player.advanced.AdvancedPlayer(currentStream, audioDevice);
                
                System.out.println("[RadioManager] Stream connected, playing...");
                player.play();
                
            } catch (Exception e) {
                System.err.println("[RadioManager] Stream error: " + e.getMessage());
                isPlaying = false;
            }
        });
        streamThread.setDaemon(true);
        streamThread.start();
    }
    
    /**
     * Play local files for a channel
     */
    private void playLocalChannel(RadioChannel channel) {
        // Load tracks for this channel
        loadChannelTracks(channel);
        
        if (currentChannelTracks.isEmpty()) {
            System.out.println("[RadioManager] No tracks for channel: " + channel.name);
            isPlaying = false;
            return;
        }
        
        if (shuffle) {
            Collections.shuffle(currentChannelTracks);
        }
        
        currentTrackIndex = 0;
        playCurrentTrack();
    }
    
    private void loadChannelTracks(RadioChannel channel) {
        currentChannelTracks.clear();
        
        File baseDir = DirectoryManager.SONGS_DIR;
        File channelDir;
        
        if (channel.source.isEmpty()) {
            // "My Music" channel - all tracks from base directory
            channelDir = baseDir;
        } else {
            // Specific subfolder for the channel
            channelDir = new File(baseDir, channel.source);
            if (!channelDir.exists()) {
                channelDir.mkdirs();
                System.out.println("[RadioManager] Created channel folder: " + channelDir.getAbsolutePath());
            }
        }
        
        File[] files = channelDir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".mp3") || 
            name.toLowerCase().endsWith(".wav") ||
            name.toLowerCase().endsWith(".ogg")
        );
        
        if (files != null) {
            Collections.addAll(currentChannelTracks, files);
        }
        
        // For "My Music", also check if we should include subfolders
        if (channel.source.isEmpty()) {
            // Include all mp3s from base dir only (not subfolders for cleaner separation)
        }
        
        System.out.println("[RadioManager] Loaded " + currentChannelTracks.size() + " tracks for channel: " + channel.name);
    }
    
    private void playCurrentTrack() {
        if (currentChannelTracks.isEmpty() || !isPlaying) return;
        
        File track = currentChannelTracks.get(currentTrackIndex);
        
        playbackThread = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(track)) {
                audioDevice = new VolumeAudioDevice();
                audioDevice.setVolume(volume);
                player = new javazoom.jl.player.advanced.AdvancedPlayer(fis, audioDevice);
                
                System.out.println("[RadioManager] Now playing: " + track.getName());
                player.play();
                
                // Track finished, play next
                if (isPlaying) {
                    nextTrack();
                }
                
            } catch (Exception e) {
                if (isPlaying) {
                    System.err.println("[RadioManager] Playback error: " + e.getMessage());
                    // Try next track
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
            if (currentStream != null) {
                currentStream.close();
                currentStream = null;
            }
            if (playbackThread != null && playbackThread.isAlive()) {
                playbackThread.interrupt();
            }
            if (streamThread != null && streamThread.isAlive()) {
                streamThread.interrupt();
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Toggle play/pause
     */
    public void toggle() {
        if (isPlaying) {
            stop();
        } else {
            play();
        }
    }
    
    /**
     * Switch to next channel
     */
    public void nextChannel() {
        currentChannelIndex = (currentChannelIndex + 1) % channels.size();
        saveSettings();
        if (isPlaying) {
            play();
        }
    }
    
    /**
     * Switch to previous channel
     */
    public void previousChannel() {
        currentChannelIndex = (currentChannelIndex - 1 + channels.size()) % channels.size();
        saveSettings();
        if (isPlaying) {
            play();
        }
    }
    
    /**
     * Set specific channel by index
     */
    public void setChannel(int index) {
        if (index >= 0 && index < channels.size()) {
            currentChannelIndex = index;
            saveSettings();
            if (isPlaying) {
                play();
            }
        }
    }
    
    /**
     * Skip to next track (for local channels)
     */
    public void nextTrack() {
        RadioChannel channel = getCurrentChannel();
        if (channel != null && channel.type.equals("local") && !currentChannelTracks.isEmpty()) {
            // Stop current playback
            try {
                if (player != null) player.close();
            } catch (Exception ignored) {}
            
            currentTrackIndex = shuffle 
                ? (int)(Math.random() * currentChannelTracks.size())
                : (currentTrackIndex + 1) % currentChannelTracks.size();
            
            if (isPlaying) {
                playCurrentTrack();
            }
        }
    }
    
    /**
     * Skip to previous track (for local channels)
     */
    public void previousTrack() {
        RadioChannel channel = getCurrentChannel();
        if (channel != null && channel.type.equals("local") && !currentChannelTracks.isEmpty()) {
            try {
                if (player != null) player.close();
            } catch (Exception ignored) {}
            
            currentTrackIndex = (currentTrackIndex - 1 + currentChannelTracks.size()) % currentChannelTracks.size();
            
            if (isPlaying) {
                playCurrentTrack();
            }
        }
    }
    
    // Getters and setters
    
    public RadioChannel getCurrentChannel() {
        if (channels.isEmpty()) return null;
        return channels.get(currentChannelIndex);
    }
    
    public String getCurrentChannelName() {
        RadioChannel ch = getCurrentChannel();
        return ch != null ? ch.name : "No Channel";
    }
    
    public String getCurrentTrackName() {
        RadioChannel channel = getCurrentChannel();
        if (channel == null) return "";
        
        if (channel.type.equals("stream")) {
            return "Internet Radio";
        } else if (!currentChannelTracks.isEmpty()) {
            return currentChannelTracks.get(currentTrackIndex).getName().replaceAll("\\.mp3$|\\.wav$|\\.ogg$", "");
        }
        return "No Track";
    }
    
    public int getCurrentChannelIndex() {
        return currentChannelIndex;
    }
    
    public List<RadioChannel> getChannels() {
        return new ArrayList<>(channels);
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
        saveSettings();
    }
    
    /**
     * Add a custom channel
     */
    public void addChannel(String name, String type, String source) {
        channels.add(new RadioChannel(name, type, source));
        System.out.println("[RadioManager] Added channel: " + name);
    }
    
    /**
     * Radio channel data class
     */
    public static class RadioChannel {
        public final String name;
        public final String type; // "stream" or "local"
        public final String source; // URL for stream, folder name for local
        
        public RadioChannel(String name, String type, String source) {
            this.name = name;
            this.type = type;
            this.source = source;
        }
    }
}
