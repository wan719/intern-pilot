package com.internpilot.analysis.service;

import com.internpilot.analysis.dto.AnalysisTaskCreateRequest;
import com.internpilot.analysis.vo.AnalysisTaskCreateResponse;
import com.internpilot.analysis.vo.AnalysisTaskDetailResponse;

import java.util.List;

public interface AnalysisTaskService {

    AnalysisTaskCreateResponse createTask(AnalysisTaskCreateRequest request);

    AnalysisTaskDetailResponse getTaskDetail(String taskNo);

    List<AnalysisTaskDetailResponse> listRunningTasks();

    AnalysisTaskDetailResponse cancelTask(String taskNo);

    List<AnalysisTaskDetailResponse> listRecentTasks(Integer limit);
}