package com.aialpha.sentiment.model;

public class CompanySentiment {
    private String company;
    private String sentiment;
    private double confidence;
    private String reasoning;

    public CompanySentiment() {
    }

    public CompanySentiment(String company, String sentiment, double confidence, String reasoning) {
        this.company = company;
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}
