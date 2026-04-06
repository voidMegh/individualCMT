package com.marketdata.websocket;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.marketdata.model.ClientSubscription;

public class MarketDataWebSocketTest {

    private ClientSubscription subscription;

    @Before
    public void setup() {
        subscription = new ClientSubscription();
    }

    @Test
    public void testSubscribeToSpecificSymbols() {
        subscription.subscribedMarketData = true;
        subscription.marketDataSymbols.addAll(List.of("AAPL", "MSFT"));

        assertTrue("Should subscribe to AAPL", subscription.shouldReceive("MARKET_DATA", "AAPL"));
        assertTrue("Should subscribe to MSFT", subscription.shouldReceive("MARKET_DATA", "MSFT"));
        assertFalse("Should not subscribe to GOOGL", subscription.shouldReceive("MARKET_DATA", "GOOGL"));
    }

    @Test
    public void testSubscribeToAllSymbols() {
        subscription.subscribedMarketData = true;

        assertTrue("Should subscribe to AAPL", subscription.shouldReceive("MARKET_DATA", "AAPL"));
        assertTrue("Should subscribe to MSFT", subscription.shouldReceive("MARKET_DATA", "MSFT"));
        assertTrue("Should subscribe to any symbol", subscription.shouldReceive("MARKET_DATA", "GOOGL"));
    }

    @Test
    public void testUnsubscribe() {
        subscription.subscribedMarketData = true;
        subscription.marketDataSymbols.addAll(List.of("AAPL", "MSFT"));

        assertTrue("Should be subscribed before unsubscribe", subscription.subscribedMarketData);

        subscription.unsubscribe("MARKET_DATA");

        assertFalse("Should not be subscribed after unsubscribe", subscription.subscribedMarketData);
        assertTrue("Symbol set should be cleared", subscription.marketDataSymbols.isEmpty());
    }

    @Test
    public void testChannelNotSubscribed() {
        subscription.subscribedMarketData = false;

        assertFalse("Should not receive if channel not subscribed",
                subscription.shouldReceive("MARKET_DATA", "AAPL"));
    }
}