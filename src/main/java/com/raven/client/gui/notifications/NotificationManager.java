package com.raven.client.gui.notifications;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class NotificationManager {

    private static final List<Notification> activeNotifications = new LinkedList<>();

    public static void show(String message, long durationMillis) {
        activeNotifications.add(new Notification(message, durationMillis));
    }

    public static List<Notification> getActive() {
        // Remove expired ones
        Iterator<Notification> it = activeNotifications.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired()) {
                it.remove();
            }
        }
        return activeNotifications;
    }
}
