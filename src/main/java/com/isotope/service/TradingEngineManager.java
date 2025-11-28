package com.isotope.service;

import com.isotope.adapter.MarketDataProducer;
import com.isotope.adapter.OrderExecutionAdapter;
import com.isotope.config.AppConfig;
import com.isotope.core.IsotopeEngine;
import com.isotope.strategy.PairsTradingStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingEngineManager {

    private final AppConfig appConfig;
    private final MarketDataProducer marketDataProducer; // Injected by Spring (Kite, CSV, or Yahoo)
    private final IndianDerivativesFeeCalculator feeCalculator;
    private IsotopeEngine isotopeEngine;
    private final ExecutorService engineExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        log.info("Initializing Trading Engine Manager...");

        double capital = appConfig.getBacktest().getInitialCapital();
        String executionMode = appConfig.getStrategy().getExecutionMode();
        OrderExecutionAdapter adapter = new OrderExecutionAdapter(feeCalculator, capital, executionMode);

        // 1. Instantiate Core Engine (Pure Java)
        isotopeEngine = new IsotopeEngine(adapter);

        // 2. Wire up Strategies
        PairsTradingStrategy strategy = new PairsTradingStrategy(appConfig.getStrategy().getAllocationPerLeg());
        isotopeEngine.registerStrategy(strategy);

        // 3. Start the Engine on a dedicated thread
        engineExecutor.submit(() -> {
            log.info("Starting Isotope Engine Loop...");
            isotopeEngine.start();

            // 4. Connect and Start Market Data Producer
            log.info("Connecting Market Data Producer...");
            marketDataProducer.connect();

            // Subscribe to symbols (example symbols)
            marketDataProducer.subscribe("NIFTY", "BANKNIFTY");

            log.info("Starting Market Data Publishing...");
            marketDataProducer.startPublishing(isotopeEngine.getMarketDataRingBuffer());
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
}
