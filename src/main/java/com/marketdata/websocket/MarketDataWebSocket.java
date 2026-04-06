package com.marketdata.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketdata.model.ClientSubscription;
import com.marketdata.model.MarketDataSnapshot;
import com.marketdata.model.Message;
import com.marketdata.service.MarketDataStore;

@ServerEndpoint("/ws/market-data")
@ApplicationScoped
public class MarketDataWebSocket {

    private static final Logger LOG = LoggerFactory.getLogger(MarketDataWebSocket.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private final Map<Session, ClientSubscription> subscriptions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mds-batch-sender");
        t.setDaemon(true);
        return t;
    });

    private final Map<Session, List<Message>> pendingMessages = new ConcurrentHashMap<>();
    private static final long BATCH_INTERVAL_MS = 50;

    @Inject
    MarketDataStore marketDataStore;

    public MarketDataWebSocket() {
        scheduler.scheduleAtFixedRate(
                this::flushPendingMessages,
                BATCH_INTERVAL_MS,
                BATCH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        LOG.info("MarketDataWebSocket initialized with {}ms batch interval", BATCH_INTERVAL_MS);
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        subscriptions.put(session, new ClientSubscription());
        pendingMessages.put(session, new CopyOnWriteArrayList<>());

        LOG.info("Client connected: session={}, totalClients={}", session.getId(), sessions.size());

        sendDirect(session, new Message("CONNECTED", Map.of(
                "sessionId", session.getId(),
                "timestamp", System.currentTimeMillis(),
                "version", "1.0.0"
        )));
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        subscriptions.remove(session);
        pendingMessages.remove(session);

        LOG.info("Client disconnected: session={}, remainingClients={}", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        LOG.error("WebSocket error for session {}: {}", session.getId(), error.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Map<String, Object> request = mapper.readValue(message, Map.class);
            String action = (String) request.get("action");

            LOG.debug("Received action: {}", action);

            switch (action) {
                case "SUBSCRIBE":
                    handleSubscribe(session, request);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscribe(session, request);
                    break;
                case "GET_SNAPSHOT":
                    handleGetSnapshot(session, request);
                    break;
                case "PING":
                    sendDirect(session, new Message("PONG", Map.of("timestamp", System.currentTimeMillis())));
                    break;
                default:
                    LOG.warn("Unknown action: {}", action);
                    sendDirect(session, new Message("ERROR", Map.of("message", "Unknown action: " + action)));
            }
        } catch (Exception e) {
            LOG.error("Error processing message: {}", e.getMessage());
            sendDirect(session, new Message("ERROR", Map.of("message", e.getMessage())));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSubscribe(Session session, Map<String, Object> request) {
        ClientSubscription sub = subscriptions.get(session);
        if (sub == null) return;

        List<String> symbols = (List<String>) request.get("symbols");

        if (symbols != null && !symbols.isEmpty()) {
            sub.marketDataSymbols.addAll(symbols);
            LOG.info("Session {} subscribed to symbols: {}", session.getId(), symbols);
        } else {
            LOG.info("Session {} subscribed to ALL symbols", session.getId());
        }

        sub.subscribedMarketData = true;

        sendDirect(session, new Message("SUBSCRIBED", Map.of(
                "symbols", symbols != null ? symbols : "all",
                "timestamp", System.currentTimeMillis()
        )));
    }

    private void handleUnsubscribe(Session session, Map<String, Object> request) {
        ClientSubscription sub = subscriptions.get(session);
        if (sub == null) return;

        sub.subscribedMarketData = false;
        sub.marketDataSymbols.clear();

        LOG.info("Session {} unsubscribed from market data", session.getId());

        sendDirect(session, new Message("UNSUBSCRIBED", Map.of(
                "timestamp", System.currentTimeMillis()
        )));
    }

    private void handleGetSnapshot(Session session, Map<String, Object> request) {
        ClientSubscription sub = subscriptions.get(session);
        if (sub == null) return;

        List<MarketDataSnapshot> snapshots = new ArrayList<>();

        if (sub.marketDataSymbols.isEmpty()) {
            snapshots.addAll(marketDataStore.getAllSnapshots());
        } else {
            for (String symbol : sub.marketDataSymbols) {
                MarketDataSnapshot snapshot = marketDataStore.getSnapshot(symbol);
                if (snapshot != null) {
                    snapshots.add(snapshot);
                }
            }
        }

        LOG.info("Sending snapshot to session {} with {} symbols", session.getId(), snapshots.size());

        sendDirect(session, new Message("SNAPSHOT_MARKET_DATA", Map.of(
                "data", snapshots,
                "timestamp", System.currentTimeMillis()
        )));
    }

    public void broadcastMarketData(String symbol, MarketDataSnapshot snapshot) {
        Message msg = new Message("MARKET_DATA", Map.of(
                "symbol", symbol,
                "lastPrice", snapshot.lastPrice,
                "bid", snapshot.bid,
                "ask", snapshot.ask,
                "change", snapshot.change,
                "changePercent", snapshot.changePercent,
                "timestamp", snapshot.timestamp
        ));

        for (Session session : sessions) {
            ClientSubscription sub = subscriptions.get(session);

            if (sub != null && sub.subscribedMarketData) {
                if (sub.marketDataSymbols.isEmpty() || sub.marketDataSymbols.contains(symbol)) {
                    queueMessage(session, msg);
                }
            }
        }
    }

    private void queueMessage(Session session, Message message) {
        List<Message> queue = pendingMessages.get(session);
        if (queue != null) {
            queue.add(message);
        }
    }

    private void flushPendingMessages() {
        for (Map.Entry<Session, List<Message>> entry : pendingMessages.entrySet()) {
            Session session = entry.getKey();
            List<Message> messages = entry.getValue();

            if (!messages.isEmpty() && session.isOpen()) {
                List<Message> batch = new ArrayList<>(messages);
                messages.clear();

                try {
                    String payload;

                    if (batch.size() == 1) {
                        payload = mapper.writeValueAsString(batch.get(0));
                    } else {
                        Message batchMsg = new Message("BATCH", Map.of("messages", batch));
                        payload = mapper.writeValueAsString(batchMsg);
                    }

                    session.getBasicRemote().sendText(payload);

                } catch (IOException e) {
                    LOG.error("Error sending batch to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    private void sendDirect(Session session, Message message) {
        if (session.isOpen()) {
            try {
                session.getBasicRemote().sendText(mapper.writeValueAsString(message));
            } catch (IOException e) {
                LOG.error("Error sending to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    public int getConnectedClientCount() {
        return sessions.size();
    }
}