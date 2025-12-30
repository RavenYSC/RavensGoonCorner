package com.raven.client.features.Stockholder;

public class BazaarItem {
    public final String id;
    public final double buyPrice;
    public final double sellPrice;
    public final double buyVolume;
    public final double sellVolume;

    public BazaarItem(String id, double buyPrice, double sellPrice, double buyVolume, double sellVolume) {
        this.id = id;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.buyVolume = buyVolume;
        this.sellVolume = sellVolume;
    }

    public double getMargin() {
        return sellPrice - buyPrice;
    }

    public double getMarginPercent() {
        return buyPrice > 0 ? ((sellPrice - buyPrice) / buyPrice) * 100 : 0;
    }
}