package com.marketdata.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSubscription {

    public boolean subscribedMarketData = false;
    public Set<String> marketDataSymbols = ConcurrentHashMap.newKeySet();

    public void subscribe(String channel, Set<String> symbols) {
        switch (channel) {
            case "MARKET_DATA":
                subscribedMarketData = true;
                if (symbols != null && !symbols.isEmpty()) {
                    marketDataSymbols.addAll(symbols);
                }
                break;
        }
    }

    public void unsubscribe(String channel) {
        switch (channel) {
            case "MARKET_DATA":
                subscribedMarketData = false;
                marketDataSymbols.clear();
                break;
        }
    }

    public boolean shouldReceive(String channel, String symbol) {
        switch (channel) {
            case "MARKET_DATA":
                return subscribedMarketData &&
                        (marketDataSymbols.isEmpty() || marketDataSymbols.contains(symbol));
            default:
                return false;
        }
    }

    public void clear() {
        subscribedMarketData = false;
        marketDataSymbols.clear();
    }
}