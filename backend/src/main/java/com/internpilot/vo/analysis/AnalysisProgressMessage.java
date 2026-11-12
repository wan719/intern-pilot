package com.internpilot.vo.analysis;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalysisProgressMessage {

    private String taskNo;

    private Long userId;

    private String status;

    private Integer progress;

    private String message;

    private Long reportId;

    private String errorMessage;

    private LocalDateTime timestamp;

    public static AnalysisProgressMessage of(
            String taskNo,
            Long userId,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisProgressMessage progressMessage = new AnalysisProgressMessage();
        progressMessage.setTaskNo(taskNo);
        progressMessage.setUserId(userId);
        progressMessage.setStatus(status);
        progressMessage.setProgress(progress);
        progressMessage.setMessage(message);
        progressMessage.setReportId(reportId);
        progressMessage.setErrorMessage(errorMessage);
        progressMessage.setTimestamp(LocalDateTime.now());
        return progressMessage;
    }
}
