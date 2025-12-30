package com.raven.client.voicechat.audio;

import com.raven.client.voicechat.VoiceChatManager;

import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles microphone audio capture
 */
public class AudioCapture {
    
    private final VoiceChatManager manager;
    
    // Audio format: 48kHz, 16-bit, mono (standard for Opus)
    public static final float SAMPLE_RATE = 48000;
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;
    public static final boolean SIGNED = true;
    public static final boolean BIG_ENDIAN = false;
    
    // Frame size: 20ms of audio at 48kHz = 960 samples
    public static final int FRAME_SIZE = 960;
    public static final int FRAME_BYTES = FRAME_SIZE * 2; // 16-bit = 2 bytes per sample
    
    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private Thread captureThread;
    private AtomicBoolean capturing = new AtomicBoolean(false);
    private AtomicBoolean initialized = new AtomicBoolean(false);
    
    // Voice activation detection
    private float voiceActivityLevel = 0;
    private boolean voiceActive = false;
    private long lastVoiceTime = 0;
    private static final long VOICE_HOLD_TIME = 300; // ms to hold after voice stops
    
    public AudioCapture(VoiceChatManager manager) {
        this.manager = manager;
    }
    
    /**
     * Initialize audio capture
     */
    public boolean initialize() {
        try {
            audioFormat = new AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_BITS,
                CHANNELS,
                SIGNED,
                BIG_ENDIAN
            );
            
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[VoiceChat] Microphone not supported");
                return false;
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(audioFormat, FRAME_BYTES * 4); // Buffer 4 frames
            
            initialized.set(true);
            System.out.println("[VoiceChat] Audio capture initialized");
            return true;
            
        } catch (LineUnavailableException e) {
            System.err.println("[VoiceChat] Microphone unavailable: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[VoiceChat] Failed to initialize audio capture: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Start capturing audio
     */
    public void startCapture() {
        if (!initialized.get() || capturing.get()) {
            return;
        }
        
        capturing.set(true);
        microphone.start();
        
        captureThread = new Thread(this::captureLoop, "VoiceChat-AudioCapture");
        captureThread.setDaemon(true);
        captureThread.start();
        
        System.out.println("[VoiceChat] Audio capture started");
    }
    
    /**
     * Stop capturing audio
     */
    public void stopCapture() {
        capturing.set(false);
        
        if (microphone != null) {
            microphone.stop();
            microphone.flush();
        }
        
        if (captureThread != null) {
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            captureThread = null;
        }
        
        System.out.println("[VoiceChat] Audio capture stopped");
    }
    
    /**
     * Shutdown audio capture
     */
    public void shutdown() {
        stopCapture();
        
        if (microphone != null) {
            microphone.close();
            microphone = null;
        }
        
        initialized.set(false);
    }
    
    /**
     * Main capture loop
     */
    private void captureLoop() {
        byte[] buffer = new byte[FRAME_BYTES];
        long lastDebugTime = 0;
        int framesSent = 0;
        
        while (capturing.get()) {
            try {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    // Apply microphone volume
                    byte[] adjustedBuffer = applyVolume(buffer, manager.getMicrophoneVolume());
                    
                    // Calculate voice activity level
                    voiceActivityLevel = calculateLevel(adjustedBuffer);
                    
                    // Check voice activation (if not using PTT)
                    boolean shouldSend = false;
                    
                    if (manager.isUsePushToTalk()) {
                        // PTT mode - send if key is pressed
                        shouldSend = manager.isPushToTalkActive();
                    } else {
                        // Voice activation mode
                        if (voiceActivityLevel > manager.getMicrophoneVolume() * 0.02f) {
                            voiceActive = true;
                            lastVoiceTime = System.currentTimeMillis();
                        } else if (voiceActive && System.currentTimeMillis() - lastVoiceTime > VOICE_HOLD_TIME) {
                            voiceActive = false;
                        }
                        shouldSend = voiceActive;
                    }
                    
                    if (shouldSend) {
                        manager.onAudioCaptured(adjustedBuffer);
                        framesSent++;
                    }
                    
                    // Debug output every 5 seconds
                    long now = System.currentTimeMillis();
                    if (now - lastDebugTime > 5000) {
                        System.out.println("[VoiceChat] Audio capture stats: level=" + 
                            String.format("%.4f", voiceActivityLevel) + 
                            ", PTT=" + manager.isPushToTalkActive() + 
                            ", usePTT=" + manager.isUsePushToTalk() +
                            ", framesSent=" + framesSent);
                        lastDebugTime = now;
                        framesSent = 0;
                    }
                }
                
            } catch (Exception e) {
                if (capturing.get()) {
                    System.err.println("[VoiceChat] Audio capture error: " + e.getMessage());
                }
            }
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
            // Convert bytes to 16-bit sample (little endian)
            int sample = (buffer[i] & 0xFF) | (buffer[i + 1] << 8);
            
            // Apply volume
            sample = (int) (sample * volume);
            
            // Clamp to prevent clipping
            sample = Math.max(-32768, Math.min(32767, sample));
            
            // Convert back to bytes
            result[i] = (byte) (sample & 0xFF);
            result[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return result;
    }
    
    /**
     * Calculate audio level (RMS)
     */
    private float calculateLevel(byte[] buffer) {
        long sum = 0;
        int count = buffer.length / 2;
        
        for (int i = 0; i < buffer.length; i += 2) {
            int sample = (buffer[i] & 0xFF) | (buffer[i + 1] << 8);
            sum += (long) sample * sample;
        }
        
        float rms = (float) Math.sqrt(sum / count);
        return rms / 32768.0f; // Normalize to 0-1 range
    }
    
    /**
     * Get current voice activity level (0-1)
     */
    public float getVoiceActivityLevel() {
        return voiceActivityLevel;
    }
    
    /**
     * Check if voice is currently active
     */
    public boolean isVoiceActive() {
        return voiceActive || (manager.isUsePushToTalk() && manager.isPushToTalkActive());
    }
    
    /**
     * Check if initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * Check if capturing
     */
    public boolean isCapturing() {
        return capturing.get();
    }
    
    /**
     * Get list of available microphones
     */
    public static Mixer.Info[] getAvailableMicrophones() {
        return AudioSystem.getMixerInfo();
    }
}
