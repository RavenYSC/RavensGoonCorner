package com.raven.client.features;

import java.util.ArrayList;
import java.util.List;

public class FeatureManager {

    private final List<Feature> features = new ArrayList<>();

    public void register(Feature feature) {
        features.add(feature);
    }

    public List<Feature> getAllFeatures() {
        return features;
    }

    public Feature getFeatureByName(String name) {
        for (Feature f : features) {
            if (f.getName().equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }
}
