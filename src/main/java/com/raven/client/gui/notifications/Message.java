package com.raven.client.gui.notifications;

public class Message {
    public enum MessageType {
        EVENT("EVENT", 0xFF6B6B),           // Red
        NEWS("NEWS", 0x4ECDC4),             // Teal
        INBOX("INBOX", 0x95E1D3),           // Light Green
        PARTY_FINDER("PARTY FINDER", 0xFFD700), // Gold
        BAZAAR("BAZAAR", 0x55FF55),         // Green
        AUCTION_HOUSE("AUCTION", 0xAA55FF), // Purple
        STOCK_MARKET("STOCKS", 0x55FFFF),   // Cyan
        VOICE_CHAT("VOICE", 0x5865F2);      // Discord blue
        
        public final String label;
        public final int color;
        
        MessageType(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }
    
    public final String id;
    public final String title;
    public final String content;
    public final MessageType type;
    public final long timestamp;
    public boolean read;
    
    public Message(String id, String title, String content, MessageType type) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }
    
    public String getPreview() {
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }
}
