package com.isotope.adapter;

import com.isotope.model.MarketDataEvent;
import com.lmax.disruptor.RingBuffer;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class KiteMarketDataProducer implements MarketDataProducer {

    private final KiteConnect kiteConnect;
    private RingBuffer<MarketDataEvent> ringBuffer;

    public KiteMarketDataProducer(KiteConnect kiteConnect) {
        this.kiteConnect = kiteConnect;
    }

    @Override
    public void connect() {
        log.info("Connecting to Kite Ticker...");
        // In a real implementation:
        // KiteTicker ticker = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());
        // ticker.setOnTickerArrivalListener(this::onTicks);
        // ticker.connect();
    }

    @Override
    public void subscribe(String... symbols) {
        log.info("Subscribing to symbols via Kite: {}", (Object) symbols);
        // Convert symbols to tokens and subscribe
    }

    @Override
    public void startPublishing(RingBuffer<MarketDataEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
        log.info("KiteMarketDataProducer started publishing to RingBuffer.");
    }

    /**
     * Callback for Kite Ticker
     */
    public void onTicks(ArrayList<Tick> ticks) {
        if (ringBuffer == null) return;

        for (Tick tick : ticks) {
            publishTick(tick);
        }
    }

    private void publishTick(Tick tick) {
        long sequence = ringBuffer.next();
        try {
            MarketDataEvent event = ringBuffer.get(sequence);
            event.setInstrumentToken(tick.getInstrumentToken());
            event.setLastTradedPrice(tick.getLastTradedPrice());
            event.setVolume(tick.getVolumeTradedToday());

            if (tick.getMarketDepth() != null && tick.getMarketDepth().get("buy") != null && !tick.getMarketDepth().get("buy").isEmpty()) {
                event.setBidPrice(tick.getMarketDepth().get("buy").getFirst().getPrice());
                event.setBidQuantity(tick.getMarketDepth().get("buy").getFirst().getQuantity());
            }
            if (tick.getMarketDepth() != null && tick.getMarketDepth().get("sell") != null && !tick.getMarketDepth().get("sell").isEmpty()) {
                event.setAskPrice(tick.getMarketDepth().get("sell").getFirst().getPrice());
                event.setAskQuantity(tick.getMarketDepth().get("sell").getFirst().getQuantity());
            }

        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
