package com.internpilot.service;

public interface AnalysisProgressPublisher {

    void publish(
            String taskNo,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    );
}