package com.isotope.adapter;

import com.isotope.model.MarketDataEvent;
import com.lmax.disruptor.RingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CsvMarketDataProducer implements MarketDataProducer {

    private final String csvFilePath;
    private final String replaySpeedStr; // "1x", "10x", "MAX"
    private RingBuffer<MarketDataEvent> ringBuffer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;

    public CsvMarketDataProducer(String csvFilePath, String replaySpeedStr) {
        this.csvFilePath = csvFilePath;
        this.replaySpeedStr = replaySpeedStr;
    }

    @Override
    public void connect() {
        log.info("Connected to CSV Source: {}", csvFilePath);
    }

    @Override
    public void subscribe(String... symbols) {
        log.info("Subscribing to symbols (CSV source publishes all): {}", (Object) symbols);
    }

    @Override
    public void startPublishing(RingBuffer<MarketDataEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
        this.running = true;
        executor.submit(this::readAndPublish);
    }

    private void readAndPublish() {
        log.info("Starting CSV playback with speed: {}", replaySpeedStr);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ClassPathResource(csvFilePath).getInputStream()))) {
            String line;
            boolean firstLine = true;
            long previousTimestamp = -1;
            long rowCount = 0;

            double speedFactor = 1.0;
            boolean isMax = "MAX".equalsIgnoreCase(replaySpeedStr);
            if (!isMax) {
                try {
                    speedFactor = Double.parseDouble(replaySpeedStr.toLowerCase().replace("x", ""));
                } catch (NumberFormatException e) {
                    log.warn("Invalid replay speed format '{}', defaulting to 1x", replaySpeedStr);
                }
            }

            // Headers: timestamp,symbol,open,high,low,close,volume
            while (running && (line = br.readLine()) != null) {
                if (firstLine) {
                    if (line.toLowerCase().startsWith("timestamp")) {
                        firstLine = false;
                        continue;
                    }
                    firstLine = false;
                }

                String[] parts = line.split(",");
                if (parts.length < 7) continue;

                try {
                    String timeStr = parts[0];
                    String symbol = parts[1];
                    double close = Double.parseDouble(parts[5]);
                    long volume = Long.parseLong(parts[6]);

                    // 1. Parse Historical Time
                    LocalDateTime timestamp = LocalDateTime.parse(timeStr); // Expects ISO format 2023-10-27T10:00:00
                    long currentEventTime = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                    // 2. Replay Delay Logic
                    if (!isMax && previousTimestamp != -1) {
                        long timeDiff = currentEventTime - previousTimestamp;
                        if (timeDiff > 0) {
                            long sleepTime = (long) (timeDiff / speedFactor);
                            if (sleepTime > 0) {
                                Thread.sleep(sleepTime);
                            }
                        }
                    }

                    previousTimestamp = currentEventTime;

                    // 3. Publish with HISTORICAL Time
                    publishEvent(symbol, close, volume, currentEventTime);

                    rowCount++;
                    if (rowCount % 1000 == 0) {
                        log.info("Processed {} rows. Current Hist Time: {}", rowCount, timeStr);
                    }

                } catch (Exception e) {
                    log.error("Error parsing CSV line: {}", line, e);
                }
            }
            log.info("CSV Playback finished. Total Rows: {}", rowCount);

        } catch (Exception e) {
            log.error("Error reading CSV file", e);
        }
    }

    private void publishEvent(String symbol, double price, long volume, long historicalTime) {
        long sequence = ringBuffer.next();
        try {
            MarketDataEvent event = ringBuffer.get(sequence);

            // Map text symbols to IDs
            if ("NIFTY".equalsIgnoreCase(symbol)) {
                event.setInstrumentToken(256265);
            } else if ("BANKNIFTY".equalsIgnoreCase(symbol)) {
                event.setInstrumentToken(260105);
            } else {
                event.setInstrumentToken(symbol.hashCode());
            }

            event.setLastTradedPrice(price);
            event.setVolume(volume);

            // FIX: Use the CSV time, NOT System.currentTimeMillis()
            event.setLastTradedTime(historicalTime);

            // Clear other fields
            event.setBidPrice(0);
            event.setAskPrice(0);
            event.setBidQuantity(0);
            event.setAskQuantity(0);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}