package com.isotope.strategy;

import com.isotope.core.OrderPublisher;
import com.isotope.model.MarketDataEvent;
import com.isotope.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Example Strategy: Pairs Trading (Nifty vs BankNifty).
 * Logic: Simple correlation check (Mocked).
 */
@Slf4j
public class PairsTradingStrategy implements Strategy {

    private OrderPublisher orderPublisher;
    private final String strategyId = "PairsTrading_Nifty_BankNifty";

    // State
    private double lastNiftyPrice = 0.0;
    private double lastBankNiftyPrice = 0.0;
    private final double THRESHOLD_RATIO = 2.5; // Example ratio

    @Override
    public void onTick(MarketDataEvent event) {
        // Assume instrument tokens are known (Mock values)
        long NIFTY_TOKEN = 256265;
        long BANKNIFTY_TOKEN = 260105;

        if (event.getInstrumentToken() == NIFTY_TOKEN) {
            lastNiftyPrice = event.getLastTradedPrice();
        } else if (event.getInstrumentToken() == BANKNIFTY_TOKEN) {
            lastBankNiftyPrice = event.getLastTradedPrice();
        }

        if (lastNiftyPrice > 0 && lastBankNiftyPrice > 0) {
            double ratio = lastBankNiftyPrice / lastNiftyPrice;

            // Simple Mean Reversion Logic (Mock)
            if (ratio > THRESHOLD_RATIO + 0.1) {
                // BankNifty expensive, Nifty cheap -> Sell BankNifty, Buy Nifty
                log.info("Ratio {} > Threshold. Signal SELL BankNifty, BUY Nifty", ratio);

                // Fire and forget (Output RingBuffer)
                if (orderPublisher != null) {
                    orderPublisher.publishOrder("BANKNIFTY", OrderEvent.Type.SELL, 25, lastBankNiftyPrice, strategyId);
                    orderPublisher.publishOrder("NIFTY", OrderEvent.Type.BUY, 50, lastNiftyPrice, strategyId);
                }

                // Reset to avoid spamming orders in this example
                lastNiftyPrice = 0;
            }
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
