package com.internpilot.vo.analysis;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalysisProgressMessage {

    private String taskNo;

    private String status;

    private Integer progress;

    private String message;

    private Long reportId;

    private String errorMessage;

    private LocalDateTime time;

    public static AnalysisProgressMessage of(
            String taskNo,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisProgressMessage progressMessage = new AnalysisProgressMessage();
        progressMessage.setTaskNo(taskNo);
        progressMessage.setStatus(status);
        progressMessage.setProgress(progress);
        progressMessage.setMessage(message);
        progressMessage.setReportId(reportId);
        progressMessage.setErrorMessage(errorMessage);
        progressMessage.setTime(LocalDateTime.now());
        return progressMessage;
    }
}