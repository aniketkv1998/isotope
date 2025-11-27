package com.isotope.strategy;

import com.isotope.core.OrderPublisher;
import com.isotope.model.MarketDataEvent;
import com.isotope.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;

/**
 * FIXED STRATEGY: Pairs Trading (Nifty vs BankNifty).
 * - Fixes "Quantity Mismatch Bug" (Ensures Exit Qty == Entry Qty).
 * - Tuned Thresholds for Net Profit.
 */
@Slf4j
public class PairsTradingStrategy implements Strategy {

    private OrderPublisher orderPublisher;
    private final String strategyId = "PairsTrading_Pro_Fixed";
    private final double allocationPerLeg;

    // Prices
    private double lastNiftyPrice = 0.0;
    private double lastBankNiftyPrice = 0.0;
    private long currentTickTime = 0;

    // Statistics
    private final int LOOKBACK_PERIOD = 50; // Fast adaptation
    private final Queue<Double> ratioHistory = new LinkedList<>();
    private double sumRatios = 0.0;

    // --- SETTINGS ---
    private final double ENTRY_Z_SCORE = 2.5;
    private final double EXIT_Z_SCORE = 0.0;
    private final double STOP_LOSS_Z_SCORE = 3.5;

    // Bumped slightly to 0.22% to ensure Green PnL
    private final double MIN_PROFIT_THRESHOLD = 0.0022;

    // Position State
    private enum Position { NONE, LONG_SPREAD, SHORT_SPREAD }
    private Position currentPosition = Position.NONE;

    // --- STATE TRACKING (THE FIX) ---
    private int heldBnQty = 0;
    private int heldNiftyQty = 0;

    public PairsTradingStrategy(double allocationPerLeg) {
        this.allocationPerLeg = allocationPerLeg;
    }

    @Override
    public void onTick(MarketDataEvent event) {
        currentTickTime = event.getLastTradedTime();

        if (event.getInstrumentToken() == 256265) lastNiftyPrice = event.getLastTradedPrice();
        else if (event.getInstrumentToken() == 260105) lastBankNiftyPrice = event.getLastTradedPrice();

        if (lastNiftyPrice == 0 || lastBankNiftyPrice == 0) return;

        double currentRatio = lastBankNiftyPrice / lastNiftyPrice;
        updateHistory(currentRatio);

        if (ratioHistory.size() < LOOKBACK_PERIOD) return;

        double mean = sumRatios / ratioHistory.size();
        double stdDev = calculateStdDev(mean);
        double zScore = (stdDev < 0.000001) ? 0 : (currentRatio - mean) / stdDev;

        checkSignals(currentRatio, mean, zScore);
    }

    private void updateHistory(double val) {
        ratioHistory.add(val);
        sumRatios += val;
        if (ratioHistory.size() > LOOKBACK_PERIOD) sumRatios -= ratioHistory.poll();
    }

    private double calculateStdDev(double mean) {
        double sumSq = 0.0;
        for (Double val : ratioHistory) sumSq += Math.pow(val - mean, 2);
        return Math.sqrt(sumSq / ratioHistory.size());
    }

    private void checkSignals(double currentRatio, double mean, double zScore) {
        double divergence = currentRatio - mean;

        // --- ENTRY LOGIC (Calculate Dynamic Qty) ---
        if (currentPosition == Position.NONE) {

            // SHORT SPREAD ENTRY
            if (zScore > ENTRY_Z_SCORE && divergence > MIN_PROFIT_THRESHOLD) {
                // Calculate and STORE quantity
                heldBnQty = (int) (allocationPerLeg / lastBankNiftyPrice);
                heldNiftyQty = (int) (allocationPerLeg / lastNiftyPrice);

                log.info("ENTRY SHORT: Z={} (>2.5). Selling {} BN / Buying {} Nifty", fmt(zScore), heldBnQty, heldNiftyQty);
                execute(OrderEvent.Type.SELL, "BANKNIFTY", heldBnQty, lastBankNiftyPrice);
                execute(OrderEvent.Type.BUY, "NIFTY", heldNiftyQty, lastNiftyPrice);
                currentPosition = Position.SHORT_SPREAD;
            }

            // LONG SPREAD ENTRY
            else if (zScore < -ENTRY_Z_SCORE && divergence < -MIN_PROFIT_THRESHOLD) {
                // Calculate and STORE quantity
                heldBnQty = (int) (allocationPerLeg / lastBankNiftyPrice);
                heldNiftyQty = (int) (allocationPerLeg / lastNiftyPrice);

                log.info("ENTRY LONG: Z={} (<-2.5). Buying {} BN / Selling {} Nifty", fmt(zScore), heldBnQty, heldNiftyQty);
                execute(OrderEvent.Type.BUY, "BANKNIFTY", heldBnQty, lastBankNiftyPrice);
                execute(OrderEvent.Type.SELL, "NIFTY", heldNiftyQty, lastNiftyPrice);
                currentPosition = Position.LONG_SPREAD;
            }
        }

        // --- EXIT LOGIC (Use Stored Qty) ---
        else if (currentPosition == Position.SHORT_SPREAD) {
            if (zScore < EXIT_Z_SCORE || zScore > STOP_LOSS_Z_SCORE) {
                String type = (zScore > STOP_LOSS_Z_SCORE) ? "STOP LOSS" : "TAKE PROFIT";
                log.info("{}: Short Spread Closed. Z={}", type, fmt(zScore));

                // Use HELD Qty (Fixes Mismatch)
                execute(OrderEvent.Type.BUY, "BANKNIFTY", heldBnQty, lastBankNiftyPrice);
                execute(OrderEvent.Type.SELL, "NIFTY", heldNiftyQty, lastNiftyPrice);
                resetPosition();
            }
        }
        else if (currentPosition == Position.LONG_SPREAD) {
            if (zScore > EXIT_Z_SCORE || zScore < -STOP_LOSS_Z_SCORE) {
                String type = (zScore < -STOP_LOSS_Z_SCORE) ? "STOP LOSS" : "TAKE PROFIT";
                log.info("{}: Long Spread Closed. Z={}", type, fmt(zScore));

                // Use HELD Qty
                execute(OrderEvent.Type.SELL, "BANKNIFTY", heldBnQty, lastBankNiftyPrice);
                execute(OrderEvent.Type.BUY, "NIFTY", heldNiftyQty, lastNiftyPrice);
                resetPosition();
            }
        }
    }

    private void resetPosition() {
        currentPosition = Position.NONE;
        heldBnQty = 0;
        heldNiftyQty = 0;
    }

    private void execute(OrderEvent.Type type, String symbol, int qty, double price) {
        if (orderPublisher != null) orderPublisher.publishOrder(symbol, type, qty, price, strategyId, currentTickTime);
    }

    private String fmt(double val) { return String.format("%.4f", val); }
    public void setOrderPublisher(OrderPublisher p) { this.orderPublisher = p; }
    public String getStrategyId() { return strategyId; }
}