package com.raven.client.music;

import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDeviceBase;

import javax.sound.sampled.*;

public class VolumeAudioDevice extends AudioDeviceBase {

    private SourceDataLine source;
    private FloatControl volumeControl;
    private float volumePercent = 1.0f;
    private AudioFormat fmt;

    @Override
    public boolean isOpen() {
        return source != null;
    }

    @Override
    protected void openImpl() throws JavaLayerException {
        // Audio format will be set in createSource
    }

    @Override
    protected void writeImpl(short[] samples, int offs, int len) throws JavaLayerException {
        if (source == null) {
            // Create source on first write when we know the decoder's audio format
            createSource();
        }
        if (source == null) return;
        
        // Convert shorts to bytes for the audio line
        byte[] buffer = new byte[len * 2];
        int idx = 0;
        for (int i = offs; i < offs + len; i++) {
            short sample = samples[i];
            // Apply software volume if hardware control not available
            if (volumeControl == null) {
                sample = (short)(sample * volumePercent);
            }
            // Little-endian format
            buffer[idx++] = (byte)(sample & 0xFF);
            buffer[idx++] = (byte)((sample >> 8) & 0xFF);
        }
        
        source.write(buffer, 0, buffer.length);
    }

    private void createSource() throws JavaLayerException {
        // Get the decoder to get the audio format
        Decoder decoder = getDecoder();
        if (decoder == null) return;
        
        // Create audio format: 16-bit signed PCM, little-endian
        int sampleRate = decoder.getOutputFrequency();
        int channels = decoder.getOutputChannels();
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            channels,
            channels * 2,
            sampleRate,
            false  // little-endian
        );
        
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            source = (SourceDataLine) AudioSystem.getLine(info);
            source.open(format);
            source.start();

            // Grab volume control if available
            if (source.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
                applyVolume();
            }
        } catch (LineUnavailableException e) {
            throw new JavaLayerException("Cannot open audio line", e);
        }
    }

    @Override
    protected void closeImpl() {
        if (source != null) {
            source.drain();
            source.stop();
            source.close();
            source = null;
        }
    }

    @Override
    protected void flushImpl() {
        if (source != null) source.flush();
    }

    @Override
    public int getPosition() {
        return source != null ? (int)(source.getMicrosecondPosition() / 1000) : 0;
    }

    public void setVolume(float volumePercent) {
        this.volumePercent = Math.max(0.0f, Math.min(1.0f, volumePercent));
        applyVolume();
    }
    
    private void applyVolume() {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            // Use logarithmic scale for more natural volume control
            float dB = min + (max - min) * volumePercent;
            volumeControl.setValue(dB);
        }
    }
}