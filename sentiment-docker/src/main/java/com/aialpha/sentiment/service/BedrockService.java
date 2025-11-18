package com.aialpha.sentiment.service;

import com.aialpha.sentiment.model.CompanySentiment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.ArrayList;
import java.util.List;

@Service
public class BedrockService {

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;

    private static final String MODEL_ID = "amazon.nova-micro-v1:0";

    public BedrockService(BedrockRuntimeClient bedrockClient, ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.objectMapper = objectMapper;
    }

    public List<CompanySentiment> analyzeSentiment(String text) {
        try {
            // Build the prompt for Nova
            String prompt = buildSentimentPrompt(text);

            // Create request payload for Nova API via Bedrock
            String requestBody = String.format("""
                {
                    "schemaVersion": "messages-v1",
                    "messages": [
                        {
                            "role": "user",
                            "content": [
                                {
                                    "text": "%s"
                                }
                            ]
                        }
                    ],
                    "inferenceConfig": {
                        "maxTokens": 2000,
                        "temperature": 0.7,
                        "topP": 0.9
                    }
                }
                """, escapeJson(prompt));

            // Invoke Bedrock model
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .body(SdkBytes.fromUtf8String(requestBody))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            // Parse Nova's response
            return parseNovaResponse(responseBody);

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze sentiment with Bedrock: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a one-shot prompt that asks Nova to analyze sentiment
     * for each Big Tech company mentioned in the text.
     */
    private String buildSentimentPrompt(String text) {
        return String.format("""
            Analyze the sentiment for each Big Tech company mentioned in the following text.

            Big Tech companies include but not limited to: Apple, Microsoft, Google, Alphabet, Amazon, Meta, Facebook,
            NVIDIA, Tesla, IBM, Intel, AMD, Oracle, Salesforce, Adobe, Netflix, Uber, Airbnb, Twitter, X Corp.

            For each company found, provide:
            - company: The company name (use standard form, e.g., "Meta" not "Facebook")
            - sentiment: POSITIVE, NEGATIVE, NEUTRAL, or MIXED
            - confidence: A score between 0.0 and 1.0 indicating your confidence
            - reasoning: A brief explanation (1-2 sentences) for your assessment

            Return ONLY a valid JSON object with this exact structure:
            {
              "companies": [
                {
                  "company": "CompanyName",
                  "sentiment": "POSITIVE",
                  "confidence": 0.95,
                  "reasoning": "Brief explanation here"
                }
              ]
            }

            If no Big Tech companies are mentioned, return: {"companies": []}

            Text to analyze:
            %s
            """, text);
    }

    /**
     * Parses Nova's JSON response and extracts company sentiment data.
     */
    private List<CompanySentiment> parseNovaResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Nova's response structure: { "output": { "message": { "content": [{ "text": "..." }] } } }
        JsonNode outputNode = root.path("output");
        if (outputNode.isMissingNode()) {
            throw new RuntimeException("Invalid response structure from Nova: missing 'output' field");
        }

        JsonNode messageNode = outputNode.path("message");
        if (messageNode.isMissingNode()) {
            throw new RuntimeException("Invalid response structure from Nova: missing 'message' field");
        }

        JsonNode contentArray = messageNode.path("content");
        if (contentArray.isMissingNode() || !contentArray.isArray() || contentArray.isEmpty()) {
            throw new RuntimeException("Invalid response structure from Nova: missing or empty 'content' array");
        }

        // Extract the text content (which should be our JSON)
        String textContent = contentArray.get(0).path("text").asText();

        // Parse the JSON that Nova returned
        JsonNode sentimentData = objectMapper.readTree(textContent);
        JsonNode companiesArray = sentimentData.path("companies");

        List<CompanySentiment> results = new ArrayList<>();
        if (companiesArray.isArray()) {
            for (JsonNode companyNode : companiesArray) {
                CompanySentiment sentiment = new CompanySentiment(
                        companyNode.path("company").asText(),
                        companyNode.path("sentiment").asText(),
                        companyNode.path("confidence").asDouble(),
                        companyNode.path("reasoning").asText()
                );
                results.add(sentiment);
            }
        }

        return results;
    }

    /**
     * Escapes special characters in JSON strings.
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public String getModelId() {
        return MODEL_ID;
    }
}
