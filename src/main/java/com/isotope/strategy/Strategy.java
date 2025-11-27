package com.isotope.strategy;

import com.isotope.core.OrderPublisher;
import com.isotope.model.MarketDataEvent;

public interface Strategy {
    /**
     * Called when a new tick is received.
     * The strategy should process the tick and decide whether to place an order.
     * This method runs in the hot path (MarketData Processor thread).
     */
    void onTick(MarketDataEvent event);

    /**
     * Injects the OrderPublisher so the strategy can send orders to the Output RingBuffer.
     */
    void setOrderPublisher(OrderPublisher orderPublisher);

    /**
     * Unique identifier for the strategy.
     */
    String getStrategyId();
}
