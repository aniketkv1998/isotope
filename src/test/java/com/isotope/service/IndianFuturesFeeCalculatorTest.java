package com.isotope.service;

import com.isotope.model.OrderEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndianFuturesFeeCalculatorTest {

    private final IndianFuturesFeeCalculator calculator = new IndianFuturesFeeCalculator();

    @Test
    public void testCalculateFee_Buy() {
        // Buy 50 Qty @ 19500
        double price = 19500.0;
        int quantity = 50;
        double turnover = price * quantity; // 9,75,000

        // Brokerage: 20
        // STT: 0 (Buy)
        // Txn Charge: 9,75,000 * 0.000019 = 18.525
        // GST: 18% of (20 + 18.525) = 18% of 38.525 = 6.9345
        // SEBI: 9,75,000 * 0.000001 = 0.975
        // Stamp Duty: 9,75,000 * 0.00002 = 19.5 (Buy only)

        // Total: 20 + 0 + 18.525 + 6.9345 + 0.975 + 19.5 = 65.9345

        double fee = calculator.calculateTotalFee(OrderEvent.Type.BUY, price, quantity);
        assertEquals(65.9345, fee, 0.0001);
    }

    @Test
    public void testCalculateFee_Sell() {
        // Sell 50 Qty @ 19600
        double price = 19600.0;
        int quantity = 50;
        double turnover = price * quantity; // 9,80,000

        // Brokerage: 20
        // STT: 9,80,000 * 0.0002 = 196 (Sell only)
        // Txn Charge: 9,80,000 * 0.000019 = 18.62
        // GST: 18% of (20 + 18.62) = 18% of 38.62 = 6.9516
        // SEBI: 9,80,000 * 0.000001 = 0.98
        // Stamp Duty: 0 (Sell)

        // Total: 20 + 196 + 18.62 + 6.9516 + 0.98 + 0 = 242.5516

        double fee = calculator.calculateTotalFee(OrderEvent.Type.SELL, price, quantity);
        assertEquals(242.5516, fee, 0.0001);
    }
}
