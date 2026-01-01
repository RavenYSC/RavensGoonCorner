package com.raven.client.gui.partyfinder;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the player's actual in-game Hypixel party membership.
 * This is used to verify that players are really in your party before
 * adding them to the server-side party finder listing.
 */
public class HypixelPartyTracker {
    
    private static HypixelPartyTracker instance;
    
    // Current in-game party members (excluding self)
    private Set<String> partyMembers = new HashSet<>();
    
    // Party leader (null if not in a party or if you are leader)
    private String partyLeader = null;
    
    // Whether we're currently in a party
    private boolean inParty = false;
    
    // Whether we're the leader
    private boolean isLeader = false;
    
    // Patterns for Hypixel party chat messages
    // "-----------------------------------------------------"
    // "Party Members (5)"
    // "Party Leader: [MVP+] Username (dot)"
    // "Party Moderators: Player1 (dot) Player2 (dot)"
    // "Party Members: Player3 (dot) Player4 (dot)"
    private static final Pattern PARTY_LEADER_PATTERN = Pattern.compile("Party Leader: (?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+)");
    private static final Pattern PARTY_MODERATORS_PATTERN = Pattern.compile("Party Moderators?: (.+)");
    private static final Pattern PARTY_MEMBERS_PATTERN = Pattern.compile("Party Members?: (.+)");
    
    // Individual join/leave messages
    // "[MVP+] Username joined the party."
    // "[VIP] Username has left the party."
    // "Username has been removed from the party."
    // "The party was disbanded because all invites expired and the party was empty."
    // "You have been kicked from the party by [MVP+] Leader"
    // "You left the party."
    // "You have joined [MVP+] Username's party!"
    // "Username has been kicked from the party"
    // "You are not currently in a party."
    private static final Pattern PLAYER_JOINED_PATTERN = Pattern.compile("(?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+) joined the party\\.");
    private static final Pattern PLAYER_LEFT_PATTERN = Pattern.compile("(?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+) has left the party\\.");
    private static final Pattern PLAYER_KICKED_PATTERN = Pattern.compile("(?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+) has been (?:removed|kicked) from the party");
    private static final Pattern PARTY_DISBANDED_PATTERN = Pattern.compile("(?:The party was disbanded|has disbanded the party)");
    private static final Pattern YOU_LEFT_PATTERN = Pattern.compile("You (?:left the party|have been kicked from the party)");
    private static final Pattern YOU_JOINED_PATTERN = Pattern.compile("You have joined (?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+)'s party!");
    private static final Pattern NOT_IN_PARTY_PATTERN = Pattern.compile("You are not (?:currently )?in a party\\.");
    private static final Pattern PARTY_CREATED_PATTERN = Pattern.compile("Party (?:Leader|created): (?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+)|You have joined (?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+)'s party");
    
    // Transfer pattern: "The party was transferred to [MVP+] NewLeader by [MVP+] OldLeader"
    private static final Pattern PARTY_TRANSFER_PATTERN = Pattern.compile("The party was transferred to (?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+)");
    
    // Promotion pattern: "[MVP+] Username was promoted to Party Leader"
    private static final Pattern PROMOTED_LEADER_PATTERN = Pattern.compile("(?:\\[[^\\]]+\\] )?([a-zA-Z0-9_]+) (?:was promoted to|is now) Party Leader");
    
    private HypixelPartyTracker() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    public static HypixelPartyTracker getInstance() {
        if (instance == null) {
            instance = new HypixelPartyTracker();
        }
        return instance;
    }
    
    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (event.type == 2) return; // Skip action bar messages
        
        String message = event.message.getUnformattedText();
        // Clean color codes
        message = message.replaceAll("§.", "").trim();
        
