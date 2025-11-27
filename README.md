# Isotope - Low-Latency Java MFT Engine

Isotope is a Mid-Frequency Trading (MFT) engine built on the LMAX Disruptor architecture, designed for the Indian NSE markets (via Zerodha KiteConnect). It prioritizes low latency and high throughput by utilizing lock-free data structures and a Zero-GC memory model in the hot path.

## Architecture: The LMAX Pattern

The system is architected around the Single Writer Principle using Ring Buffers to decouple components and minimize thread contention.

### 1. Core Components
*   **Input RingBuffer (Market Data):** Handles incoming high-frequency ticks.
*   **Event Processor (Strategy Engine):** A single-threaded consumer that executes strategy logic. This ensures no lock contention on strategy state.
*   **Output RingBuffer (Order Execution):** Decouples the strategy execution from network I/O (Order Placement).
*   **Adapters:**
    *   `MarketDataAdapter`: Producer that pushes ticks to the Input RingBuffer.
    *   `OrderExecutionAdapter`: Consumer that reads from the Output RingBuffer and calls the Broker API.

### 2. Zero-GC Memory Model
*   **Object Pooling:** `MarketDataEvent` and `OrderEvent` objects are pre-allocated in the Ring Buffers at startup.
*   **Event Translation:** Incoming data is copied into these pre-allocated containers, avoiding `new` keyword usage during trading hours.
*   **Final Fields:** Extensive use of `final` and primitives to optimize CPU cache lines.

## Directory Structure

```
src/main/java/com/isotope
├── core/           # Engine setup, RingBuffer initialization
├── model/          # Events (Tick, Order) and Factories
├── strategy/       # Strategy Interfaces and Loader
├── adapter/        # I/O Adapters (Kite, Execution)
└── IsotopeEngine.java
```

## Strategy Development

Isotope supports a pluggable strategy architecture. Strategies implement the `Strategy` interface and are injected into the engine.

### Adding a New Strategy
1.  Implement `com.isotope.strategy.Strategy`.
2.  Create a folder in `strategies/<StrategyName>`.
3.  Add a `README.md` following `STRATEGY_TEMPLATE.md`.

## Build & Run

**Prerequisites:** Java 21, Maven.

```bash
mvn clean install
```

## Disclaimer
This software is for educational and research purposes. High-frequency trading involves significant financial risk. Use at your own risk.
