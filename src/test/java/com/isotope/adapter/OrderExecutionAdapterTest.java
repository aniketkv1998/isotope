package com.isotope.adapter;

import com.isotope.model.OrderEvent;
import com.isotope.service.IndianDerivativesFeeCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class OrderExecutionAdapterTest {

    private OrderExecutionAdapter adapter;
    private IndianDerivativesFeeCalculator feeCalculator;
    private static final String CSV_FILE = "trades.csv";

    @BeforeEach
    void setUp() {
        feeCalculator = new IndianDerivativesFeeCalculator();
    }

    @AfterEach
    void tearDown() {
        File file = new File(CSV_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testFuturesExecution() throws IOException {
        adapter = new OrderExecutionAdapter(feeCalculator, 1000000.0, "FUTURES");

        OrderEvent buyEvent = new OrderEvent();
        buyEvent.setType(OrderEvent.Type.BUY);
        buyEvent.setPrice(100.0);
        buyEvent.setQuantity(100);
        buyEvent.setTradingSymbol("TEST_FUT");
        buyEvent.setTimestamp(System.currentTimeMillis());
        buyEvent.setStrategyId("TEST");

        adapter.onEvent(buyEvent, 1, true);

        // Calculate expected fee for Futures
        double expectedFee = feeCalculator.calculateTotalFee(OrderEvent.Type.BUY, 100.0, 100);

        // Check CSV output
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            String header = reader.readLine();
            assertNotNull(header);
            String line = reader.readLine();
            assertNotNull(line);
            String[] parts = line.split(",");
            // "timestamp,strategy_id,symbol,action,quantity,price,fees,net_cash_flow,running_balance,fee_saving"
            // fees is index 6
            double fees = Double.parseDouble(parts[6]);
            assertEquals(expectedFee, fees, 0.01);

            // fee_saving is index 9
            double feeSaving = Double.parseDouble(parts[9]);
            assertEquals(0.0, feeSaving, 0.01); // No saving in FUTURES mode
        }
    }

    @Test
    void testSyntheticExecution_Buy() throws IOException {
        // For BUY, Futures is usually cheaper because STT is 0 on Futures Buy,
        // but Synthetic Buy involves Sell Put which attracts STT.
        adapter = new OrderExecutionAdapter(feeCalculator, 1000000.0, "SYNTHETIC");

        OrderEvent buyEvent = new OrderEvent();
        buyEvent.setType(OrderEvent.Type.BUY);
        buyEvent.setPrice(100.0);
        buyEvent.setQuantity(100);
        buyEvent.setTradingSymbol("TEST_SYN");
        buyEvent.setTimestamp(System.currentTimeMillis());
        buyEvent.setStrategyId("TEST");

        adapter.onEvent(buyEvent, 1, true);

        // Calculate expected fee for Synthetic
        // Price = 100. Premium = 0.85
        double premium = 100.0 * 0.0085;
        // Long Synthetic = Buy Call + Sell Put
        double callFee = feeCalculator.calculateOptionFee(OrderEvent.Type.BUY, premium, 100);
        double putFee = feeCalculator.calculateOptionFee(OrderEvent.Type.SELL, premium, 100);
        double expectedFee = callFee + putFee;

        // Calculate expected Futures Fee for comparison
        double futuresFee = feeCalculator.calculateTotalFee(OrderEvent.Type.BUY, 100.0, 100);
        double expectedSaving = futuresFee - expectedFee;

        // Check CSV output
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            String header = reader.readLine();
            assertNotNull(header);
            String line = reader.readLine();
            assertNotNull(line);
            String[] parts = line.split(",");

            double fees = Double.parseDouble(parts[6]);
            assertEquals(expectedFee, fees, 0.01);

            double feeSaving = Double.parseDouble(parts[9]);
            assertEquals(expectedSaving, feeSaving, 0.01);

            // We don't assert feeSaving > 0 here because for small BUY orders, Synthetic might be more expensive.
        }
    }

    @Test
    void testSyntheticExecution_Sell_Savings() throws IOException {
        // For SELL, Futures pays 0.02% STT.
        // Synthetic Sell (Bearish) = Buy Put + Sell Call. Sell Call pays 0.1% on Premium.
        // Premium is ~0.85% of Price. STT = 0.1% * 0.85% * Price = 0.00085% of Price.
        // Futures STT = 0.02% of Price.
        // So Synthetic STT is much lower.
        // If amount is large enough to cover the extra Brokerage (Synthetic pays 2 legs brokerage),
        // Savings should be positive.

        adapter = new OrderExecutionAdapter(feeCalculator, 1000000.0, "SYNTHETIC");

        double price = 20000.0;
        int quantity = 50;
        // Turnover = 10,00,000.
        // Futures STT = 200.
        // Synthetic STT = 1000000 * 0.0085 * 0.001 = 8.5.
        // Saving on STT = 191.5.
        // Extra Brokerage = 20 (Futures 20, Synthetic 40).
        // Net Saving should be positive.

        OrderEvent sellEvent = new OrderEvent();
        sellEvent.setType(OrderEvent.Type.SELL);
        sellEvent.setPrice(price);
        sellEvent.setQuantity(quantity);
        sellEvent.setTradingSymbol("TEST_SYN_SELL");
        sellEvent.setTimestamp(System.currentTimeMillis());
        sellEvent.setStrategyId("TEST");

        adapter.onEvent(sellEvent, 1, true);

        // Check CSV output
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            String header = reader.readLine();
            String line = reader.readLine();
            String[] parts = line.split(",");

            double feeSaving = Double.parseDouble(parts[9]);
            assertTrue(feeSaving > 0, "Synthetic SELL execution should save fees for large turnover");
        }
    }
}
