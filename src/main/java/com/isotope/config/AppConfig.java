package com.isotope.config;

import com.zerodhatech.kiteconnect.KiteConnect;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "isotope")
public class AppConfig {

    private KiteConfig kite = new KiteConfig();
    private StrategyConfig strategy = new StrategyConfig();
    private BacktestConfig backtest = new BacktestConfig();

    @Bean
    public KiteConnect kiteConnect() {
        KiteConnect kiteConnect = new KiteConnect(kite.getApiKey());
        kiteConnect.setAccessToken(kite.getAccessToken());
        return kiteConnect;
    }

    @Data
    public static class KiteConfig {
        private String apiKey;
        private String accessToken;
        private String apiSecret;
    }

    @Data
    public static class StrategyConfig {
        private boolean enabled;
        private double riskLimit;
    }

    @Data
    public static class BacktestConfig {
        private double initialCapital = 1000000.0;
    }
}
