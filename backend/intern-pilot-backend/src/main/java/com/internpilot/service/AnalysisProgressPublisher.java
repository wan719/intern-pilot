package com.internpilot.service;

public interface AnalysisProgressPublisher {

    void publish(
            String taskNo,
            Long userId,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    );
}