package com.raven.client.gui.notifications;

public class Notification {

    public final String message;
    public final long startTime;
    public final long duration;

    public Notification(String message, long durationMillis) {
        this.message = message;
        this.startTime = System.currentTimeMillis();
        this.duration = durationMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > duration;
    }
}
