package com.isotope.service;

import com.isotope.model.OrderEvent;
import org.springframework.stereotype.Service;

@Service
public class IndianFuturesFeeCalculator {

    // Fee structure constants for NSE Futures
    private static final double BROKERAGE_PER_ORDER = 20.0;
    private static final double STT_RATE = 0.0002; // 0.02% (Sell side only)
    private static final double TXN_CHARGE_RATE = 0.000019; // 0.0019%
    private static final double GST_RATE = 0.18; // 18% on (Brokerage + Txn Charges)
    private static final double SEBI_CHARGE_RATE = 0.000001; // 10 per crore = 0.0001% -> Wait, 10 / 10,000,000 = 1/1,000,000 = 0.000001. User said 0.0001% which is 1000 per crore?
    // User instruction: "SEBI Charges: ₹10 per crore (0.0001% of turnover)."
    // Let's verify the percentage.
    // 1 Crore = 10,000,000
    // 10 / 10,000,000 = 0.000001
    // 0.0001% = 0.0001 / 100 = 0.000001
    // So 0.000001 is correct multiplier.

    private static final double STAMP_DUTY_RATE = 0.00002; // 0.002% (Buy side only)

    /**
     * Calculates the total fee for a given order execution.
     *
     * @param type     Order type (BUY/SELL)
     * @param price    Execution price
     * @param quantity Executed quantity
     * @return Total fee in INR
     */
    public double calculateTotalFee(OrderEvent.Type type, double price, int quantity) {
        if (price <= 0 || quantity <= 0) {
            return 0.0;
        }

        double turnover = price * quantity;

        // 1. Brokerage: Flat ₹20 per order.
        double brokerage = BROKERAGE_PER_ORDER;

        // 2. STT: 0.02% of turnover (Sell side only).
        double stt = 0.0;
        if (type == OrderEvent.Type.SELL) {
            stt = turnover * STT_RATE;
        }

        // 3. Exchange Transaction Charges: 0.0019% of turnover.
        double txnCharges = turnover * TXN_CHARGE_RATE;

        // 4. GST: 18% on (Brokerage + Transaction Charges).
        double gst = (brokerage + txnCharges) * GST_RATE;

        // 5. SEBI Charges: ₹10 per crore (0.0001% of turnover).
        double sebiCharges = turnover * SEBI_CHARGE_RATE;

        // 6. Stamp Duty: 0.002% of turnover (Buy side only).
        double stampDuty = 0.0;
        if (type == OrderEvent.Type.BUY) {
            stampDuty = turnover * STAMP_DUTY_RATE;
        }

        // Total Fee
        double totalFee = brokerage + stt + txnCharges + gst + sebiCharges + stampDuty;

        return totalFee;
    }
}
