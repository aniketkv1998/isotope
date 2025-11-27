package com.isotope.service;

import com.isotope.model.OrderEvent;
import org.springframework.stereotype.Service;

@Service
public class IndianFuturesFeeCalculator {

    // Oct 2024 Revised Rates
    private static final double BROKERAGE_MAX = 20.0;
    private static final double STT_RATE = 0.0002; // 0.02% Sell
    private static final double NSE_TXN_RATE = 0.0000173; // 0.00173%
    private static final double SEBI_RATE = 0.000001; // ₹10/crore
    private static final double IPFT_RATE = 0.000001; // ₹10/crore
    private static final double STAMP_DUTY_RATE = 0.00002; // 0.002% Buy
    private static final double GST_RATE = 0.18;

    public double calculateTotalFee(OrderEvent.Type type, double price, int quantity) {
        if (price <= 0 || quantity <= 0) return 0.0;

        double turnover = price * quantity;

        // 1. Brokerage (0.03% or ₹20, whichever is lower)
        double brokerage = Math.min(turnover * 0.0003, BROKERAGE_MAX);

        // 2. Regulatory Charges
        double txnCharges = turnover * NSE_TXN_RATE;
        double sebiCharges = turnover * SEBI_RATE;
        double ipftCharges = turnover * IPFT_RATE;

        double stt = (type == OrderEvent.Type.SELL) ? (turnover * STT_RATE) : 0.0;
        double stampDuty = (type == OrderEvent.Type.BUY) ? (turnover * STAMP_DUTY_RATE) : 0.0;

        // 3. GST (on Service Fees only)
        double gst = (brokerage + txnCharges + sebiCharges) * GST_RATE;

        return brokerage + stt + txnCharges + sebiCharges + ipftCharges + stampDuty + gst;
    }
}