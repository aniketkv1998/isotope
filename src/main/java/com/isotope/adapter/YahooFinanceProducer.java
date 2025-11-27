package com.isotope.adapter;

import com.isotope.model.MarketDataEvent;
import com.lmax.disruptor.RingBuffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YahooFinanceProducer implements MarketDataProducer {

    public YahooFinanceProducer() {
    }

    @Override
    public void connect() {
        log.info("YahooFinanceProducer: Connected (Stub).");
    }

    @Override
    public void subscribe(String... symbols) {
        log.info("YahooFinanceProducer: Subscribed to {}", (Object) symbols);
    }

    @Override
    public void startPublishing(RingBuffer<MarketDataEvent> ringBuffer) {
        log.info("YahooFinanceProducer: Started publishing (Stub - no actual data).");
        // Here we would call Python script or API
    }
}
