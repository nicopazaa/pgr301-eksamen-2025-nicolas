package com.aialpha.sentiment;

// Trigger Docker CI

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SentimentDockerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentimentDockerApplication.class, args);
    }
}
