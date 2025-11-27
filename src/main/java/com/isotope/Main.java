package com.isotope;

import com.isotope.core.IsotopeEngine;
import com.isotope.strategy.PairsTradingStrategy;
import com.isotope.strategy.Strategy;
import com.isotope.strategy.StrategyLoader;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("Initializing Isotope MFT Engine...");

        IsotopeEngine engine = new IsotopeEngine();

        // Load strategies
        StrategyLoader loader = new StrategyLoader();
        // In a real scenario, we would pass a path to jars
        List<Strategy> loadedStrategies = loader.loadStrategies("strategies");

        // For demonstration, if loader returns empty (mock), we manually add the example
        if (loadedStrategies.isEmpty()) {
            log.info("No external strategies found. Loading default PairsTradingStrategy.");
            engine.registerStrategy(new PairsTradingStrategy());
        } else {
            for (Strategy s : loadedStrategies) {
                engine.registerStrategy(s);
            }
        }

        // Start the engine
        engine.start();

        // --- Simulation Block ---
        log.info("Starting Simulation...");
        simulateMarketData(engine);

        // Keep alive
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        engine.stop();
        log.info("Engine Shutdown.");
    }

    private static void simulateMarketData(IsotopeEngine engine) {
        // Mock Ticks for Nifty and BankNifty
        // Nifty Token: 256265, BankNifty Token: 260105

        long NIFTY = 256265;
        long BANKNIFTY = 260105;

        // Sequence 1: Normal ratio ~ 2.0 (40000 / 20000)
        sendTick(engine, NIFTY, 20000.0);
        sendTick(engine, BANKNIFTY, 40000.0);

        // Sequence 2: Divergence! BankNifty spikes to 60000 (Ratio 3.0)
        // Threshold is 2.5, so this should trigger a SELL BankNifty, BUY Nifty
        log.info("Simulating Market Spike...");
        sendTick(engine, NIFTY, 20000.0);
        sendTick(engine, BANKNIFTY, 60000.0);
    }

    private static void sendTick(IsotopeEngine engine, long token, double price) {
        Tick tick = new Tick();
        tick.setInstrumentToken(token);
        tick.setLastTradedPrice(price);
        tick.setVolumeTradedToday(1000);
        // tick.setTickTimestamp(new Date()); // Date might be tricky to mock without setters in some versions, ignoring for now

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
