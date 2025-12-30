package com.raven.client.features.Stockholder;

public class BazaarSnapshot {
    public final String timestamp;
    public final double buyPrice, sellPrice, buyVolume, sellVolume;

    public BazaarSnapshot(String timestamp, double buy, double sell, double buyVol, double sellVol) {
        this.timestamp = timestamp;
        this.buyPrice = buy;
        this.sellPrice = sell;
        this.buyVolume = buyVol;
        this.sellVolume = sellVol;
    }

    public double getMargin() {
        return sellPrice - buyPrice;
    }

    public double getMarginPercent() {
        return buyPrice > 0 ? (getMargin() / buyPrice) * 100 : 0;
    }
}