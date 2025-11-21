package com.aialpha.sentiment.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;
import java.util.Map;

@Configuration
public class MetricsConfig {

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
                .region(Region.EU_NORTH_1) // behold regionen du bruker i AWS
                .build();
    }

    @Bean
    public CloudWatchConfig cloudWatchConfig() {
        return new CloudWatchConfig() {

            // VIKTIG: namespace brukes også i Terraform-dashboard og -alarm
            private final Map<String, String> configuration = Map.of(
                    "cloudwatch.namespace", "kandidat-48-SentimentApp",
                    "cloudwatch.step", Duration.ofSeconds(5).toString()
            );

            @Override
            public String get(String key) {
                return configuration.get(key);
            }
        };
    }

    @Bean
    public MeterRegistry cloudWatchMeterRegistry(CloudWatchAsyncClient cloudWatchAsyncClient,
                                                 CloudWatchConfig cloudWatchConfig) {
        return CloudWatchMeterRegistry.builder(
                        cloudWatchConfig,
                        Clock.SYSTEM,
                        cloudWatchAsyncClient
                )
                .build();
    }
}
