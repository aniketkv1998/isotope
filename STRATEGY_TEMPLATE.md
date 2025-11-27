# Strategy Documentation Template

## Overview
**Strategy Name:** [Name of the Strategy]
**Author:** [Author Name]
**Date:** [Date]
**Description:** A high-level summary of what the strategy does.

## Mathematical Logic
Describe the quantitative model or logic used.
*   **Model:** (e.g., Cointegration, Z-Score, Moving Average Crossover)
*   **Formula:**
    ```math
    Z = (Price - Mean) / StdDev
    ```
*   **Entry Condition:** When Z-Score > 2.0
*   **Exit Condition:** When Z-Score < 0.5

## Parameters
List all configurable parameters.

| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `lookback_period` | Integer | 20 | Number of ticks/bars to calculate mean. |
| `threshold` | Double | 2.5 | Signal trigger threshold. |
| `stop_loss` | Double | 1.0% | Max loss per trade. |

## Risk Management
Specific risk rules applied to this strategy.
*   **Max Position Size:** ...
*   **Max Daily Loss:** ...
*   **Hedging:** (e.g., Always pairs long/short)

## Implementation Details
*   **Class Name:** `com.isotope.strategy.YourStrategy`
*   **Dependencies:** List any external libs or data sources.

## Backtest Results (Optional)
Summary of performance on historical data.
