package com.aialpha.sentiment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SentimentMetrics {

    private final MeterRegistry meterRegistry;

    // Gauge for "current number of companies detected" on the last request
    private final AtomicInteger companiesGauge;

    public SentimentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Gauge is created once and oppdateres hver gang vi har et nytt resultat
        this.companiesGauge = meterRegistry.gauge(
                "sentiment.analysis.companies.detected",
                new AtomicInteger(0)
        );
    }

    /**
     * Counter for antall analyser, tagget på sentiment og selskap.
     */
    public void recordAnalysis(String sentiment, String company) {
        Counter.builder("sentiment.analysis.total")
                .tag("sentiment", sentiment == null ? "UNKNOWN" : sentiment)
                .tag("company", company == null || company.isBlank() ? "UNKNOWN" : company)
                .description("Total number of sentiment analysis requests per sentiment/company")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Timer for hvor lang tid en analyse tar.
     */
    public void recordDuration(long milliseconds, String company, String model) {
        Timer.builder("sentiment.analysis.duration")
                .description("Time taken to perform a sentiment analysis")
                .tag("company", company == null || company.isBlank() ? "UNKNOWN" : company)
                .tag("model", model == null || model.isBlank() ? "UNKNOWN" : model)
                .register(meterRegistry)
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Gauge for antall selskaper detektert i siste request.
     */
    public void recordCompaniesDetected(int count) {
        if (companiesGauge != null) {
            companiesGauge.set(count);
        }
    }

    /**
     * DistributionSummary for modellens confidence per selskap og sentiment.
     */
    public void recordConfidence(double confidence, String sentiment, String company) {
        DistributionSummary.builder("sentiment.analysis.confidence")
                .description("Model confidence per company and sentiment (0.0 - 1.0)")
                .baseUnit("ratio")
                .tag("sentiment", sentiment == null ? "UNKNOWN" : sentiment)
                .tag("company", company == null || company.isBlank() ? "UNKNOWN" : company)
                .register(meterRegistry)
                .record(confidence);
    }
}
