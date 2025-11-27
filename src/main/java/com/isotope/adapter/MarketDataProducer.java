package com.isotope.adapter;

import com.isotope.model.MarketDataEvent;
import com.lmax.disruptor.RingBuffer;

public interface MarketDataProducer {
    void connect();
    void subscribe(String... symbols);
    void startPublishing(RingBuffer<MarketDataEvent> ringBuffer);
}
