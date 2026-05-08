package com.internpilot.service;

import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;

public interface AnalysisTaskService {

    AnalysisTaskCreateResponse createTask(AnalysisTaskCreateRequest request);

    AnalysisTaskDetailResponse getTaskDetail(String taskNo);
}