package com.aialpha.sentiment.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class SentimentMetrics {

    private final MeterRegistry meterRegistry;

    // Constructor injection of MeterRegistry
    public SentimentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Example implementation: Counter for sentiment analysis requests
     * This counter tracks the total number of sentiment analyses by sentiment type and company
     */
    public void recordAnalysis(String sentiment, String company) {
        Counter.builder("sentiment.analysis.total")
                .tag("sentiment", sentiment)
                .tag("company", company)
                .description("Total number of sentiment analysis requests")
                .register(meterRegistry)
                .increment();
    }

    public void recordDuration(long milliseconds, String company, String model) {
        // TODO: Record timer
    }

    public void recordCompaniesDetected(int count) {
        // TODO: Update gauge
    }

    public void recordConfidence(double confidence, String sentiment, String company) {
        // TODO: Record distribution summary
    }
}
