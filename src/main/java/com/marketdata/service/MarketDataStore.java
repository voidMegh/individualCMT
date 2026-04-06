package com.marketdata.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marketdata.model.MarketDataSnapshot;
import com.marketdata.util.DeltaDetector;

@ApplicationScoped
@Singleton
public class MarketDataStore {

    private static final Logger LOG = LoggerFactory.getLogger(MarketDataStore.class);

    private final Map<String, MarketDataSnapshot> store = new ConcurrentHashMap<>();
    private volatile MarketDataListener listener;

    public boolean putSnapshot(String symbol, MarketDataSnapshot snapshot) {
        MarketDataSnapshot previous = store.get(symbol);

        boolean isDelta = DeltaDetector.isDeltaUpdate(previous, snapshot);

        if (isDelta) {
            store.put(symbol, snapshot);
            LOG.debug("Updated {} -> {}", symbol, snapshot);

            if (listener != null) {
                try {
                    listener.onMarketDataUpdate(snapshot);
                } catch (Exception e) {
                    LOG.error("Error notifying listener for {}: {}", symbol, e.getMessage());
                }
            }
        }

        return isDelta;
    }

    public MarketDataSnapshot getSnapshot(String symbol) {
        return store.get(symbol);
    }

    public Collection<MarketDataSnapshot> getAllSnapshots() {
        return Collections.unmodifiableCollection(store.values());
    }

    public Collection<MarketDataSnapshot> getSnapshots(String... symbols) {
        java.util.List<MarketDataSnapshot> results = new java.util.ArrayList<>();
        for (String symbol : symbols) {
            MarketDataSnapshot snapshot = store.get(symbol);
            if (snapshot != null) {
                results.add(snapshot);
            }
        }
        return results;
    }

    public void clear() {
        store.clear();
        LOG.info("Market data store cleared");
    }

    public int size() {
        return store.size();
    }

    public boolean contains(String symbol) {
        return store.containsKey(symbol);
    }

    public void setListener(MarketDataListener listener) {
        this.listener = listener;
    }

    @FunctionalInterface
    public interface MarketDataListener {
        void onMarketDataUpdate(MarketDataSnapshot snapshot);
    }
}