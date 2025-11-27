package com.isotope.adapter;

import com.isotope.model.MarketDataEvent;
import com.lmax.disruptor.RingBuffer;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.models.Tick;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * Wrapper for KiteTicker that pushes ticks to the Input RingBuffer.
 */
@Slf4j
public class MarketDataAdapter {
    private final RingBuffer<MarketDataEvent> ringBuffer;
    // Mocking KiteTicker for now as we don't have real credentials or network
    // In production this would extend or contain KiteTicker

    public MarketDataAdapter(RingBuffer<MarketDataEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * Callback method simulating what happens when KiteTicker receives ticks.
     * This method is responsible for translating external Tick objects into
     * zero-GC events on the RingBuffer.
     */
    public void onTicks(ArrayList<Tick> ticks) {
        for (Tick tick : ticks) {
            publishTick(tick);
        }
    }

    private void publishTick(Tick tick) {
        long sequence = ringBuffer.next();  // Grab the next sequence
        try {
            MarketDataEvent event = ringBuffer.get(sequence); // Get the entry in the Disruptor

            // Copy data from the external Tick object to our internal pre-allocated event
            // Note: We avoid 'new' here.
            event.setInstrumentToken(tick.getInstrumentToken());
            event.setLastTradedPrice(tick.getLastTradedPrice());
            event.setVolume(tick.getVolumeTradedToday());
            // event.setLastTradedTime(tick.getTickTimestamp().getTime()); // Assuming date is not null

            if (tick.getMarketDepth() != null && tick.getMarketDepth().get("buy") != null && !tick.getMarketDepth().get("buy").isEmpty()) {
                 event.setBidPrice(tick.getMarketDepth().get("buy").get(0).getPrice());
                 event.setBidQuantity(tick.getMarketDepth().get("buy").get(0).getQuantity());
            }
             if (tick.getMarketDepth() != null && tick.getMarketDepth().get("sell") != null && !tick.getMarketDepth().get("sell").isEmpty()) {
                 event.setAskPrice(tick.getMarketDepth().get("sell").get(0).getPrice());
                 event.setAskQuantity(tick.getMarketDepth().get("sell").get(0).getQuantity());
            }

        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
