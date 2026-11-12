package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.controller.analysis.AnalysisController;
import com.internpilot.controller.analysis.AnalysisTaskController;
import com.internpilot.controller.interview.InterviewQuestionController;
import com.internpilot.controller.job.JobController;
import com.internpilot.controller.recommendation.JobRecommendationController;
import com.internpilot.controller.resume.ResumeController;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.dto.recommendation.JobRecommendationGenerateRequest;
import com.internpilot.service.analysis.AnalysisService;
import com.internpilot.service.analysis.AnalysisTaskService;
import com.internpilot.service.interview.InterviewQuestionService;
import com.internpilot.service.job.JobService;
import com.internpilot.service.recommendation.JobRecommendationService;
import com.internpilot.service.resume.ResumeService;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchDetailResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchListResponse;
import com.internpilot.vo.recommendation.JobRecommendationGenerateResponse;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeListResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerDirectCoverageTest {

    @Test
    void analysisControllerShouldDelegateEveryEndpoint() {
        AnalysisService service = mock(AnalysisService.class);
        AnalysisController controller = new AnalysisController(service);
        when(service.match(any())).thenReturn(new AnalysisResultResponse());
        when(service.listReports(eq(1L), eq(2L), eq(60), eq(1), eq(10)))
                .thenReturn(page(new AnalysisReportListResponse()));
        when(service.getReportDetail(9L)).thenReturn(new AnalysisReportDetailResponse());

        assertThat(controller.match(new AnalysisMatchRequest()).getData()).isNotNull();
        assertThat(controller.listReports(1L, 2L, 60, 1, 10).getData().getRecords()).hasSize(1);
        assertThat(controller.getReportDetail(9L).getData()).isNotNull();
        assertThat(controller.deleteReport(9L).getCode()).isEqualTo(200);
        verify(service).deleteReport(9L);
    }

    @Test
    void analysisTaskControllerShouldDelegateEveryEndpoint() {
        AnalysisTaskService service = mock(AnalysisTaskService.class);
        AnalysisTaskController controller = new AnalysisTaskController(service);
        when(service.createTask(any())).thenReturn(new AnalysisTaskCreateResponse());
        when(service.getTaskDetail("T1")).thenReturn(new AnalysisTaskDetailResponse());
        when(service.listRunningTasks()).thenReturn(List.of(new AnalysisTaskDetailResponse()));
        when(service.cancelTask("T1")).thenReturn(new AnalysisTaskDetailResponse());
        when(service.listRecentTasks(5)).thenReturn(List.of(new AnalysisTaskDetailResponse()));

        assertThat(controller.createTask(new AnalysisTaskCreateRequest()).getData()).isNotNull();
        assertThat(controller.getTaskDetail("T1").getData()).isNotNull();
        assertThat(controller.listRunningTasks().getData()).hasSize(1);
        assertThat(controller.cancelTask("T1").getData()).isNotNull();
        assertThat(controller.listRecentTasks(5).getData()).hasSize(1);
    }

    @Test
    void jobResumeInterviewAndRecommendationControllersShouldDelegate() {
        JobService jobService = mock(JobService.class);
        JobController jobController = new JobController(jobService);
        when(jobService.create(any())).thenReturn(new JobCreateResponse());
        when(jobService.list(eq("java"), eq("backend"), eq("remote"), eq(1), eq(10)))
                .thenReturn(page(new JobListResponse()));
        when(jobService.getDetail(2L)).thenReturn(new JobDetailResponse());
        when(jobService.update(eq(2L), any(JobUpdateRequest.class))).thenReturn(true);
        when(jobService.delete(2L)).thenReturn(true);

        assertThat(jobController.create(new JobCreateRequest()).getData()).isNotNull();
        assertThat(jobController.list("java", "backend", "remote", 1, 10).getData().getRecords()).hasSize(1);
        assertThat(jobController.getDetail(2L).getData()).isNotNull();
        assertThat(jobController.update(2L, new JobUpdateRequest()).getData()).isTrue();
        assertThat(jobController.delete(2L).getData()).isTrue();

        ResumeService resumeService = mock(ResumeService.class);
        ResumeController resumeController = new ResumeController(resumeService);
        when(resumeService.upload(any(), eq("resume"))).thenReturn(new ResumeUploadResponse());
        when(resumeService.list(1, 10)).thenReturn(page(new ResumeListResponse()));
        when(resumeService.getDetail(3L)).thenReturn(new ResumeDetailResponse());
        when(resumeService.delete(3L)).thenReturn(true);
        when(resumeService.setDefault(3L)).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf".getBytes());

        assertThat(resumeController.upload(file, "resume").getData()).isNotNull();
        assertThat(resumeController.list(1, 10).getData().getRecords()).hasSize(1);
        assertThat(resumeController.getDetail(3L).getData()).isNotNull();
        assertThat(resumeController.delete(3L).getData()).isTrue();
        assertThat(resumeController.setDefault(3L).getData()).isTrue();

        InterviewQuestionService interviewService = mock(InterviewQuestionService.class);
        InterviewQuestionController interviewController = new InterviewQuestionController(interviewService);
        when(interviewService.generate(any())).thenReturn(new InterviewQuestionGenerateResponse());
        when(interviewService.list(10L, 20L, 1, 10)).thenReturn(page(new InterviewQuestionListResponse()));
        when(interviewService.getDetail(4L)).thenReturn(new InterviewQuestionDetailResponse());
        when(interviewService.delete(4L)).thenReturn(true);
        when(interviewService.regenerate(4L)).thenReturn(new InterviewQuestionGenerateResponse());

        assertThat(interviewController.generate(new InterviewQuestionGenerateRequest()).getData()).isNotNull();
        assertThat(interviewController.list(10L, 20L, 1, 10).getData().getRecords()).hasSize(1);
        assertThat(interviewController.getDetail(4L).getData()).isNotNull();
        assertThat(interviewController.delete(4L).getData()).isTrue();
        assertThat(interviewController.regenerate(4L).getData()).isNotNull();

        JobRecommendationService recommendationService = mock(JobRecommendationService.class);
        JobRecommendationController recommendationController = new JobRecommendationController(recommendationService);
        when(recommendationService.generate(any())).thenReturn(new JobRecommendationGenerateResponse());
        when(recommendationService.list(1, 10)).thenReturn(page(new JobRecommendationBatchListResponse()));
        when(recommendationService.getDetail(5L)).thenReturn(new JobRecommendationBatchDetailResponse());
        when(recommendationService.delete(5L)).thenReturn(true);

        assertThat(recommendationController.generate(new JobRecommendationGenerateRequest()).getData()).isNotNull();
        assertThat(recommendationController.list(1, 10).getData().getRecords()).hasSize(1);
        assertThat(recommendationController.getDetail(5L).getData()).isNotNull();
        assertThat(recommendationController.delete(5L).getData()).isTrue();
    }

    private static <T> PageResult<T> page(T item) {
        return new PageResult<>(List.of(item), 1L, 1L, 10L, 1L);
    }
}
