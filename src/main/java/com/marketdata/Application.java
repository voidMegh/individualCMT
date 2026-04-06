package com.marketdata;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Application implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("Market Data Service (G3-M2) Starting...");
        System.out.println("WebSocket Endpoint: ws://localhost:8080/ws/market-data");
        System.out.println("========================================");

        Quarkus.waitForExit();
        return 0;
    }

    public static void main(String[] args) {
        Quarkus.run(Application.class, args);
    }
}