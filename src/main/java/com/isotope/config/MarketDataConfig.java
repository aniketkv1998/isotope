package com.isotope.config;

import com.isotope.adapter.CsvMarketDataProducer;
import com.isotope.adapter.KiteMarketDataProducer;
import com.isotope.adapter.MarketDataProducer;
import com.isotope.adapter.YahooFinanceProducer;
import com.zerodhatech.kiteconnect.KiteConnect;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "engine.data-source")
public class MarketDataConfig {

    private String type; // KITE, CSV, YAHOO
    private CsvConfig csv = new CsvConfig();

    @Data
    public static class CsvConfig {
        private String filePath;
        private String replaySpeed;
    }

    @Bean
    @ConditionalOnProperty(name = "engine.data-source.type", havingValue = "KITE")
    public MarketDataProducer kiteMarketDataProducer(KiteConnect kiteConnect) {
        return new KiteMarketDataProducer(kiteConnect);
    }

    @Bean
    @ConditionalOnProperty(name = "engine.data-source.type", havingValue = "CSV")
    public MarketDataProducer csvMarketDataProducer() {
        return new CsvMarketDataProducer(csv.getFilePath(), csv.getReplaySpeed());
    }

    @Bean
    @ConditionalOnProperty(name = "engine.data-source.type", havingValue = "YAHOO")
    public MarketDataProducer yahooMarketDataProducer() {
        return new YahooFinanceProducer();
    }
}
