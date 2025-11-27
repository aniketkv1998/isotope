package com.isotope.adapter;

import com.isotope.model.OrderEvent;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes OrderEvents from the Output RingBuffer and executes them via KiteConnect.
 * Separates Strategy Logic from Network I/O.
 */
@Slf4j
public class OrderExecutionAdapter implements EventHandler<OrderEvent> {

    // In a real implementation, this would hold the KiteConnect instance
    // private final KiteConnect kiteConnect;

    public OrderExecutionAdapter() {
        // this.kiteConnect = kiteConnect;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.getType() == null) {
            log.warn("Received empty or invalid order event at sequence {}", sequence);
            return;
        }

        try {
            // Simulate network latency or API call
            log.info("EXECUTING ORDER: [{}] {} {} @ {} for Strategy: {}",
                    event.getType(), event.getQuantity(), event.getTradingSymbol(), event.getPrice(), event.getStrategyId());

            // Real implementation:
            // OrderParams orderParams = new OrderParams();
            // orderParams.tradingsymbol = event.getTradingSymbol();
            // ...
            // kiteConnect.placeOrder(orderParams, Constants.VARIETY_REGULAR);

        } catch (Exception e) {
            log.error("Failed to execute order", e);
        }
    }
}
