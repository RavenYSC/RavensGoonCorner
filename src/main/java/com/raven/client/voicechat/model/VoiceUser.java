package com.raven.client.voicechat.model;

/**
 * Represents a user in a voice room
 */
public class VoiceUser {
    
    private final String userId; // Discord user ID or server-assigned ID
    private String displayName;
    private String discordName;
    
    private boolean muted;
    private boolean deafened;
    private boolean talking;
    private float volume;
    
    public VoiceUser(String userId, String displayName, String discordName) {
        this.userId = userId;
        this.displayName = displayName != null ? displayName : "Unknown";
        this.discordName = discordName;
        this.muted = false;
        this.deafened = false;
        this.talking = false;
        this.volume = 1.0f;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getDiscordName() {
        return discordName;
    }
    
    public void setDiscordName(String discordName) {
        this.discordName = discordName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isMuted() {
        return muted;
    }
    
    public void setMuted(boolean muted) {
        this.muted = muted;
    }
    
    public boolean isDeafened() {
        return deafened;
    }
    
    public void setDeafened(boolean deafened) {
        this.deafened = deafened;
    }
    
    public boolean isTalking() {
        return talking;
    }
    
    public void setTalking(boolean talking) {
        this.talking = talking;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void setVolume(float volume) {
        this.volume = volume;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VoiceUser) {
            return userId.equals(((VoiceUser) obj).userId);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
