package com.isotope.adapter;

import com.isotope.model.OrderEvent;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Consumes OrderEvents and writes them to a CSV file for the Dashboard.
 */
@Slf4j
public class OrderExecutionAdapter implements EventHandler<OrderEvent> {

    private BufferedWriter csvWriter;

    public OrderExecutionAdapter() {
        try {
            // 1. Create 'trades.csv' in the project root folder
            // 'false' means we overwrite the file every time we restart the app
            csvWriter = new BufferedWriter(new FileWriter("trades.csv", false));

            // 2. Write the Header Row so Python knows the columns
            csvWriter.write("timestamp,strategy_id,symbol,action,quantity,price\n");
            csvWriter.flush();
            log.info("Created trades.csv successfully.");
        } catch (IOException e) {
            log.error("Failed to initialize trades.csv writer", e);
        }
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.getType() == null) return;

        try {
            // Log to Console (for you to see)
            log.info("EXECUTING: {} {} @ {}", event.getType(), event.getTradingSymbol(), event.getPrice());

            // 3. Write to CSV (for the Dashboard to see)
            if (csvWriter != null) {
                // Format: 1698421000,PairsStrategy,NIFTY,BUY,50,19500.00
                String line = String.format("%d,%s,%s,%s,%d,%.2f%n",
                        event.getTimestamp(),
                        event.getStrategyId(),
                        event.getTradingSymbol(),
                        event.getType(),
                        event.getQuantity(),
                        event.getPrice()
                );
                csvWriter.write(line);
                csvWriter.flush(); // Force save to disk immediately
            }
        } catch (IOException e) {
            log.error("Failed to write to trades.csv", e);
        }
    }
}