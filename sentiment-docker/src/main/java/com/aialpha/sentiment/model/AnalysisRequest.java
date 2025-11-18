package com.aialpha.sentiment.model;

public class AnalysisRequest {
    private String requestId;
    private String text;

    public AnalysisRequest() {
    }

    public AnalysisRequest(String requestId, String text) {
        this.requestId = requestId;
        this.text = text;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
