package com.raven.client.gui.partyfinder;

import java.util.ArrayList;
import java.util.List;

public class PartyFinderCategory {
    public final String name;
    public final int color;
    public final List<PartyFinderCategory> children;
    public PartyFinderCategory parent;
    
    // Filter requirements for this category
    public String primaryFilter;      // Main filter (e.g., "Catacombs Level", "Fishing Level")
    public int primaryFilterMin;      // Minimum value for primary filter
    public List<String> secondaryFilters; // Additional filters (Combat stats, etc.)
    
    public PartyFinderCategory(String name, int color) {
        this.name = name;
        this.color = color;
        this.children = new ArrayList<>();
        this.parent = null;
        this.primaryFilter = null;
        this.primaryFilterMin = 0;
        this.secondaryFilters = new ArrayList<>();
    }
    
    public PartyFinderCategory withPrimaryFilter(String filterName, int minValue) {
        this.primaryFilter = filterName;
        this.primaryFilterMin = minValue;
        return this;
    }
    
    public PartyFinderCategory withSecondaryFilter(String filterName) {
        this.secondaryFilters.add(filterName);
        return this;
    }
    
    public boolean hasFilters() {
        return primaryFilter != null || !secondaryFilters.isEmpty();
    }
    
    public PartyFinderCategory addChild(PartyFinderCategory child) {
        child.parent = this;
        this.children.add(child);
        return this;
    }
    
    public PartyFinderCategory addChild(String name, int color) {
        PartyFinderCategory child = new PartyFinderCategory(name, color);
        return addChild(child);
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    public String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " > " + name;
    }
    