        processMessage(message);
    }
    
    private void processMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        
        String myName = mc.thePlayer.getName();
        
        // Check for "You are not in a party"
        if (NOT_IN_PARTY_PATTERN.matcher(message).find()) {
            clearParty();
            return;
        }
        
        // Check for party disband
        if (PARTY_DISBANDED_PATTERN.matcher(message).find()) {
            clearParty();
            return;
        }
        
        // Check if you left/kicked from party
        if (YOU_LEFT_PATTERN.matcher(message).find()) {
            clearParty();
            return;
        }
        
        // Check if you joined someone's party
        Matcher youJoinedMatcher = YOU_JOINED_PATTERN.matcher(message);
        if (youJoinedMatcher.find()) {
            clearParty();
            inParty = true;
            partyLeader = youJoinedMatcher.group(1);
            isLeader = false;
            System.out.println("[PartyTracker] Joined " + partyLeader + "'s party");
            return;
        }
        
        // Check for party list header (from /party list or /pl)
        Matcher leaderMatcher = PARTY_LEADER_PATTERN.matcher(message);
        if (leaderMatcher.find()) {
            String leader = leaderMatcher.group(1);
            partyLeader = leader;
            inParty = true;
            isLeader = leader.equalsIgnoreCase(myName);
            
            // The leader won't be in our members set, but they're part of the party
            System.out.println("[PartyTracker] Party leader: " + leader + " (isLeader=" + isLeader + ")");
            return;
        }
        
        // Check for party moderators line
        Matcher modMatcher = PARTY_MODERATORS_PATTERN.matcher(message);
        if (modMatcher.find()) {
            parseAndAddMembers(modMatcher.group(1), myName);
            return;
        }
        
        // Check for party members line
        Matcher membersMatcher = PARTY_MEMBERS_PATTERN.matcher(message);
        if (membersMatcher.find()) {
            parseAndAddMembers(membersMatcher.group(1), myName);
            return;
        }
        
        // Check for player joined
        Matcher joinedMatcher = PLAYER_JOINED_PATTERN.matcher(message);
        if (joinedMatcher.find()) {
            String player = joinedMatcher.group(1);
            if (!player.equalsIgnoreCase(myName)) {
                partyMembers.add(player.toLowerCase());
                inParty = true;
                System.out.println("[PartyTracker] " + player + " joined the party");
            }
            return;
        }
        
        // Check for player left
        Matcher leftMatcher = PLAYER_LEFT_PATTERN.matcher(message);
        if (leftMatcher.find()) {
            String player = leftMatcher.group(1);
            partyMembers.remove(player.toLowerCase());
            System.out.println("[PartyTracker] " + player + " left the party");
            return;
        }
        
        // Check for player kicked
        Matcher kickedMatcher = PLAYER_KICKED_PATTERN.matcher(message);
        if (kickedMatcher.find()) {
            String player = kickedMatcher.group(1);
            partyMembers.remove(player.toLowerCase());
            System.out.println("[PartyTracker] " + player + " was kicked from the party");
            return;
        }
        
        // Check for party transfer
        Matcher transferMatcher = PARTY_TRANSFER_PATTERN.matcher(message);
        if (transferMatcher.find()) {
            partyLeader = transferMatcher.group(1);
            isLeader = partyLeader.equalsIgnoreCase(myName);
            System.out.println("[PartyTracker] Party transferred to " + partyLeader);
            return;
        }
        
        // Check for promotion to leader
        Matcher promotedMatcher = PROMOTED_LEADER_PATTERN.matcher(message);
        if (promotedMatcher.find()) {
            partyLeader = promotedMatcher.group(1);
            isLeader = partyLeader.equalsIgnoreCase(myName);
            System.out.println("[PartyTracker] " + partyLeader + " promoted to leader");
            return;
        }
    }
    
    /**
     * Parse member names from a line like "Player1 (dot) Player2 (dot) Player3"
     */
    private void parseAndAddMembers(String membersLine, String myName) {
        // Split by common delimiters (bullet char or , or space after names)
        // The bullet character is Unicode \u25CF or similar
        String[] parts = membersLine.split("[\u25CF\u25CB\u2022\u00B7,]");
        for (String part : parts) {
            // Extract player name (remove rank brackets if present)
            String cleaned = part.replaceAll("\\[[^\\]]+\\]", "").trim();
            // Remove any trailing symbols
            cleaned = cleaned.replaceAll("[^a-zA-Z0-9_]", "").trim();
            
            if (!cleaned.isEmpty() && !cleaned.equalsIgnoreCase(myName)) {
                partyMembers.add(cleaned.toLowerCase());
                System.out.println("[PartyTracker] Found party member: " + cleaned);
            }
        }
        inParty = true;
    }
    
    /**
     * Clear all party data
     */
    private void clearParty() {
        partyMembers.clear();
        partyLeader = null;
        inParty = false;
        isLeader = false;
        System.out.println("[PartyTracker] Party cleared");
    }
    
    /**
     * Check if a player is in your in-game Hypixel party
     */
    public boolean isPlayerInParty(String playerName) {
        if (playerName == null) return false;
        
        String lowerName = playerName.toLowerCase();
        
        // Check if it's the leader
        if (partyLeader != null && partyLeader.equalsIgnoreCase(playerName)) {
            return true;
        }
        
        // Check if it's us
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.thePlayer.getName().equalsIgnoreCase(playerName)) {
            return inParty; // We're in the party if we're tracking a party
        }
        
        return partyMembers.contains(lowerName);
    }
    
    /**
     * Get all current party members (excluding self)
     */
    public Set<String> getPartyMembers() {
        return new HashSet<>(partyMembers);
    }
    
    /**
     * Get party leader name
     */
    public String getPartyLeader() {
        return partyLeader;
    }
    
    /**
     * Check if we're currently in a party
     */
    public boolean isInParty() {
        return inParty;
    }
    
    /**
     * Check if we're the party leader
     */
    public boolean isPartyLeader() {
        return isLeader;
    }
    
    /**
     * Get party size (including self)
     */
    public int getPartySize() {
        if (!inParty) return 0;
        return partyMembers.size() + 1; // +1 for self (or +1 for leader if we're not counting them separately)
    }
    
    /**
     * Force refresh party data by requesting /party list
     * This should be called when the GUI is opened to get current state
     */
    public void requestPartyRefresh() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage("/pl");
        }
    }
}
