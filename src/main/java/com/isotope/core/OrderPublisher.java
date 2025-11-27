package com.isotope.core;

import com.isotope.model.OrderEvent;

public interface OrderPublisher {
    void publishOrder(String symbol, OrderEvent.Type type, int quantity, double price, String strategyId);
}