    // Static method to build the full category tree
    public static List<PartyFinderCategory> buildCategoryTree() {
        List<PartyFinderCategory> root = new ArrayList<>();
        
        // DUNGEONS
        PartyFinderCategory dungeons = new PartyFinderCategory("Dungeons", 0xFFAA00);
        dungeons.withPrimaryFilter("Catacombs Level", 0)
                .withSecondaryFilter("Combat Level")
                .withSecondaryFilter("Class Level")
                .withSecondaryFilter("Secrets Found");
        
        // Catacombs
        PartyFinderCategory catacombs = new PartyFinderCategory("Catacombs", 0xFF5555);
        catacombs.withPrimaryFilter("Catacombs Level", 0);
        catacombs.addChild(new PartyFinderCategory("Floor 1", 0xFF6666).withPrimaryFilter("Catacombs Level", 0));
        catacombs.addChild(new PartyFinderCategory("Floor 2", 0xFF6666).withPrimaryFilter("Catacombs Level", 2));
        catacombs.addChild(new PartyFinderCategory("Floor 3", 0xFF6666).withPrimaryFilter("Catacombs Level", 5));
        catacombs.addChild(new PartyFinderCategory("Floor 4", 0xFF6666).withPrimaryFilter("Catacombs Level", 10));
        catacombs.addChild(new PartyFinderCategory("Floor 5", 0xFF6666).withPrimaryFilter("Catacombs Level", 15));
        catacombs.addChild(new PartyFinderCategory("Floor 6", 0xFF6666).withPrimaryFilter("Catacombs Level", 20));
        catacombs.addChild(new PartyFinderCategory("Floor 7", 0xFF6666).withPrimaryFilter("Catacombs Level", 24));
        dungeons.addChild(catacombs);
        
        // Master Mode
        PartyFinderCategory masterMode = new PartyFinderCategory("Master Mode", 0xAA0000);
        masterMode.withPrimaryFilter("Catacombs Level", 24)
                  .withSecondaryFilter("Combat Level")
                  .withSecondaryFilter("Secrets Found");
        masterMode.addChild(new PartyFinderCategory("M1", 0xBB2222).withPrimaryFilter("Catacombs Level", 24));
        masterMode.addChild(new PartyFinderCategory("M2", 0xBB2222).withPrimaryFilter("Catacombs Level", 26));
        masterMode.addChild(new PartyFinderCategory("M3", 0xBB2222).withPrimaryFilter("Catacombs Level", 28));
        masterMode.addChild(new PartyFinderCategory("M4", 0xBB2222).withPrimaryFilter("Catacombs Level", 30));
        masterMode.addChild(new PartyFinderCategory("M5", 0xBB2222).withPrimaryFilter("Catacombs Level", 32));
        masterMode.addChild(new PartyFinderCategory("M6", 0xBB2222).withPrimaryFilter("Catacombs Level", 34));
        masterMode.addChild(new PartyFinderCategory("M7", 0xBB2222).withPrimaryFilter("Catacombs Level", 36));
        dungeons.addChild(masterMode);
        
        root.add(dungeons);
        
        // KUUDRA
        PartyFinderCategory kuudra = new PartyFinderCategory("Kuudra", 0xFF5555);
        kuudra.withPrimaryFilter("Combat Level", 0)
              .withSecondaryFilter("Crimson Isle Rep")
              .withSecondaryFilter("Kuudra Completions");
        kuudra.addChild(new PartyFinderCategory("Basic", 0xFF7777).withPrimaryFilter("Combat Level", 20));
        kuudra.addChild(new PartyFinderCategory("Hot", 0xFF9955).withPrimaryFilter("Combat Level", 25));
        kuudra.addChild(new PartyFinderCategory("Burning", 0xFFAA33).withPrimaryFilter("Combat Level", 28));
        kuudra.addChild(new PartyFinderCategory("Fiery", 0xFFCC00).withPrimaryFilter("Combat Level", 32));
        kuudra.addChild(new PartyFinderCategory("Infernal", 0xFF0000).withPrimaryFilter("Combat Level", 36));
        root.add(kuudra);
        
        // EVENTS
        PartyFinderCategory events = new PartyFinderCategory("Events", 0xFFFF55);
        events.withPrimaryFilter("SkyBlock Level", 0);
        events.addChild(new PartyFinderCategory("Diana (Mythological)", 0xFFAA00)
              .withPrimaryFilter("Mythological Kills", 0)
              .withSecondaryFilter("Griffin Pet Level"));
        events.addChild(new PartyFinderCategory("Jerry's Workshop", 0x55FF55).withPrimaryFilter("SkyBlock Level", 0));
        events.addChild(new PartyFinderCategory("Spooky Festival", 0xFF5500).withPrimaryFilter("Combat Level", 10));
        root.add(events);
        
        // BESTIARY
        PartyFinderCategory bestiary = new PartyFinderCategory("Bestiary", 0x88DD88);
        bestiary.withPrimaryFilter("Combat Level", 0)
                .withSecondaryFilter("Bestiary Milestone");
        bestiary.addChild(new PartyFinderCategory("Hub", 0x55AA55).withPrimaryFilter("Combat Level", 5));
        bestiary.addChild(new PartyFinderCategory("The Barn", 0xFFAA00).withPrimaryFilter("Combat Level", 5));
        bestiary.addChild(new PartyFinderCategory("Mushroom Desert", 0xCC8855).withPrimaryFilter("Combat Level", 10));
        bestiary.addChild(new PartyFinderCategory("The Park", 0x55DD55).withPrimaryFilter("Combat Level", 10));
        bestiary.addChild(new PartyFinderCategory("Spiders Den", 0xAA00AA).withPrimaryFilter("Combat Level", 10));
        bestiary.addChild(new PartyFinderCategory("The End", 0xAA55FF).withPrimaryFilter("Combat Level", 15));
        bestiary.addChild(new PartyFinderCategory("Crimson Isle", 0xFF5555).withPrimaryFilter("Combat Level", 25));
        bestiary.addChild(new PartyFinderCategory("Gold Mine", 0xFFCC00).withPrimaryFilter("Combat Level", 5));
        bestiary.addChild(new PartyFinderCategory("Deep Caverns", 0x5555AA).withPrimaryFilter("Combat Level", 10));
        bestiary.addChild(new PartyFinderCategory("Dwarven Mines", 0x00AAAA).withPrimaryFilter("Combat Level", 15));
        bestiary.addChild(new PartyFinderCategory("Crystal Hollows", 0xAA00FF).withPrimaryFilter("Combat Level", 20));
        bestiary.addChild(new PartyFinderCategory("Backwater Bayou", 0x557755).withPrimaryFilter("Combat Level", 15));
        bestiary.addChild(new PartyFinderCategory("Rift", 0xAA55FF).withPrimaryFilter("Combat Level", 20));
        bestiary.addChild(new PartyFinderCategory("Jerrys Workshop", 0x55FF55).withPrimaryFilter("Combat Level", 5));
        root.add(bestiary);
        
        // SLAYERS
        PartyFinderCategory slayers = new PartyFinderCategory("Slayers", 0x55FF55);
        slayers.withPrimaryFilter("Combat Level", 0)
               .withSecondaryFilter("Slayer XP");
        
        PartyFinderCategory revenant = new PartyFinderCategory("Revenant Horror", 0x00AA00);
        revenant.withPrimaryFilter("Zombie Slayer XP", 0);
        revenant.addChild(new PartyFinderCategory("Tier 1", 0x55FF55).withPrimaryFilter("Combat Level", 5));
        revenant.addChild(new PartyFinderCategory("Tier 2", 0x55FF55).withPrimaryFilter("Combat Level", 10));
        revenant.addChild(new PartyFinderCategory("Tier 3", 0x55FF55).withPrimaryFilter("Combat Level", 15));
        revenant.addChild(new PartyFinderCategory("Tier 4", 0x55FF55).withPrimaryFilter("Combat Level", 20));
        revenant.addChild(new PartyFinderCategory("Tier 5", 0x55FF55).withPrimaryFilter("Combat Level", 25));
        slayers.addChild(revenant);
        
        PartyFinderCategory tarantula = new PartyFinderCategory("Tarantula Broodfather", 0xAA00AA);
        tarantula.withPrimaryFilter("Spider Slayer XP", 0);
        tarantula.addChild(new PartyFinderCategory("Tier 1", 0xFF55FF).withPrimaryFilter("Combat Level", 5));
        tarantula.addChild(new PartyFinderCategory("Tier 2", 0xFF55FF).withPrimaryFilter("Combat Level", 10));
        tarantula.addChild(new PartyFinderCategory("Tier 3", 0xFF55FF).withPrimaryFilter("Combat Level", 15));
        tarantula.addChild(new PartyFinderCategory("Tier 4", 0xFF55FF).withPrimaryFilter("Combat Level", 20));
        slayers.addChild(tarantula);
        
        PartyFinderCategory sven = new PartyFinderCategory("Sven Packmaster", 0x5555FF);
        sven.withPrimaryFilter("Wolf Slayer XP", 0);
        sven.addChild(new PartyFinderCategory("Tier 1", 0x7777FF).withPrimaryFilter("Combat Level", 5));
        sven.addChild(new PartyFinderCategory("Tier 2", 0x7777FF).withPrimaryFilter("Combat Level", 10));
        sven.addChild(new PartyFinderCategory("Tier 3", 0x7777FF).withPrimaryFilter("Combat Level", 15));
        sven.addChild(new PartyFinderCategory("Tier 4", 0x7777FF).withPrimaryFilter("Combat Level", 20));
        slayers.addChild(sven);
        
        PartyFinderCategory voidgloom = new PartyFinderCategory("Voidgloom Seraph", 0xAA55FF);
        voidgloom.withPrimaryFilter("Enderman Slayer XP", 0);
        voidgloom.addChild(new PartyFinderCategory("Tier 1", 0xBB77FF).withPrimaryFilter("Combat Level", 15));
        voidgloom.addChild(new PartyFinderCategory("Tier 2", 0xBB77FF).withPrimaryFilter("Combat Level", 20));
        voidgloom.addChild(new PartyFinderCategory("Tier 3", 0xBB77FF).withPrimaryFilter("Combat Level", 25));
        voidgloom.addChild(new PartyFinderCategory("Tier 4", 0xBB77FF).withPrimaryFilter("Combat Level", 30));
        slayers.addChild(voidgloom);
        
        PartyFinderCategory inferno = new PartyFinderCategory("Inferno Demonlord", 0xFF5500);
        inferno.withPrimaryFilter("Blaze Slayer XP", 0);
        inferno.addChild(new PartyFinderCategory("Tier 1", 0xFF7733).withPrimaryFilter("Combat Level", 20));
        inferno.addChild(new PartyFinderCategory("Tier 2", 0xFF7733).withPrimaryFilter("Combat Level", 25));
        inferno.addChild(new PartyFinderCategory("Tier 3", 0xFF7733).withPrimaryFilter("Combat Level", 30));
        inferno.addChild(new PartyFinderCategory("Tier 4", 0xFF7733).withPrimaryFilter("Combat Level", 35));
        slayers.addChild(inferno);
        
        root.add(slayers);
        
        // FISHING
        PartyFinderCategory fishing = new PartyFinderCategory("Fishing", 0x5555FF);
        fishing.withPrimaryFilter("Fishing Level", 0)
               .withSecondaryFilter("Sea Creature Kills")
               .withSecondaryFilter("Trophy Fish");
        fishing.addChild(new PartyFinderCategory("Lava Fishing", 0xFF5500)
               .withPrimaryFilter("Fishing Level", 25)
               .withSecondaryFilter("Magma Lord Kills"));
        fishing.addChild(new PartyFinderCategory("Trophy Fishing", 0xFFAA00)
               .withPrimaryFilter("Fishing Level", 20)
               .withSecondaryFilter("Trophy Fish Caught"));
        fishing.addChild(new PartyFinderCategory("Sea Creatures", 0x00AAAA).withPrimaryFilter("Fishing Level", 15));
        fishing.addChild(new PartyFinderCategory("Plhlegblast", 0xAA00AA).withPrimaryFilter("Fishing Level", 30));
        root.add(fishing);
        
        // MINING
        PartyFinderCategory mining = new PartyFinderCategory("Mining", 0x55FFFF);
        mining.withPrimaryFilter("Mining Level", 0)
              .withSecondaryFilter("HOTM Level")
              .withSecondaryFilter("Powder");
        
        PartyFinderCategory dwarven = new PartyFinderCategory("Dwarven Mines", 0x00AAAA);
        dwarven.withPrimaryFilter("Mining Level", 10);
        dwarven.addChild(new PartyFinderCategory("Commission Parties", 0x55FFFF).withPrimaryFilter("Mining Level", 10));
        dwarven.addChild(new PartyFinderCategory("Titanium Grinding", 0xCCCCCC).withPrimaryFilter("Mining Level", 15));
        mining.addChild(dwarven);
        
        PartyFinderCategory crystal = new PartyFinderCategory("Crystal Hollows", 0xAA00FF);
        crystal.withPrimaryFilter("HOTM Level", 3);
        crystal.addChild(new PartyFinderCategory("Nucleus Runs", 0xBB55FF).withPrimaryFilter("HOTM Level", 4));
        crystal.addChild(new PartyFinderCategory("Gemstone Grinding", 0xFF55AA).withPrimaryFilter("HOTM Level", 5));
        crystal.addChild(new PartyFinderCategory("Worm Fishing", 0x884400).withPrimaryFilter("Fishing Level", 20));
        mining.addChild(crystal);
        
        PartyFinderCategory glacite = new PartyFinderCategory("Glacite Tunnels", 0x55AAFF);
        glacite.withPrimaryFilter("HOTM Level", 5);
        glacite.addChild(new PartyFinderCategory("Corpse Runs", 0x7799FF).withPrimaryFilter("HOTM Level", 6));
        glacite.addChild(new PartyFinderCategory("Mineshaft", 0x5588DD).withPrimaryFilter("HOTM Level", 7));
        mining.addChild(glacite);
        
        root.add(mining);
        
        // RIFT
        PartyFinderCategory rift = new PartyFinderCategory("Rift", 0xAA55FF);
        rift.withPrimaryFilter("Rift Unlocked", 1)
            .withSecondaryFilter("Motes");
        rift.addChild(new PartyFinderCategory("Rift Bosses", 0xBB77FF).withPrimaryFilter("Combat Level", 20));
        rift.addChild(new PartyFinderCategory("Motes Farming", 0x9944DD).withPrimaryFilter("Rift Unlocked", 1));
        root.add(rift);
        
        // OTHER
        PartyFinderCategory other = new PartyFinderCategory("Other", 0xAAAAAA);
        other.addChild(new PartyFinderCategory("Dragon Fights", 0xAA00AA)
             .withPrimaryFilter("Combat Level", 20)
             .withSecondaryFilter("Dragon Kills"));
        root.add(other);
        
        return root;
    }
}
