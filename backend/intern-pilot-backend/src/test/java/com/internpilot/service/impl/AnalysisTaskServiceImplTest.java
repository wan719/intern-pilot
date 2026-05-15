package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.entity.AnalysisTask;
import com.internpilot.enums.AnalysisTaskStatusEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisTaskMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.service.AnalysisProgressPublisher;
import com.internpilot.service.AnalysisService;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisTaskServiceImplTest {

    @Mock
    private AnalysisTaskMapper analysisTaskMapper;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private AnalysisProgressPublisher progressPublisher;

    @Mock
    private Executor analysisTaskExecutor;

    private AnalysisTaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskService = new AnalysisTaskServiceImpl(
                analysisTaskMapper,
                analysisService,
                progressPublisher,
                analysisTaskExecutor
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTaskShouldInsertAndPublishPending() {
        mockLoginUser(1L);

        AnalysisTaskCreateRequest request = new AnalysisTaskCreateRequest();
        request.setResumeId(10L);
        request.setJobId(20L);
        request.setForceRefresh(false);

        doAnswer(invocation -> {
            AnalysisTask task = invocation.getArgument(0);
            task.setId(1L);
            return 1;
        }).when(analysisTaskMapper).insert(any(AnalysisTask.class));

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            return null;
        }).when(analysisTaskExecutor).execute(any(Runnable.class));

        var response = taskService.createTask(request);

        assertNotNull(response.getTaskNo());
        assertTrue(response.getTaskNo().startsWith("TASK_"));
        assertEquals(AnalysisTaskStatusEnum.PENDING.getCode(), response.getStatus());
        assertEquals(0, response.getProgress());

        ArgumentCaptor<AnalysisTask> taskCaptor = ArgumentCaptor.forClass(AnalysisTask.class);
        verify(analysisTaskMapper).insert(taskCaptor.capture());
        AnalysisTask savedTask = taskCaptor.getValue();
        assertEquals(1L, savedTask.getUserId());
        assertEquals(10L, savedTask.getResumeId());
        assertEquals(20L, savedTask.getJobId());
        assertEquals(AnalysisTaskStatusEnum.PENDING.getCode(), savedTask.getStatus());

        verify(progressPublisher).publish(
                anyString(), eq(1L), eq(AnalysisTaskStatusEnum.PENDING.getCode()),
                eq(0), eq("任务已创建"), eq(null), eq(null)
        );
    }

    @Test
    void getTaskDetailShouldReturnTaskForOwner() {
        mockLoginUser(1L);

        AnalysisTask task = buildTask("TASK_001", 1L, AnalysisTaskStatusEnum.PARSING_RESUME.getCode(), 15);
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        AnalysisTaskDetailResponse detail = taskService.getTaskDetail("TASK_001");

        assertEquals("TASK_001", detail.getTaskNo());
        assertEquals(AnalysisTaskStatusEnum.PARSING_RESUME.getCode(), detail.getStatus());
        assertEquals(15, detail.getProgress());
    }

    @Test
    void getTaskDetailShouldThrowWhenNotOwner() {
        mockLoginUser(2L);

        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> taskService.getTaskDetail("TASK_001"));
    }

    @Test
    void getTaskDetailShouldThrowWhenTaskNotFound() {
        mockLoginUser(1L);

        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> taskService.getTaskDetail("NONEXISTENT"));
    }

    @Test
    void getTaskDetailShouldIncludeUserIdInQuery() {
        mockLoginUser(1L);

        AnalysisTask task = buildTask("TASK_001", 1L, AnalysisTaskStatusEnum.COMPLETED.getCode(), 100);
        task.setReportId(99L);
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        AnalysisTaskDetailResponse detail = taskService.getTaskDetail("TASK_001");

        assertEquals(99L, detail.getReportId());
        assertEquals(AnalysisTaskStatusEnum.COMPLETED.getCode(), detail.getStatus());
        assertEquals(100, detail.getProgress());
    }

    @Test
    void createTaskShouldNotExecuteWhenInsertFails() {
        mockLoginUser(1L);

        AnalysisTaskCreateRequest request = new AnalysisTaskCreateRequest();
        request.setResumeId(10L);
        request.setJobId(20L);

        when(analysisTaskMapper.insert(any(AnalysisTask.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> taskService.createTask(request));
        verify(analysisTaskExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void createTaskShouldCompleteDirectlyWhenAnalysisCacheHit() {
        mockLoginUser(1L);

        AnalysisTaskCreateRequest request = new AnalysisTaskCreateRequest();
        request.setResumeId(10L);
        request.setJobId(20L);

        doAnswer(invocation -> {
            AnalysisTask task = invocation.getArgument(0);
            task.setId(1L);
            return 1;
        }).when(analysisTaskMapper).insert(any(AnalysisTask.class));

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(analysisTaskExecutor).execute(any(Runnable.class));

        AnalysisTask task = buildTask("TASK_001", 1L, AnalysisTaskStatusEnum.PENDING.getCode(), 0);
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        AnalysisResultResponse result = new AnalysisResultResponse();
        result.setReportId(99L);
        result.setCacheHit(true);
        when(analysisService.matchForUser(any(), eq(1L))).thenReturn(result);

        taskService.createTask(request);

        ArgumentCaptor<AnalysisTask> updateCaptor = ArgumentCaptor.forClass(AnalysisTask.class);
        verify(analysisTaskMapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());

        assertTrue(updateCaptor.getAllValues().stream()
                .anyMatch(update -> AnalysisTaskStatusEnum.COMPLETED.getCode().equals(update.getStatus())
                        && Long.valueOf(99L).equals(update.getReportId())));
        assertTrue(updateCaptor.getAllValues().stream()
                .noneMatch(update -> AnalysisTaskStatusEnum.GENERATING_REPORT.getCode().equals(update.getStatus())));

        verify(progressPublisher).publish(
                anyString(),
                eq(1L),
                eq(AnalysisTaskStatusEnum.COMPLETED.getCode()),
                eq(AnalysisTaskStatusEnum.COMPLETED.getDefaultProgress()),
                anyString(),
                eq(99L),
                eq(null)
        );
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "testuser", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private AnalysisTask buildTask(String taskNo, Long userId, String status, Integer progress) {
        AnalysisTask task = new AnalysisTask();
        task.setId(1L);
        task.setTaskNo(taskNo);
        task.setUserId(userId);
        task.setResumeId(10L);
        task.setJobId(20L);
        task.setStatus(status);
        task.setProgress(progress);
        task.setMessage("测试消息");
        return task;
    }
}
