package com.aialpha.sentiment.service;

import com.aialpha.sentiment.model.SentimentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3StorageService(S3Client s3Client, ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
    }

    public void storeResult(SentimentResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            String key = generateS3Key(result.getRequestId());

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString(json));
        } catch (Exception e) {
            throw new RuntimeException("Failed to store result in S3: " + e.getMessage(), e);
        }
    }

    private String generateS3Key(String requestId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return String.format("sentiment-results/%s/%s.json", yearMonth, requestId);
    }
}
