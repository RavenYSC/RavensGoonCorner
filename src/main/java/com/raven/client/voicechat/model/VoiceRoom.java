package com.raven.client.voicechat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a voice chat room
 */
public class VoiceRoom {
    
    private final String id;
    private String name;
    private String ownerId;
    private boolean isPrivate;
    private String linkedPartyId; // If linked to a Party Finder party
    private int maxUsers;
    private List<UUID> userIds;
    
    public VoiceRoom(String id, String name) {
        this.id = id;
        this.name = name;
        this.isPrivate = false;
        this.linkedPartyId = null;
        this.maxUsers = 10;
        this.userIds = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public boolean isPrivate() {
        return isPrivate;
    }
    
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
    
    public String getLinkedPartyId() {
        return linkedPartyId;
    }
    
    public void setLinkedPartyId(String partyId) {
        this.linkedPartyId = partyId;
    }
    
    public boolean isLinkedToParty() {
        return linkedPartyId != null && !linkedPartyId.isEmpty();
    }
    
    public int getMaxUsers() {
        return maxUsers;
    }
    
    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }
    
    public List<UUID> getUserIds() {
        return userIds;
    }
    
    public int getUserCount() {
        return userIds.size();
    }
    
    public void addUser(UUID userId) {
        if (!userIds.contains(userId)) {
            userIds.add(userId);
        }
    }
    
    public void removeUser(UUID userId) {
        userIds.remove(userId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VoiceRoom) {
            return id.equals(((VoiceRoom) obj).id);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
