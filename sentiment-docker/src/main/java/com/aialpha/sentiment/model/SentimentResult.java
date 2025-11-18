package com.aialpha.sentiment.model;

import java.time.Instant;
import java.util.List;

public class SentimentResult {
    private String requestId;
    private String timestamp;
    private String method;
    private String model;
    private List<CompanySentiment> companies;

    public SentimentResult() {
        this.timestamp = Instant.now().toString();
    }

    public SentimentResult(String requestId, String method, String model, List<CompanySentiment> companies) {
        this.requestId = requestId;
        this.timestamp = Instant.now().toString();
        this.method = method;
        this.model = model;
        this.companies = companies;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<CompanySentiment> getCompanies() {
        return companies;
    }

    public void setCompanies(List<CompanySentiment> companies) {
        this.companies = companies;
    }
}
