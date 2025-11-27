package com.isotope.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents an order execution signal.
 * Designed for Object Pooling.
 */
@Getter
@Setter
@ToString
public class OrderEvent {
    public enum Type {
        BUY, SELL, CANCEL, MODIFY
    }

    private long instrumentToken;
    private String tradingSymbol;
    private Type type;
    private int quantity;
    private double price;
    private String strategyId;
    private long timestamp;

    public void clear() {
        this.instrumentToken = 0;
        this.tradingSymbol = null;
        this.type = null;
        this.quantity = 0;
        this.price = 0.0;
        this.strategyId = null;
        this.timestamp = 0;
    }
}
