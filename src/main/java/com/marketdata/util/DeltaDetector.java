package com.marketdata.util;

import com.marketdata.model.MarketDataSnapshot;

public class DeltaDetector {

    private static final double DELTA_THRESHOLD = 0.00001;

    public static boolean isDeltaUpdate(MarketDataSnapshot lastSnapshot, MarketDataSnapshot newSnapshot) {
        if (lastSnapshot == null) {
            return true;
        }

        if (lastSnapshot.lastPrice <= 0 || newSnapshot.lastPrice <= 0) {
            return true;
        }

        double priceChange = Math.abs(newSnapshot.lastPrice - lastSnapshot.lastPrice);
        double percentChange = priceChange / lastSnapshot.lastPrice;

        return percentChange > DELTA_THRESHOLD;
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static double calculateSpreadPercent(double bid, double ask) {
        double mid = (bid + ask) / 2.0;
        return mid > 0 ? ((ask - bid) / mid) * 100 : 0;
    }
}