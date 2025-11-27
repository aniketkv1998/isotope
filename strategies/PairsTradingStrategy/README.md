# Pairs Trading Strategy (Nifty vs BankNifty)

## Overview
**Strategy Name:** PairsTrading_Nifty_BankNifty
**Type:** Statistical Arbitrage / Mean Reversion
**Description:** This strategy monitors the price ratio between Nifty 50 and Bank Nifty indices. It assumes a historical correlation and trades when the ratio diverges significantly from the mean, expecting a reversion.

## Mathematical Logic
*   **Ratio:** $R = Price(BankNifty) / Price(Nifty)$
*   **Logic:**
    *   We track the spread/ratio between the two assets.
    *   If $R > Threshold$, BankNifty is overvalued relative to Nifty. -> **SELL BankNifty / BUY Nifty**.
    *   If $R < Threshold$, BankNifty is undervalued. -> **BUY BankNifty / SELL Nifty**.

## Parameters

| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `THRESHOLD_RATIO` | Double | 2.5 | The pivot ratio. Divergence above this + 0.1 triggers a trade. |
| `Qty Nifty` | Int | 50 | Lot size for Nifty. |
| `Qty BankNifty` | Int | 25 | Lot size for BankNifty. |

## Risk Management
*   **Atomic Execution:** Orders are sent almost simultaneously via the Order RingBuffer.
*   **State Reset:** In this simplified version, state resets after a signal to prevent cascading orders.

## Implementation
*   **Class:** `com.isotope.strategy.PairsTradingStrategy`
*   **Latency:** Zero-GC hot path in `onTick`.
