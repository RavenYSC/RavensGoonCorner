package com.raven.client.voicechat.audio;

import com.raven.client.voicechat.VoiceChatManager;

import javax.sound.sampled.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles audio playback for received voice data
 */
public class AudioPlayback {
    
    private final VoiceChatManager manager;
    
    private AudioFormat audioFormat;
    private SourceDataLine speaker;
    private AtomicBoolean initialized = new AtomicBoolean(false);
    
    // Per-user jitter buffers to smooth out network variations
    private Map<String, JitterBuffer> userBuffers = new ConcurrentHashMap<>();
    
    // Playback thread
    private Thread playbackThread;
    private AtomicBoolean playing = new AtomicBoolean(false);
    
    public AudioPlayback(VoiceChatManager manager) {
        this.manager = manager;
    }
    
    /**
     * Initialize audio playback
     */
    public boolean initialize() {
        try {
            audioFormat = new AudioFormat(
                AudioCapture.SAMPLE_RATE,
                AudioCapture.SAMPLE_SIZE_BITS,
                AudioCapture.CHANNELS,
                AudioCapture.SIGNED,
                AudioCapture.BIG_ENDIAN
            );
            
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[VoiceChat] Speaker not supported");
                return false;
            }
            
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(audioFormat, AudioCapture.FRAME_BYTES * 8); // Buffer 8 frames
            speaker.start();
            
            // Start playback thread
            playing.set(true);
            playbackThread = new Thread(this::playbackLoop, "VoiceChat-AudioPlayback");
            playbackThread.setDaemon(true);
            playbackThread.start();
            
            initialized.set(true);
            System.out.println("[VoiceChat] Audio playback initialized");
            return true;
            
        } catch (LineUnavailableException e) {
            System.err.println("[VoiceChat] Speaker unavailable: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[VoiceChat] Failed to initialize audio playback: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Shutdown audio playback
     */
    public void shutdown() {
        playing.set(false);
        
        if (playbackThread != null) {
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            playbackThread = null;
        }
        
        if (speaker != null) {
            speaker.stop();
            speaker.close();
            speaker = null;
        }
        
        userBuffers.clear();
        initialized.set(false);
    }
    
    /**
     * Queue audio data for playback from a specific user
     */
    public void playAudio(String userId, byte[] pcmData, float volume) {
        if (!initialized.get()) {
            System.err.println("[VoiceChat] playAudio called but not initialized!");
            return;
        }
        
        if (pcmData == null || pcmData.length == 0) {
            System.err.println("[VoiceChat] playAudio received null/empty data");
            return;
        }
        
        // Get or create jitter buffer for this user
        JitterBuffer buffer = userBuffers.computeIfAbsent(userId, k -> {
            System.out.println("[VoiceChat] Created jitter buffer for user: " + userId);
            return new JitterBuffer();
        });
        
        // Apply volume and queue
        byte[] adjustedData = applyVolume(pcmData, volume);
        buffer.addFrame(adjustedData);
    }
    
    /**
     * Stop playback for a specific user
     */
    public void stopUser(String userId) {
        userBuffers.remove(userId);
    }
    
    /**
     * Main playback loop - mixes audio from all users
     */
    private void playbackLoop() {
        byte[] mixBuffer = new byte[AudioCapture.FRAME_BYTES];
        long lastDebugTime = 0;
        int framesPlayed = 0;
        
        while (playing.get()) {
            try {
                // Clear mix buffer
                java.util.Arrays.fill(mixBuffer, (byte) 0);
                
                boolean hasAudio = false;
                
                // Mix audio from all users
                for (JitterBuffer buffer : userBuffers.values()) {
                    byte[] frame = buffer.getFrame();
                    if (frame != null) {
                        mixAudio(mixBuffer, frame);
                        hasAudio = true;
                    }
                }
                
                // Write to speaker if we have audio
                if (hasAudio) {
                    // Make sure speaker is running
                    if (!speaker.isRunning()) {
                        speaker.start();
                    }
                    
                    int written = speaker.write(mixBuffer, 0, mixBuffer.length);
                    framesPlayed++;
                    
                    // Debug every 5 seconds
                    long now = System.currentTimeMillis();
                    if (now - lastDebugTime > 5000) {
                        System.out.println("[VoiceChat] Audio playback: " + framesPlayed + " frames, written=" + written + 
                            ", available=" + speaker.available() + ", active=" + speaker.isActive() + 
                            ", running=" + speaker.isRunning());
                        lastDebugTime = now;
                        framesPlayed = 0;
                    }
                } else {
                    // Sleep briefly if no audio to prevent busy waiting
                    Thread.sleep(10);
                }
                
            } catch (Exception e) {
                if (playing.get()) {
                    System.err.println("[VoiceChat] Playback error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Mix source audio into destination buffer
     */
    private void mixAudio(byte[] dest, byte[] source) {
        for (int i = 0; i < dest.length && i < source.length; i += 2) {
            // Convert to 16-bit signed samples (little-endian)
            short sample1 = (short) ((dest[i] & 0xFF) | ((dest[i + 1] & 0xFF) << 8));
            short sample2 = (short) ((source[i] & 0xFF) | ((source[i + 1] & 0xFF) << 8));
            
            // Mix samples (use int to avoid overflow)
            int mixed = sample1 + sample2;
            
            // Clamp to 16-bit range
            if (mixed > 32767) {
                mixed = 32767;
            } else if (mixed < -32768) {
                mixed = -32768;
            }
            
            // Convert back to bytes (little-endian)
            dest[i] = (byte) (mixed & 0xFF);
            dest[i + 1] = (byte) ((mixed >> 8) & 0xFF);
        }
    }
    
    /**
     * Apply volume adjustment to audio buffer
     */
    private byte[] applyVolume(byte[] buffer, float volume) {
        if (volume == 1.0f) {
            return buffer;
        }
        
        byte[] result = new byte[buffer.length];
        
        for (int i = 0; i < buffer.length; i += 2) {
            // Read 16-bit signed sample (little-endian)
            short sample = (short) ((buffer[i] & 0xFF) | ((buffer[i + 1] & 0xFF) << 8));
            // Apply volume
            int adjusted = (int) (sample * volume);
            // Clamp to 16-bit range
            adjusted = Math.max(-32768, Math.min(32767, adjusted));
            // Write back (little-endian)
            result[i] = (byte) (adjusted & 0xFF);
            result[i + 1] = (byte) ((adjusted >> 8) & 0xFF);
        }
        
        return result;
    }
    
    public boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * Simple jitter buffer to smooth out network variations
     */
    private static class JitterBuffer {
        private static final int BUFFER_SIZE = 5; // Number of frames to buffer
        private byte[][] frames = new byte[BUFFER_SIZE][];
        private int writeIndex = 0;
        private int readIndex = 0;
        private int frameCount = 0;
        private long lastFrameTime = 0;
        private static final long FRAME_TIMEOUT = 100; // ms before considering user stopped
        
        public synchronized void addFrame(byte[] frame) {
            frames[writeIndex] = frame;
            writeIndex = (writeIndex + 1) % BUFFER_SIZE;
            
            if (frameCount < BUFFER_SIZE) {
                frameCount++;
            } else {
                // Buffer full, advance read index
                readIndex = (readIndex + 1) % BUFFER_SIZE;
            }
            
            lastFrameTime = System.currentTimeMillis();
        }
        
        public synchronized byte[] getFrame() {
            // Check for timeout
            if (System.currentTimeMillis() - lastFrameTime > FRAME_TIMEOUT) {
                frameCount = 0;
                return null;
            }
            
            // Wait for buffer to fill before starting playback
            if (frameCount < 2) {
                return null;
            }
            
            if (frameCount > 0) {
                byte[] frame = frames[readIndex];
                frames[readIndex] = null;
                readIndex = (readIndex + 1) % BUFFER_SIZE;
                frameCount--;
                return frame;
            }
            
            return null;
        }
    }
}
