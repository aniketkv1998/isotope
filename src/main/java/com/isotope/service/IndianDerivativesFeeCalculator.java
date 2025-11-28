package com.isotope.service;

import com.isotope.model.OrderEvent;
import org.springframework.stereotype.Service;

@Service
public class IndianDerivativesFeeCalculator {

    // Futures Rates (Oct 2024)
    private static final double FUT_BROKERAGE_MAX = 20.0;
    private static final double FUT_STT_RATE = 0.0002; // 0.02% Sell
    private static final double FUT_NSE_TXN_RATE = 0.0000173; // 0.00173%
    private static final double FUT_SEBI_RATE = 0.000001; // ₹10/crore
    private static final double FUT_IPFT_RATE = 0.000001; // ₹10/crore
    private static final double FUT_STAMP_DUTY_RATE = 0.00002; // 0.002% Buy
    private static final double FUT_GST_RATE = 0.18;

    // Options Rates
    private static final double OPT_BROKERAGE_FLAT = 20.0;
    private static final double OPT_STT_RATE = 0.001; // 0.1% on Sell Premium
    private static final double OPT_NSE_TXN_RATE = 0.0003503; // 0.03503% on Premium
    private static final double OPT_SEBI_RATE = 0.000001; // ₹10/crore on Premium
    private static final double OPT_IPFT_RATE = 0.000001; // ₹10/crore on Premium
    private static final double OPT_STAMP_DUTY_RATE = 0.00003; // 0.003% Buy Premium
    private static final double OPT_GST_RATE = 0.18;

    /**
     * Calculates fees for FUTURES contracts.
     */
    public double calculateTotalFee(OrderEvent.Type type, double price, int quantity) {
        if (price <= 0 || quantity <= 0) return 0.0;

        double turnover = price * quantity;

        // 1. Brokerage (0.03% or ₹20, whichever is lower)
        double brokerage = Math.min(turnover * 0.0003, FUT_BROKERAGE_MAX);

        // 2. Regulatory Charges
        double txnCharges = turnover * FUT_NSE_TXN_RATE;
        double sebiCharges = turnover * FUT_SEBI_RATE;
        double ipftCharges = turnover * FUT_IPFT_RATE;

        double stt = (type == OrderEvent.Type.SELL) ? (turnover * FUT_STT_RATE) : 0.0;
        double stampDuty = (type == OrderEvent.Type.BUY) ? (turnover * FUT_STAMP_DUTY_RATE) : 0.0;

        // 3. GST (on Service Fees only)
        double gst = (brokerage + txnCharges + sebiCharges) * FUT_GST_RATE;

        return brokerage + stt + txnCharges + sebiCharges + ipftCharges + stampDuty + gst;
    }

    /**
     * Calculates fees for OPTION contracts.
     * Note: Brokerage is flat ₹20, no 0.03% rule.
     */
    public double calculateOptionFee(OrderEvent.Type type, double premium, int quantity) {
        if (premium <= 0 || quantity <= 0) return 0.0;

        double turnover = premium * quantity;

        // 1. Brokerage (Flat ₹20 per order)
        double brokerage = OPT_BROKERAGE_FLAT;

        // 2. Regulatory Charges (On Premium Turnover)
        double txnCharges = turnover * OPT_NSE_TXN_RATE;
        double sebiCharges = turnover * OPT_SEBI_RATE;
        double ipftCharges = turnover * OPT_IPFT_RATE;

        // STT is on Sell Premium only
        double stt = (type == OrderEvent.Type.SELL) ? (turnover * OPT_STT_RATE) : 0.0;

        // Stamp Duty is on Buy Premium only
        double stampDuty = (type == OrderEvent.Type.BUY) ? (turnover * OPT_STAMP_DUTY_RATE) : 0.0;

        // 3. GST (on Brokerage + Exchange Txn only per instructions "GST: 18% on (Brokerage + Txn)")
        // Typically SEBI/IPFT are also included but adhering to specific instructions if given.
        // Instructions say: "GST: 18% on (Brokerage + Txn)."
        // However, standard market practice usually includes SEBI charges in GST base.
        // Let's re-read carefully: "GST: 18% on (Brokerage + Txn)."
        // I will follow the explicit instruction, but usually it's on taxable value.
        // Re-reading "SEBI/IPFT: Standard rates on Premium."
        // I will follow the instruction: GST on (Brokerage + Txn).

        double gst = (brokerage + txnCharges) * OPT_GST_RATE;

        return brokerage + stt + txnCharges + sebiCharges + ipftCharges + stampDuty + gst;
    }
}
