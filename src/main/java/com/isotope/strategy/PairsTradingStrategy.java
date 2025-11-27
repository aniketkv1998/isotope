package com.isotope.strategy;

import com.isotope.core.OrderPublisher;
import com.isotope.model.MarketDataEvent;
import com.isotope.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * FIXED Strategy: Pairs Trading (Nifty vs BankNifty).
 * - Removed "Amnesia" bug (Price reset).
 * - Added "Short" side logic.
 * - Tightened thresholds for MFT frequency.
 */
@Slf4j
public class PairsTradingStrategy implements Strategy {

    private OrderPublisher orderPublisher;
    private final String strategyId = "PairsTrading_Nifty_BankNifty";

    // State
    private double lastNiftyPrice = 0.0;
    private double lastBankNiftyPrice = 0.0;

    // Logic Parameters
    private final double MEAN_RATIO = 2.45; // Approx historical mean
    private final double THRESHOLD = 0.005; // 0.5% deviation (Much tighter than 0.1)

    // Position State to prevent spamming the SAME order 100 times/sec
    private boolean isLongSpread = false;
    private boolean isShortSpread = false;

    @Override
    public void onTick(MarketDataEvent event) {
        // 1. Update Prices
        if (event.getInstrumentToken() == 256265) { // Nifty
            lastNiftyPrice = event.getLastTradedPrice();
        } else if (event.getInstrumentToken() == 260105) { // BankNifty
            lastBankNiftyPrice = event.getLastTradedPrice();
        }

        // 2. Only trade if we have BOTH prices
        if (lastNiftyPrice > 0 && lastBankNiftyPrice > 0) {
            double currentRatio = lastBankNiftyPrice / lastNiftyPrice;

            // Log ratio occasionally for debugging (optional)
            // log.debug("Ratio: {}", currentRatio);

            // --- SIGNAL 1: Spread is too HIGH (BankNifty Expensive) ---
            // Sell BankNifty, Buy Nifty -> Expect Ratio to go DOWN
            if (currentRatio > MEAN_RATIO + THRESHOLD) {
                if (!isShortSpread) {
                    log.info("ENTRY SHORT SPREAD: Ratio {} > Limit. SELL BN / BUY NIFTY", String.format("%.4f", currentRatio));

                    if (orderPublisher != null) {
                        // In MFT, we size by Notional Value (e.g. 10 Lakhs each side)
                        // For test: 25 BN (~10L) vs 50 Nifty (~10L)
                        orderPublisher.publishOrder("BANKNIFTY", OrderEvent.Type.SELL, 25, lastBankNiftyPrice, strategyId);
                        orderPublisher.publishOrder("NIFTY", OrderEvent.Type.BUY, 50, lastNiftyPrice, strategyId);
                    }

                    isShortSpread = true;  // Mark position open
                    isLongSpread = false;  // Flip position if we were long
                }
            }

            // --- SIGNAL 2: Spread is too LOW (BankNifty Cheap) ---
            // Buy BankNifty, Sell Nifty -> Expect Ratio to go UP
            else if (currentRatio < MEAN_RATIO - THRESHOLD) {
                if (!isLongSpread) {
                    log.info("ENTRY LONG SPREAD: Ratio {} < Limit. BUY BN / SELL NIFTY", String.format("%.4f", currentRatio));

                    if (orderPublisher != null) {
                        orderPublisher.publishOrder("BANKNIFTY", OrderEvent.Type.BUY, 25, lastBankNiftyPrice, strategyId);
                        orderPublisher.publishOrder("NIFTY", OrderEvent.Type.SELL, 50, lastNiftyPrice, strategyId);
                    }

                    isLongSpread = true;   // Mark position open
                    isShortSpread = false; // Flip position if we were short
                }
            }

            // --- SIGNAL 3: Mean Reversion (Take Profit) ---
            // If ratio comes back to normal, close everything (Optional)
            // Ideally, MFT strategies flip Long <-> Short rather than going flat to save fees.
        }
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