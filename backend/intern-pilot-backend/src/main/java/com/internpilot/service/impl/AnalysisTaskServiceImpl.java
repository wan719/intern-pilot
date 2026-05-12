package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.entity.AnalysisTask;
import com.internpilot.enums.AnalysisTaskStatusEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisTaskMapper;
import com.internpilot.service.AnalysisProgressPublisher;
import com.internpilot.service.AnalysisService;
import com.internpilot.service.AnalysisTaskService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class AnalysisTaskServiceImpl implements AnalysisTaskService {

    private final AnalysisTaskMapper analysisTaskMapper;
    private final AnalysisService analysisService;
    private final AnalysisProgressPublisher progressPublisher;

    @Qualifier("analysisTaskExecutor")
    private final Executor analysisTaskExecutor;

    @Override
    public AnalysisTaskCreateResponse createTask(AnalysisTaskCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        AnalysisTask task = new AnalysisTask();
        task.setTaskNo(generateTaskNo());
        task.setUserId(currentUserId);
        task.setResumeId(request.getResumeId());
        task.setResumeVersionId(request.getResumeVersionId());
        task.setJobId(request.getJobId());
        task.setStatus(AnalysisTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setMessage("任务已创建");
        task.setForceRefresh(Boolean.TRUE.equals(request.getForceRefresh()) ? 1 : 0);

        analysisTaskMapper.insert(task);

        progressPublisher.publish(task.getTaskNo(), task.getStatus(), task.getProgress(), task.getMessage(), null, null);
        analysisTaskExecutor.execute(() -> executeTask(task.getTaskNo(), currentUserId));

        return toCreateResponse(task);
    }

    @Override
    public AnalysisTaskDetailResponse getTaskDetail(String taskNo) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        AnalysisTask task = analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getTaskNo, taskNo)
                        .eq(AnalysisTask::getUserId, currentUserId)
                        .eq(AnalysisTask::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (task == null) {
            throw new BusinessException("分析任务不存在或无权限访问");
        }

        return toDetailResponse(task);
    }

    private void executeTask(String taskNo, Long userId) {
        AnalysisTask task = analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getTaskNo, taskNo)
                        .eq(AnalysisTask::getUserId, userId)
                        .eq(AnalysisTask::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (task == null) {
            return;
        }

        try {
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 10, "正在校验用户权限", null, null);
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 20, "正在读取简历", null, null);
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 30, "正在读取岗位 JD", null, null);
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 40, "正在检查缓存", null, null);
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 50, "正在构建 Prompt", null, null);
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 70, "正在调用 AI 分析", null, null);

            AnalysisMatchRequest matchRequest = new AnalysisMatchRequest();
            matchRequest.setResumeId(task.getResumeId());
            matchRequest.setResumeVersionId(task.getResumeVersionId());
            matchRequest.setJobId(task.getJobId());
            matchRequest.setForceRefresh(task.getForceRefresh() != null && task.getForceRefresh() == 1);

            AnalysisResultResponse result = analysisService.matchForUser(matchRequest, userId);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 85, "正在解析 AI 返回结果", result.getReportId(), null);
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 95, "正在保存分析报告", result.getReportId(), null);
            updateProgress(task, AnalysisTaskStatusEnum.SUCCESS.getCode(), 100, "分析完成", result.getReportId(), null);
        } catch (Exception e) {
            updateProgress(
                    task,
                    AnalysisTaskStatusEnum.FAILED.getCode(),
                    task.getProgress() == null ? 0 : task.getProgress(),
                    "分析失败",
                    null,
                    e.getMessage()
            );
        }
    }

    private void updateProgress(
            AnalysisTask task,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisTask update = new AnalysisTask();
        update.setId(task.getId());
        update.setStatus(status);
        update.setProgress(progress);
        update.setMessage(message);

        if (reportId != null) {
            update.setReportId(reportId);
        }
        if (AnalysisTaskStatusEnum.RUNNING.getCode().equals(status) && task.getStartedAt() == null) {
            update.setStartedAt(LocalDateTime.now());
        }
        if (AnalysisTaskStatusEnum.SUCCESS.getCode().equals(status)
                || AnalysisTaskStatusEnum.FAILED.getCode().equals(status)) {
            update.setFinishedAt(LocalDateTime.now());
        }
        if (errorMessage != null) {
            update.setErrorMessage(errorMessage);
        }

        analysisTaskMapper.updateById(update);

        task.setStatus(status);
        task.setProgress(progress);
        task.setMessage(message);
        if (reportId != null) {
            task.setReportId(reportId);
        }

        progressPublisher.publish(task.getTaskNo(), status, progress, message, task.getReportId(), errorMessage);
    }

    private String generateTaskNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "TASK_" + date + "_" + random;
    }

    private AnalysisTaskCreateResponse toCreateResponse(AnalysisTask task) {
        AnalysisTaskCreateResponse response = new AnalysisTaskCreateResponse();
        response.setTaskId(task.getId());
        response.setTaskNo(task.getTaskNo());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setMessage(task.getMessage());
        return response;
    }

    private AnalysisTaskDetailResponse toDetailResponse(AnalysisTask task) {
        AnalysisTaskDetailResponse response = new AnalysisTaskDetailResponse();
        response.setTaskId(task.getId());
        response.setTaskNo(task.getTaskNo());
        response.setResumeId(task.getResumeId());
        response.setResumeVersionId(task.getResumeVersionId());
        response.setJobId(task.getJobId());
        response.setReportId(task.getReportId());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setMessage(task.getMessage());
        response.setErrorMessage(task.getErrorMessage());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }
}
