package com.internpilot.vo.admin;

import lombok.Data;

@Data
public class AdminDashboardSummaryResponse {

    private Long userCount;

    private Long todayNewUserCount;

    private Long resumeCount;

    private Long jobCount;

    private Long analysisReportCount;

    private Long interviewQuestionReportCount;

    private Long applicationCount;

    private Long todayOperationLogCount;

    private Long failedOperationCount;
}