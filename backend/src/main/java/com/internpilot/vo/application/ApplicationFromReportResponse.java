package com.internpilot.vo.application;

import lombok.Data;

@Data
public class ApplicationFromReportResponse {
    private String outcome;
    private Long applicationId;

    public static ApplicationFromReportResponse of(String outcome, Long applicationId) {
        ApplicationFromReportResponse response = new ApplicationFromReportResponse();
        response.setOutcome(outcome);
        response.setApplicationId(applicationId);
        return response;
    }
}
