package com.raven.client.features;

public enum FeatureCategory {
    GENERAL("General"),
    RENDER("Render"),
    QOL("Quality of Life"),
    STOCKHOLDER("Bazaar"),
    DUNGEONS("Dungeons"),
    COMBAT("Combat"),
    SLAYERS("Slayers"),
    FISHING("Fishing"),
    MINING("Mining"),
    FORAGING("Foraging"),
    RIFT("Rift"),
    MISC("Misc"),
    Raven("Raven's Stuff"),
	Jelle("Jelle's Stuff"),
	Kris("Kris' Stuff"),
    Dev("To be Developed");
	
    private final String displayName;

    FeatureCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return displayName;
    }
}
