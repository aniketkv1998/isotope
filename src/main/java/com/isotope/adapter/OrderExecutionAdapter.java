package com.isotope.adapter;

import com.isotope.config.AppConfig;
import com.isotope.model.OrderEvent;
import com.isotope.service.IndianFuturesFeeCalculator;
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
    private final IndianFuturesFeeCalculator feeCalculator;
    private double currentBalance;

    public OrderExecutionAdapter(AppConfig appConfig, IndianFuturesFeeCalculator feeCalculator) {
        this.feeCalculator = feeCalculator;
        this.currentBalance = appConfig.getBacktest().getInitialCapital();
        try {
            // 1. Create 'trades.csv' in the project root folder
            // 'false' means we overwrite the file every time we restart the app
            csvWriter = new BufferedWriter(new FileWriter("trades.csv", false));

            // 2. Write the Header Row so Python knows the columns
            csvWriter.write("timestamp,strategy_id,symbol,action,quantity,price,fees,net_cash_flow,running_balance\n");
            csvWriter.flush();
            log.info("Created trades.csv successfully.");
        } catch (IOException e) {
            log.error("Failed to initialize trades.csv writer", e);
        }
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.getType() == null) return;

        double turnover = event.getPrice() * event.getQuantity();
        double fees = feeCalculator.calculateTotalFee(event.getType(), event.getPrice(), event.getQuantity());
        double netCashFlow = 0.0;

        if (event.getType() == OrderEvent.Type.BUY) {
            netCashFlow = -turnover - fees;
        } else if (event.getType() == OrderEvent.Type.SELL) {
            netCashFlow = turnover - fees;
        }

        currentBalance += netCashFlow;

        try {
            // Log to Console (for you to see)
            log.info("EXECUTING: {} {} @ {}, Fees: {}, Balance: {}", event.getType(), event.getTradingSymbol(), event.getPrice(), fees, currentBalance);

            // 3. Write to CSV (for the Dashboard to see)
            if (csvWriter != null) {
                // Format: timestamp,strategy_id,symbol,action,quantity,price,fees,net_cash_flow,running_balance
                String line = String.format("%d,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                        event.getTimestamp(),
                        event.getStrategyId(),
                        event.getTradingSymbol(),
                        event.getType(),
                        event.getQuantity(),
                        event.getPrice(),
                        fees,
                        netCashFlow,
                        currentBalance
                );
                csvWriter.write(line);
                csvWriter.flush(); // Force save to disk immediately
            }
        } catch (IOException e) {
            log.error("Failed to write to trades.csv", e);
        }
    }
}