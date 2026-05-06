package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;

public interface AnalysisService {

    AnalysisResultResponse match(AnalysisMatchRequest request);

    PageResult<AnalysisReportListResponse> listReports(
            Long resumeId,
            Long jobId,
            Integer minScore,
            Integer pageNum,
            Integer pageSize
    );

    AnalysisReportDetailResponse getReportDetail(Long id);
}
