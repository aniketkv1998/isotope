package com.isotope.strategy;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Slf4j
public class StrategyLoader {

    public List<Strategy> loadStrategies(String strategyDir) {
        List<Strategy> strategies = new ArrayList<>();
        File dir = new File(strategyDir);

        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Strategy directory not found: {}", strategyDir);
            return strategies;
        }

        // In a full implementation, this would load .jar files dynamically.
        // For this scaffold, we will assume strategies are in the classpath
        // or we manually instantiate known strategies for simplicity,
        // OR we use ServiceLoader if they were separate jars.

        // Simulating loading by just returning an empty list or scanning (simplified)
        log.info("Scanning for strategies in {}", strategyDir);

        // In a real implementation, we would use URLClassLoader to load jars from the dir
        // For this scaffold, we will manually add the demo strategy if the dir matches "strategies"
        // to prove the flow.
        if (strategyDir.contains("strategies")) {
             // This is just to demonstrate that the loader *logic* would go here.
             // In the Main.java we handle the fallback, but let's make the loader useful too.
             // strategies.add(new PairsTradingStrategy());
        }

        return strategies;
    }
}
