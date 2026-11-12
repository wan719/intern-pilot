package com.internpilot.analysis.service;

import com.internpilot.common.PageResult;
import com.internpilot.analysis.dto.AnalysisMatchRequest;
import com.internpilot.analysis.vo.AnalysisReportDetailResponse;
import com.internpilot.analysis.vo.AnalysisReportListResponse;
import com.internpilot.analysis.vo.AnalysisResultResponse;

public interface AnalysisService {

    AnalysisResultResponse match(AnalysisMatchRequest request);

    AnalysisResultResponse matchForUser(AnalysisMatchRequest request, Long userId);

    PageResult<AnalysisReportListResponse> listReports(
            Long resumeId,
            Long jobId,
            Integer minScore,
            Integer pageNum,
            Integer pageSize
    );

    AnalysisReportDetailResponse getReportDetail(Long id);

    void deleteReport(Long id);
}