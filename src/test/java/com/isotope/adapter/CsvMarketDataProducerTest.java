package com.isotope.adapter;

import com.isotope.model.MarketDataEvent;
import com.lmax.disruptor.RingBuffer;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class CsvMarketDataProducerTest {

    @Test
    public void testCsvProducerPublishesEvents() throws Exception {
        // Mock RingBuffer
        RingBuffer<MarketDataEvent> ringBuffer = mock(RingBuffer.class);
        when(ringBuffer.next()).thenReturn(0L);
        when(ringBuffer.get(0L)).thenReturn(new MarketDataEvent());

        // Create Producer
        CsvMarketDataProducer producer = new CsvMarketDataProducer("market_data.csv", "MAX");

        // Need to verify that it calls publish
        // Since it runs in a thread, we might need to wait or change how we test.
        // But for unit test, we can just call the private method if we made it accessible or inject the executor.
        // Or we can just start it and wait.

        CountDownLatch latch = new CountDownLatch(1);

        // Mock publish to count down
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(ringBuffer).publish(anyLong());

        producer.startPublishing(ringBuffer);

        boolean published = latch.await(2, TimeUnit.SECONDS);
        assertTrue(published, "Should have published at least one event from CSV");
    }
}
