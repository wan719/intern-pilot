package com.internpilot.vo.application;

import lombok.Data;

@Data
public class ApplicationPlanContextResponse {
    private String outcome;
    private Long reportId;
    private Long applicationId;
    private JobSummary job;
    private ResumeSummary resume;
    private String reason;

    @Data
    public static class JobSummary {
        private Long id;
        private String companyName;
        private String jobTitle;
    }

    @Data
    public static class ResumeSummary {
        private Long id;
        private String resumeName;
    }
}
