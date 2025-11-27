package com.isotope.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a market tick event.
 * Designed for Object Pooling: Mutable fields are updated in place.
 */
@Getter
@Setter
@ToString
public class MarketDataEvent {
    private long instrumentToken;
    private double lastTradedPrice;
    private long volume;
    private long lastTradedTime;
    private double bidPrice;
    private double askPrice;
    private long bidQuantity;
    private long askQuantity;

    // Reset method for object pooling cleanliness (optional but good practice)
    public void clear() {
        this.instrumentToken = 0;
        this.lastTradedPrice = 0.0;
        this.volume = 0;
        this.lastTradedTime = 0;
        this.bidPrice = 0.0;
        this.askPrice = 0.0;
        this.bidQuantity = 0;
        this.askQuantity = 0;
    }
}
