package com.isotope.core;

import com.isotope.adapter.OrderExecutionAdapter;
import com.isotope.model.MarketDataEvent;
import com.isotope.model.MarketDataEventFactory;
import com.isotope.model.OrderEvent;
import com.isotope.model.OrderEventFactory;
import com.isotope.strategy.Strategy;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class IsotopeEngine implements OrderPublisher {

    private final Disruptor<MarketDataEvent> marketDataDisruptor;
    private final Disruptor<OrderEvent> orderDisruptor;

    @Getter
    private final RingBuffer<MarketDataEvent> marketDataRingBuffer;
    @Getter
    private final RingBuffer<OrderEvent> orderRingBuffer;

    private final List<Strategy> strategies = new ArrayList<>();

    private static final int BUFFER_SIZE = 1024; // Must be power of 2

    public IsotopeEngine() {
        // 1. Setup Output Disruptor (Orders) first, so strategies can use it
        OrderEventFactory orderFactory = new OrderEventFactory();
        orderDisruptor = new Disruptor<>(
                orderFactory,
                BUFFER_SIZE,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, // Multiple strategies might push orders
                new BlockingWaitStrategy()
        );

        // Connect Consumer: OrderExecutionAdapter
        orderDisruptor.handleEventsWith(new OrderExecutionAdapter());

        orderRingBuffer = orderDisruptor.getRingBuffer();


        // 2. Setup Input Disruptor (Market Data)
        MarketDataEventFactory marketDataFactory = new MarketDataEventFactory();
        marketDataDisruptor = new Disruptor<>(
                marketDataFactory,
                BUFFER_SIZE,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, // ProducerType.MULTI because we might have different threads publishing or just safe default
                // Actually, if we use Single Writer Principle we want SINGLE, but Adapter might run on its own thread.
                // CsvProducer runs on executor. KiteTicker runs on its own thread.
                // If we guarantee only one producer thread active at a time, we can use SINGLE.
                // But the producer interface doesn't guarantee single threaded access to ringbuffer if we had multiple producers (which we don't for now).
                // Let's stick to MULTI to be safe or SINGLE if we are sure.
                // The previous code had SINGLE with MarketDataAdapter.
                // MarketDataAdapter was just a wrapper.
                // Here the producer is external.
                // Let's use SINGLE for performance if we assume 1 active producer.
                // But wait, CsvProducer uses a thread. Kite uses a thread.
                // As long as there is only ONE thread calling publish(), SINGLE is fine.
                // We only inject ONE producer. So SINGLE is fine.
                new BlockingWaitStrategy()
        );

        // Connect Consumer: Strategy Processor
        marketDataDisruptor.handleEventsWith(new StrategyEventHandler());

        marketDataRingBuffer = marketDataDisruptor.getRingBuffer();
    }

    public void start() {
        log.info("Starting Isotope Engine...");
        orderDisruptor.start();
        marketDataDisruptor.start();
        log.info("Engine Started.");
    }

    public void stop() {
        marketDataDisruptor.shutdown();
        orderDisruptor.shutdown();
    }

    public void registerStrategy(Strategy strategy) {
        strategy.setOrderPublisher(this);
        strategies.add(strategy);
        log.info("Registered strategy: {}", strategy.getStrategyId());
    }

    /**
     * Internal EventHandler that dispatches ticks to all registered strategies.
     * This runs on the MarketData Processor Thread (Single Writer Principle).
     */
    private class StrategyEventHandler implements EventHandler<MarketDataEvent> {
        @Override
        public void onEvent(MarketDataEvent event, long sequence, boolean endOfBatch) {
            for (Strategy strategy : strategies) {
                try {
                    strategy.onTick(event);
                } catch (Exception e) {
                    log.error("Error in strategy {}", strategy.getStrategyId(), e);
                }
            }
        }
    }

    // --- OrderPublisher Implementation ---

    @Override
    public void publishOrder(String symbol, OrderEvent.Type type, int quantity, double price, String strategyId) {
        long sequence = orderRingBuffer.next();
        try {
            OrderEvent event = orderRingBuffer.get(sequence);
            event.setTradingSymbol(symbol);
            event.setType(type);
            event.setQuantity(quantity);
            event.setPrice(price);
            event.setStrategyId(strategyId);
            event.setTimestamp(System.currentTimeMillis());
        } finally {
            orderRingBuffer.publish(sequence);
        }
    }
}
