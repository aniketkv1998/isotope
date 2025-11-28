package com.isotope.adapter;

import com.isotope.model.OrderEvent;
import com.isotope.service.IndianDerivativesFeeCalculator;
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
    private final IndianDerivativesFeeCalculator feeCalculator;
    private double runningBalance;
    private final String executionMode;

    // Position Tracking: Symbol -> Average Entry Price
    private final Map<String, Double> entryPrices = new HashMap<>();
    // Position Tracking: Symbol -> Net Quantity (+Long, -Short)
    private final Map<String, Integer> positions = new HashMap<>();

    public OrderExecutionAdapter(IndianDerivativesFeeCalculator feeCalculator, double initialCapital, String executionMode) {
        this.feeCalculator = feeCalculator;
        this.runningBalance = initialCapital;
        this.executionMode = executionMode;

        try {
            csvWriter = new BufferedWriter(new FileWriter("trades.csv", false));
            csvWriter.write("timestamp,strategy_id,symbol,action,quantity,price,fees,net_cash_flow,running_balance,fee_saving\n");
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

            double fees;
            double feeSaving = 0.0;

            if ("SYNTHETIC".equalsIgnoreCase(executionMode)) {
                // Estimate ATM Premium: Price * 0.0085
                double estimatedPremium = price * 0.0085;

                // Calculate Fees for Leg 1 (Call) + Fees for Leg 2 (Put)
                // Long Synthetic (Bullish): Buy Call + Sell Put
                // Short Synthetic (Bearish): Buy Put + Sell Call

                double callFee;
                double putFee;

                if (event.getType() == OrderEvent.Type.BUY) {
                    // Long Synthetic: Buy Call + Sell Put
                    callFee = feeCalculator.calculateOptionFee(OrderEvent.Type.BUY, estimatedPremium, quantity);
                    putFee = feeCalculator.calculateOptionFee(OrderEvent.Type.SELL, estimatedPremium, quantity);
                } else {
                    // Short Synthetic: Buy Put + Sell Call
                    // Correction based on instructions: "Short Synthetic (Bearish): Buy Put + Sell Call"
                    // Typically Short Synthetic Futures is Long Put + Short Call
                    putFee = feeCalculator.calculateOptionFee(OrderEvent.Type.BUY, estimatedPremium, quantity);
                    callFee = feeCalculator.calculateOptionFee(OrderEvent.Type.SELL, estimatedPremium, quantity);
                }

                fees = callFee + putFee;

                // Calculate Futures Fee for comparison
                double futuresFee = feeCalculator.calculateTotalFee(event.getType(), price, quantity);
                feeSaving = futuresFee - fees;

            } else {
                // FUTURES
                fees = feeCalculator.calculateTotalFee(event.getType(), price, quantity);
            }

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
                csvWriter.write(String.format("%d,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                        event.getTimestamp(),
                        event.getStrategyId(), symbol, event.getType(), quantity, price, fees, netCashFlow, runningBalance, feeSaving));
                csvWriter.flush();
            }

        } catch (Exception e) {
            log.error("Trade processing error", e);
        }
    }
}
