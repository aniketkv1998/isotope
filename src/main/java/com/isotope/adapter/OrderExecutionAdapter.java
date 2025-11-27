package com.isotope.adapter;

import com.isotope.model.OrderEvent;
import com.isotope.service.IndianFuturesFeeCalculator;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OrderExecutionAdapter implements EventHandler<OrderEvent> {

    private BufferedWriter csvWriter;
    private final IndianFuturesFeeCalculator feeCalculator;
    private double runningBalance; // Comes from Config now

    // Position Tracking: Symbol -> Average Entry Price
    private final Map<String, Double> entryPrices = new HashMap<>();
    // Position Tracking: Symbol -> Net Quantity (+Long, -Short)
    private final Map<String, Integer> positions = new HashMap<>();

    public OrderExecutionAdapter(IndianFuturesFeeCalculator feeCalculator, double initialCapital) {
        this.feeCalculator = feeCalculator;
        this.runningBalance = initialCapital;

        try {
            csvWriter = new BufferedWriter(new FileWriter("trades.csv", false));
            csvWriter.write("timestamp,strategy_id,symbol,action,quantity,price,fees,net_cash_flow,running_balance\n");
            csvWriter.flush();
        } catch (IOException e) {
            log.error("Failed to init CSV", e);
        }
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.getType() == null) return;

        try {
            double price = event.getPrice();
            int quantity = event.getQuantity();
            String symbol = event.getTradingSymbol();

            // 1. Fees (Always Paid)
            double fees = feeCalculator.calculateTotalFee(event.getType(), price, quantity);
            double netCashFlow = -fees;

            // 2. PnL Logic (Realized only on Closing)
            // Determine signed quantity (+ for Buy, - for Sell)
            int tradeQty = (event.getType() == OrderEvent.Type.BUY) ? quantity : -quantity;
            int currentPos = positions.getOrDefault(symbol, 0);
            double currentEntry = entryPrices.getOrDefault(symbol, 0.0);

            // Check if this trade reduces our position (Closing)
            boolean isClosing = (currentPos > 0 && tradeQty < 0) || (currentPos < 0 && tradeQty > 0);

            if (isClosing) {
                // We are realizing profit/loss
                int qtyClosing = Math.min(Math.abs(currentPos), Math.abs(tradeQty));
                double pnl = 0.0;

                if (currentPos > 0) { // Long Closing
                    pnl = (price - currentEntry) * qtyClosing;
                } else { // Short Covering
                    pnl = (currentEntry - price) * qtyClosing;
                }

                netCashFlow += pnl; // Add Profit (or subtract Loss)

                // Update Position State
                int newPos = currentPos + tradeQty;
                positions.put(symbol, newPos);
                if (newPos == 0) entryPrices.remove(symbol);

            } else {
                // Opening new position (No Cash Flow impact except Fees)
                // Update Weighted Average Price
                double totalVal = (Math.abs(currentPos) * currentEntry) + (Math.abs(tradeQty) * price);
                int newTotalQty = Math.abs(currentPos) + Math.abs(tradeQty);
                entryPrices.put(symbol, totalVal / newTotalQty);
                positions.put(symbol, currentPos + tradeQty);
            }

            // 3. Update Balance
            runningBalance += netCashFlow;

            // 4. Log
            if (csvWriter != null) {
                csvWriter.write(String.format("%d,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                        event.getTimestamp(), // Uses CSV Timestamp
                        event.getStrategyId(), symbol, event.getType(), quantity, price, fees, netCashFlow, runningBalance));
                csvWriter.flush();
            }

        } catch (Exception e) {
            log.error("Trade processing error", e);
        }
    }
}