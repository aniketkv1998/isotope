package com.isotope.strategy;

import com.isotope.core.OrderPublisher;
import com.isotope.model.MarketDataEvent;
import com.isotope.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;

/**
 * DYNAMIC Strategy: Pairs Trading (Nifty vs BankNifty).
 * - Self-calibrating: Uses a Simple Moving Average (SMA) to find the 'True Mean'.
 * - Adapts to any market regime (2023 data, 2025 data, doesn't matter).
 */
@Slf4j
public class PairsTradingStrategy implements Strategy {

    private OrderPublisher orderPublisher;
    private final String strategyId = "PairsTrading_Dynamic";
    private final double allocationPerLeg;

    public PairsTradingStrategy(double allocationPerLeg) {
        this.allocationPerLeg = allocationPerLeg;
    }

    // Prices
    private double lastNiftyPrice = 0.0;
    private double lastBankNiftyPrice = 0.0;

    // Dynamic Math Parameters
    private final int LOOKBACK_PERIOD = 200; // Learn from last 200 ticks
    private final Queue<Double> ratioHistory = new LinkedList<>();
    private double sumRatios = 0.0;

    // Trading Thresholds
    private final double ENTRY_THRESHOLD = 0.0015; // 0.15% deviation (Sensitive)
    private final double EXIT_THRESHOLD = 0.0005;  // 0.05% deviation (Take Profit quickly)

    // Position State
    private enum Position { NONE, LONG_SPREAD, SHORT_SPREAD }
    private Position currentPosition = Position.NONE;

    private int currentBnQty = 0;
    private int currentNiftyQty = 0;

    private long currentTickTime = 0;

    @Override
    public void onTick(MarketDataEvent event) {
        currentTickTime = event.getLastTradedTime();
        // 1. Ingest Prices
        if (event.getInstrumentToken() == 256265) {
            lastNiftyPrice = event.getLastTradedPrice();
        } else if (event.getInstrumentToken() == 260105) {
            lastBankNiftyPrice = event.getLastTradedPrice();
        }

        // 2. Wait for valid data
        if (lastNiftyPrice == 0 || lastBankNiftyPrice == 0) return;

        // 3. Calculate Ratio
        double currentRatio = lastBankNiftyPrice / lastNiftyPrice;

        // 4. Update Moving Average (The "Learning" Part)
        updateMovingAverage(currentRatio);

        // 5. Only trade if we have enough data to know the mean
        if (ratioHistory.size() < LOOKBACK_PERIOD) return;

        double meanRatio = sumRatios / ratioHistory.size();

        // 6. Trading Logic
        checkSignals(currentRatio, meanRatio);
    }

    private void updateMovingAverage(double currentRatio) {
        ratioHistory.add(currentRatio);
        sumRatios += currentRatio;

        if (ratioHistory.size() > LOOKBACK_PERIOD) {
            double old = ratioHistory.poll();
            sumRatios -= old;
        }
    }

    private void checkSignals(double current, double mean) {
        double divergence = current - mean;

        // Guard against zero prices
        if (lastBankNiftyPrice <= 0 || lastNiftyPrice <= 0) {
            log.warn("Invalid prices: BN={}, Nifty={}. Skipping signal check.", lastBankNiftyPrice, lastNiftyPrice);
            return;
        }

        // --- LOGIC: SHORT SPREAD (Betting Ratio goes DOWN) ---
        if (currentPosition == Position.NONE && divergence > ENTRY_THRESHOLD) {
            // Calculate entry quantities
            currentBnQty = (int) (allocationPerLeg / lastBankNiftyPrice);
            currentNiftyQty = (int) (allocationPerLeg / lastNiftyPrice);

            log.info("ENTRY SHORT: Ratio {} is too High (Mean {}). SELL BN / BUY NIFTY", fmt(current), fmt(mean));
            execute(OrderEvent.Type.SELL, "BANKNIFTY", currentBnQty, lastBankNiftyPrice);
            execute(OrderEvent.Type.BUY, "NIFTY", currentNiftyQty, lastNiftyPrice);
            currentPosition = Position.SHORT_SPREAD;
        }
        // Exit Short
        else if (currentPosition == Position.SHORT_SPREAD && divergence < EXIT_THRESHOLD) {
            log.info("EXIT SHORT: Ratio {} returned to Mean {}. PROFIT.", fmt(current), fmt(mean));
            execute(OrderEvent.Type.BUY, "BANKNIFTY", currentBnQty, lastBankNiftyPrice); // Cover
            execute(OrderEvent.Type.SELL, "NIFTY", currentNiftyQty, lastNiftyPrice);       // Sell

            // Reset state
            currentPosition = Position.NONE;
            currentBnQty = 0;
            currentNiftyQty = 0;
        }

        // --- LOGIC: LONG SPREAD (Betting Ratio goes UP) ---
        else if (currentPosition == Position.NONE && divergence < -ENTRY_THRESHOLD) {
            // Calculate entry quantities
            currentBnQty = (int) (allocationPerLeg / lastBankNiftyPrice);
            currentNiftyQty = (int) (allocationPerLeg / lastNiftyPrice);

            log.info("ENTRY LONG: Ratio {} is too Low (Mean {}). BUY BN / SELL NIFTY", fmt(current), fmt(mean));
            execute(OrderEvent.Type.BUY, "BANKNIFTY", currentBnQty, lastBankNiftyPrice);
            execute(OrderEvent.Type.SELL, "NIFTY", currentNiftyQty, lastNiftyPrice);
            currentPosition = Position.LONG_SPREAD;
        }
        // Exit Long
        else if (currentPosition == Position.LONG_SPREAD && divergence > -EXIT_THRESHOLD) {
            log.info("EXIT LONG: Ratio {} returned to Mean {}. PROFIT.", fmt(current), fmt(mean));
            execute(OrderEvent.Type.SELL, "BANKNIFTY", currentBnQty, lastBankNiftyPrice); // Sell
            execute(OrderEvent.Type.BUY, "NIFTY", currentNiftyQty, lastNiftyPrice);          // Cover

            // Reset state
            currentPosition = Position.NONE;
            currentBnQty = 0;
            currentNiftyQty = 0;
        }
    }

    private void execute(OrderEvent.Type type, String symbol, int qty, double price) {
        if (orderPublisher != null) {
            orderPublisher.publishOrder(symbol, type, qty, price, strategyId, currentTickTime);
        }
    }

    private String fmt(double val) {
        return String.format("%.4f", val);
    }

    @Override
    public void setOrderPublisher(OrderPublisher orderPublisher) {
        this.orderPublisher = orderPublisher;
    }

    @Override
    public String getStrategyId() {
        return strategyId;
    }
}