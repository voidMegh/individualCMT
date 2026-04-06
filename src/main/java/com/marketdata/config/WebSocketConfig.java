package com.marketdata.config;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

@ApplicationScoped
public class WebSocketConfig {

    public void configureEndpoints(ServerContainer serverContainer) throws DeploymentException {
        // MarketDataWebSocket is auto-registered via @ServerEndpoint annotation
    }
}