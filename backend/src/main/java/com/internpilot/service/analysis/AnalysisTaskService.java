package com.internpilot.service.analysis;

import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;

import java.util.List;

public interface AnalysisTaskService {

    AnalysisTaskCreateResponse createTask(AnalysisTaskCreateRequest request);

    AnalysisTaskDetailResponse getTaskDetail(String taskNo);

    List<AnalysisTaskDetailResponse> listRunningTasks();

    AnalysisTaskDetailResponse cancelTask(String taskNo);

    List<AnalysisTaskDetailResponse> listRecentTasks(Integer limit);
}