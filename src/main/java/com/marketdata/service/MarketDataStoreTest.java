package com.marketdata.service;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.marketdata.model.MarketDataSnapshot;

public class MarketDataStoreTest {

    private MarketDataStore store;

    @Before
    public void setup() {
        store = new MarketDataStore();
    }

    @Test
    public void testPutAndGetSnapshot() {
        MarketDataSnapshot snapshot = new MarketDataSnapshot("AAPL", 150.00, 149.98, 150.02, System.currentTimeMillis());

        boolean isDelta = store.putSnapshot("AAPL", snapshot);
        assertTrue("First update should be delta", isDelta);

        MarketDataSnapshot retrieved = store.getSnapshot("AAPL");
        assertNotNull("Snapshot should be found", retrieved);
        assertEquals("Symbol should match", "AAPL", retrieved.symbol);
        assertEquals("Price should match", 150.00, retrieved.lastPrice, 0.001);
    }

    @Test
    public void testDeltaDetection() {
        MarketDataSnapshot snap1 = new MarketDataSnapshot("MSFT", 300.00, 299.98, 300.02, System.currentTimeMillis());
        store.putSnapshot("MSFT", snap1);

        MarketDataSnapshot snap2 = new MarketDataSnapshot("MSFT", 300.001, 299.98, 300.02, System.currentTimeMillis());
        boolean isDelta = store.putSnapshot("MSFT", snap2);
        assertFalse("Small change should not trigger delta", isDelta);

        MarketDataSnapshot snap3 = new MarketDataSnapshot("MSFT", 300.10, 299.98, 300.02, System.currentTimeMillis());
        isDelta = store.putSnapshot("MSFT", snap3);
        assertTrue("Large change should trigger delta", isDelta);
    }

    @Test
    public void testGetAllSnapshots() {
        store.putSnapshot("AAPL", new MarketDataSnapshot("AAPL", 150.00, 149.98, 150.02, System.currentTimeMillis()));
        store.putSnapshot("MSFT", new MarketDataSnapshot("MSFT", 300.00, 299.98, 300.02, System.currentTimeMillis()));
        store.putSnapshot("GOOGL", new MarketDataSnapshot("GOOGL", 2800.00, 2799.98, 2800.02, System.currentTimeMillis()));

        assertEquals("Should have 3 snapshots", 3, store.getAllSnapshots().size());
    }

    @Test
    public void testClear() {
        store.putSnapshot("AAPL", new MarketDataSnapshot("AAPL", 150.00, 149.98, 150.02, System.currentTimeMillis()));
        assertEquals("Should have 1 snapshot", 1, store.size());

        store.clear();
        assertEquals("Should have 0 snapshots after clear", 0, store.size());
    }
}