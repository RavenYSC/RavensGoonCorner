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
            return;
        }
        
        // Get or create jitter buffer for this user
        JitterBuffer buffer = userBuffers.computeIfAbsent(userId, k -> new JitterBuffer());
        
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
        
        while (playing.get()) {
            try {
                // Clear mix buffer
                for (int i = 0; i < mixBuffer.length; i++) {
                    mixBuffer[i] = 0;
                }
                
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
                    speaker.write(mixBuffer, 0, mixBuffer.length);
                } else {
                    // Sleep briefly if no audio to prevent busy waiting
                    Thread.sleep(10);
                }
                
            } catch (Exception e) {
                if (playing.get()) {
                    System.err.println("[VoiceChat] Playback error: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Mix source audio into destination buffer
     */
    private void mixAudio(byte[] dest, byte[] source) {
        for (int i = 0; i < dest.length && i < source.length; i += 2) {
            // Convert to 16-bit samples
            int sample1 = (dest[i] & 0xFF) | (dest[i + 1] << 8);
            int sample2 = (source[i] & 0xFF) | (source[i + 1] << 8);
            
            // Mix samples
            int mixed = sample1 + sample2;
            
            // Soft clipping to prevent harsh distortion
            if (mixed > 32767) {
                mixed = 32767 - (mixed - 32767) / 4;
                mixed = Math.min(32767, mixed);
            } else if (mixed < -32768) {
                mixed = -32768 - (mixed + 32768) / 4;
                mixed = Math.max(-32768, mixed);
            }
            
            // Convert back to bytes
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
            int sample = (buffer[i] & 0xFF) | (buffer[i + 1] << 8);
            sample = (int) (sample * volume);
            sample = Math.max(-32768, Math.min(32767, sample));
            result[i] = (byte) (sample & 0xFF);
            result[i + 1] = (byte) ((sample >> 8) & 0xFF);
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
