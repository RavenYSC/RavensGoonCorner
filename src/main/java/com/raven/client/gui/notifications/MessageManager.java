package com.raven.client.gui.notifications;

import java.util.*;

public class MessageManager {
    private static final List<Message> messages = new ArrayList<>();
    private static final Set<String> readMessages = new HashSet<>();
    
    public static void addMessage(Message msg) {
        messages.add(0, msg); // Add to front (newest first)
        // Keep only last 50 messages
        while (messages.size() > 50) {
            messages.remove(messages.size() - 1);
        }
    }
    
    public static List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public static List<Message> getMessagesByType(Message.MessageType type) {
        List<Message> filtered = new ArrayList<>();
        for (Message m : messages) {
            if (m.type == type) {
                filtered.add(m);
            }
        }
        return filtered;
    }
    
    public static void markAsRead(String messageId) {
        readMessages.add(messageId);
        for (Message m : messages) {
            if (m.id.equals(messageId)) {
                m.read = true;
            }
        }
    }
    
    public static int getUnreadCount() {
        int count = 0;
        for (Message m : messages) {
            if (!m.read) count++;
        }
        return count;
    }
    
    public static int getUnreadCountByType(Message.MessageType type) {
        int count = 0;
        for (Message m : messages) {
            if (m.type == type && !m.read) count++;
        }
        return count;
    }
    
    public static void clear() {
        messages.clear();
        readMessages.clear();
    }
}
