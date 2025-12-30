package com.raven.client.features;

public abstract class Feature {

    private final String name;
    private FeatureCategory category = null; 
    private boolean enabled = false;

    // Updated constructor to include category
    public Feature(String name, FeatureCategory category) {
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public FeatureCategory getCategory() { 
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onEnable() {}
    public void onDisable() {}
}
