package com.aialpha.sentiment.controller;

import com.aialpha.sentiment.model.AnalysisRequest;
import com.aialpha.sentiment.model.CompanySentiment;
import com.aialpha.sentiment.model.SentimentResult;
import com.aialpha.sentiment.service.BedrockService;
import com.aialpha.sentiment.service.S3StorageService;
import com.aialpha.sentiment.metrics.SentimentMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SentimentController {

    private final BedrockService bedrockService;
    private final S3StorageService s3StorageService;
    private final SentimentMetrics sentimentMetrics;

    public SentimentController(BedrockService bedrockService,
                              S3StorageService s3StorageService,
                              SentimentMetrics sentimentMetrics) {
        this.bedrockService = bedrockService;
        this.s3StorageService = s3StorageService;
        this.sentimentMetrics = sentimentMetrics;
    }

    @PostMapping("/analyze")
    public ResponseEntity<SentimentResult> analyzeSentiment(@RequestBody AnalysisRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            // Call Bedrock to analyze sentiment
            List<CompanySentiment> companies = bedrockService.analyzeSentiment(request.getText());

            // Create result
            SentimentResult result = new SentimentResult(
                    request.getRequestId(),
                    "AI-Powered (AWS Bedrock + Claude)",
                    bedrockService.getModelId(),
                    companies
            );

            // Store in S3
            s3StorageService.storeResult(result);

            // Record metrics (if implemented by student)
            long duration = System.currentTimeMillis() - startTime;
            sentimentMetrics.recordCompaniesDetected(companies.size());
            for (CompanySentiment company : companies) {
                sentimentMetrics.recordAnalysis(company.getSentiment(), company.getCompany());
                sentimentMetrics.recordConfidence(company.getConfidence(), company.getSentiment(), company.getCompany());
                sentimentMetrics.recordDuration(duration, company.getCompany(), bedrockService.getModelId());
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
