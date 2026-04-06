package com.marketdata.model;

public class MarketDataSnapshot {
    public String symbol;
    public double lastPrice;
    public double bid;
    public double ask;
    public double open;
    public double high;
    public double low;
    public double change;
    public double changePercent;
    public long volume;
    public long timestamp;

    public MarketDataSnapshot() {
    }

    public MarketDataSnapshot(String symbol, double lastPrice, double bid, double ask, long timestamp) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    public double getMidPrice() {
        return (bid + ask) / 2.0;
    }

    public double getSpread() {
        return ask - bid;
    }

    public double getSpreadPercent() {
        double mid = getMidPrice();
        return mid > 0 ? (getSpread() / mid) * 100 : 0;
    }

    @Override
    public String toString() {
        return String.format("%s: %.2f (bid=%.2f ask=%.2f) %+.2f (%.2f%%) vol=%d",
                symbol, lastPrice, bid, ask, change, changePercent, volume);
    }
}