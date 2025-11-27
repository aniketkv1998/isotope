package com.isotope.adapter;

import com.isotope.model.OrderEvent;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

/**
 * Consumes OrderEvents from the Output RingBuffer and executes them via KiteConnect.
 * Separates Strategy Logic from Network I/O.
 */
@Slf4j
public class OrderExecutionAdapter implements EventHandler<OrderEvent> {

    // In a real implementation, this would hold the KiteConnect instance
    // private final KiteConnect kiteConnect;
    private static final String TRADES_FILE = "trades.csv";

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

            logTrade(event);

            // Real implementation:
            // OrderParams orderParams = new OrderParams();
            // orderParams.tradingsymbol = event.getTradingSymbol();
            // ...
            // kiteConnect.placeOrder(orderParams, Constants.VARIETY_REGULAR);

        } catch (Exception e) {
            log.error("Failed to execute order", e);
        }
    }

    private void logTrade(OrderEvent event) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRADES_FILE, true))) {
            // Check if file is empty to write header (simple check: if length is 0)
            java.io.File file = new java.io.File(TRADES_FILE);
            if (file.length() == 0) {
                writer.write("timestamp,strategy_id,symbol,action,quantity,price");
                writer.newLine();
            }

            String timestamp = Instant.now().toString();
            String record = String.format("%s,%s,%s,%s,%d,%f",
                    timestamp,
                    event.getStrategyId(),
                    event.getTradingSymbol(),
                    event.getType(),
                    event.getQuantity(),
                    event.getPrice());

            writer.write(record);
            writer.newLine();
        } catch (IOException e) {
            log.error("Failed to write trade to CSV", e);
        }
    }
}
