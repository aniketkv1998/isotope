package com.isotope.service;

import com.isotope.config.AppConfig;
import com.isotope.core.IsotopeEngine;
import com.isotope.strategy.PairsTradingStrategy;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.models.Tick;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingEngineManager {

    private final AppConfig appConfig;
    private final KiteConnect kiteConnect;
    private IsotopeEngine isotopeEngine;
    private final ExecutorService engineExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        log.info("Initializing Trading Engine Manager...");
        log.info("Kite API Key: {}", appConfig.getKite().getApiKey()); // Be careful logging secrets in prod

        // 1. Instantiate Core Engine (Pure Java)
        // Injecting Spring-managed KiteConnect bean into the pure Java core
        isotopeEngine = new IsotopeEngine(kiteConnect);

        // 2. Wire up Strategies
        // In a real app, we might scan the classpath or use a more dynamic loader
        // For now, we manually register the example strategy
        PairsTradingStrategy strategy = new PairsTradingStrategy();
        // Configure strategy with properties if needed (e.g., risk limits from config)
        // strategy.setRiskLimit(appConfig.getStrategy().getRiskLimit());
        isotopeEngine.registerStrategy(strategy);

        // 3. Start the Engine on a dedicated thread to ensure Spring doesn't block it
        // and to keep the hot path isolated.
        engineExecutor.submit(() -> {
            log.info("Starting Isotope Engine Loop...");
            isotopeEngine.start();

            // --- Simulation Start (Moved from old Main) ---
            // In a real scenario, this would be replaced by connecting to the WebSocket
            log.info("Starting Simulation in Background...");
            simulateMarketData(isotopeEngine);
            // --- Simulation End ---
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Trading Engine...");
        if (isotopeEngine != null) {
            isotopeEngine.stop();
        }
        engineExecutor.shutdown();
    }

    // --- Simulation Logic (Moved from Main.java for demonstration) ---
    private void simulateMarketData(IsotopeEngine engine) {
        try {
            // Give the engine a moment to start up fully
            Thread.sleep(1000);

            long NIFTY = 256265;
            long BANKNIFTY = 260105;

            // Sequence 1: Normal ratio ~ 2.0 (40000 / 20000)
            log.info("Simulating Normal Market Data...");
            sendTick(engine, NIFTY, 20000.0);
            sendTick(engine, BANKNIFTY, 40000.0);

            Thread.sleep(1000);

            // Sequence 2: Divergence! BankNifty spikes to 60000 (Ratio 3.0)
            // Threshold is 2.5, so this should trigger a SELL BankNifty, BUY Nifty
            log.info("Simulating Market Spike...");
            sendTick(engine, NIFTY, 20000.0);
            sendTick(engine, BANKNIFTY, 60000.0);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendTick(IsotopeEngine engine, long token, double price) {
        Tick tick = new Tick();
        tick.setInstrumentToken(token);
        tick.setLastTradedPrice(price);
        tick.setVolumeTradedToday(1000);

        // Mock Depth for bid/ask
        Map<String, ArrayList<com.zerodhatech.models.Depth>> depth = new HashMap<>();
        ArrayList<com.zerodhatech.models.Depth> buyDepth = new ArrayList<>();
        com.zerodhatech.models.Depth d1 = new com.zerodhatech.models.Depth();
        d1.setPrice(price - 1);
        d1.setQuantity(100);
        buyDepth.add(d1);
        depth.put("buy", buyDepth);

        ArrayList<com.zerodhatech.models.Depth> sellDepth = new ArrayList<>();
        com.zerodhatech.models.Depth d2 = new com.zerodhatech.models.Depth();
        d2.setPrice(price + 1);
        d2.setQuantity(100);
        sellDepth.add(d2);
        depth.put("sell", sellDepth);

        tick.setMarketDepth(depth);

        ArrayList<Tick> ticks = new ArrayList<>();
        ticks.add(tick);

        engine.getMarketDataAdapter().onTicks(ticks);
    }
}
