package com.raven.client.music;

import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDeviceBase;

import javax.sound.sampled.*;

public class VolumeAudioDevice extends AudioDeviceBase {

    private SourceDataLine source;
    private FloatControl volumeControl;

    protected void setAudioFormat(AudioFormat fmt) throws JavaLayerException {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            source = (SourceDataLine) AudioSystem.getLine(info);
            source.open(fmt);
            source.start();

            // Grab volume control if available
            if (source.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
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
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float volume = min + (max - min) * volumePercent;
            volumeControl.setValue(volume);
        }
    }
}