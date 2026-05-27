package com.internpilot.service.impl;

import com.internpilot.service.application.impl.ApplicationServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.ApplicationRecord;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRecordMapper applicationRecordMapper;

    @Mock
    private JobDescriptionMapper jobDescriptionMapper;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private AnalysisReportMapper analysisReportMapper;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldBindCurrentUser() {
        mockLoginUser(1L);

        JobDescription job = new JobDescription();
        job.setId(10L);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);

        Resume resume = new Resume();
        resume.setId(20L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume);

        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setJobId(10L);
        request.setResumeId(20L);
        request.setStatus("TO_APPLY");
        request.setPriority("HIGH");
        request.setApplyDate(LocalDate.of(2026, 5, 6));

        doAnswer(invocation -> {
            ApplicationRecord record = invocation.getArgument(0);
            record.setId(100L);
            record.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(applicationRecordMapper).insert(any(ApplicationRecord.class));

        ApplicationCreateResponse response = applicationService.create(request);

        assertNotNull(response);
        assertEquals(100L, response.getApplicationId());
        assertEquals(10L, response.getJobId());

        ArgumentCaptor<ApplicationRecord> captor = ArgumentCaptor.forClass(ApplicationRecord.class);
        verify(applicationRecordMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("TO_APPLY", captor.getValue().getStatus());
    }

    @Test
    void createShouldUseDefaultStatusAndPriorityWhenOptionalFieldsBlank() {
        mockLoginUser(1L);
        JobDescription job = new JobDescription();
        job.setId(10L);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);
        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ApplicationRecord record = invocation.getArgument(0);
            record.setId(101L);
            return 1;
        }).when(applicationRecordMapper).insert(any(ApplicationRecord.class));

        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setJobId(10L);
        request.setStatus(" ");
        request.setPriority("");

        ApplicationCreateResponse response = applicationService.create(request);

        assertEquals(101L, response.getApplicationId());
        ArgumentCaptor<ApplicationRecord> captor = ArgumentCaptor.forClass(ApplicationRecord.class);
        verify(applicationRecordMapper).insert(captor.capture());
        assertEquals("TO_APPLY", captor.getValue().getStatus());
        assertEquals("MEDIUM", captor.getValue().getPriority());
    }

    @Test
    void createShouldRejectDuplicateInvalidInputsAndMissingOwnedObjects() {
        mockLoginUser(1L);
        JobDescription job = new JobDescription();
        job.setId(10L);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);

        ApplicationCreateRequest duplicate = new ApplicationCreateRequest();
        duplicate.setJobId(10L);
        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThrows(BusinessException.class, () -> applicationService.create(duplicate));

        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        ApplicationCreateRequest invalidStatus = new ApplicationCreateRequest();
        invalidStatus.setJobId(10L);
        invalidStatus.setStatus("BAD");
        assertThrows(BusinessException.class, () -> applicationService.create(invalidStatus));

        ApplicationCreateRequest invalidPriority = new ApplicationCreateRequest();
        invalidPriority.setJobId(10L);
        invalidPriority.setPriority("BAD");
        assertThrows(BusinessException.class, () -> applicationService.create(invalidPriority));

        ApplicationCreateRequest missingResume = new ApplicationCreateRequest();
        missingResume.setJobId(10L);
        missingResume.setResumeId(20L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> applicationService.create(missingResume));

        ApplicationCreateRequest missingReport = new ApplicationCreateRequest();
        missingReport.setJobId(10L);
        missingReport.setReportId(30L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> applicationService.create(missingReport));
    }

    @Test
    void createShouldRejectMissingJob() {
        mockLoginUser(1L);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setJobId(10L);

        assertThrows(BusinessException.class, () -> applicationService.create(request));
    }

    @Test
    void listShouldReturnPagedApplications() {
        mockLoginUser(1L);

        ApplicationRecord record = new ApplicationRecord();
        record.setId(1L);
        record.setJobId(10L);
        record.setResumeId(20L);
        record.setStatus("APPLIED");
        record.setPriority("HIGH");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        Page<ApplicationRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1L);

        when(applicationRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        JobDescription job = new JobDescription();
        job.setId(10L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        when(jobDescriptionMapper.selectById(10L)).thenReturn(job);

        PageResult<ApplicationListResponse> result = applicationService.list(null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("腾讯", result.getRecords().get(0).getCompanyName());
        assertEquals("Java后端开发实习生", result.getRecords().get(0).getJobTitle());
    }

    @Test
    void listShouldFilterByKeywordAndNormalizePageArguments() {
        mockLoginUser(1L);

        ApplicationRecord javaRecord = new ApplicationRecord();
        javaRecord.setId(1L);
        javaRecord.setJobId(10L);
        javaRecord.setStatus("APPLIED");
        javaRecord.setPriority("HIGH");

        ApplicationRecord qaRecord = new ApplicationRecord();
        qaRecord.setId(2L);
        qaRecord.setJobId(11L);
        qaRecord.setStatus("APPLIED");
        qaRecord.setPriority("LOW");

        Page<ApplicationRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(javaRecord, qaRecord));
        page.setTotal(2L);
        when(applicationRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        JobDescription javaJob = new JobDescription();
        javaJob.setId(10L);
        javaJob.setCompanyName("Acme");
        javaJob.setJobTitle("Java Backend");
        JobDescription qaJob = new JobDescription();
        qaJob.setId(11L);
        qaJob.setCompanyName("Beta");
        qaJob.setJobTitle("QA");
        when(jobDescriptionMapper.selectById(10L)).thenReturn(javaJob);
        when(jobDescriptionMapper.selectById(11L)).thenReturn(qaJob);

        PageResult<ApplicationListResponse> result = applicationService.list("APPLIED", "java", 0, 0);

        assertEquals(1, result.getRecords().size());
        assertEquals("Java Backend", result.getRecords().get(0).getJobTitle());
    }

    @Test
    void listShouldThrowExceptionWhenInvalidStatus() {
        mockLoginUser(1L);

        assertThrows(BusinessException.class, () -> {
            applicationService.list("INVALID_STATUS", null, 1, 10);
        });
    }

    @Test
    void getDetailShouldReturnFullDetail() {
        mockLoginUser(1L);

        ApplicationRecord record = new ApplicationRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setJobId(10L);
        record.setResumeId(20L);
        record.setReportId(30L);
        record.setStatus("APPLIED");
        record.setPriority("HIGH");
        record.setNote("准备投递");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        JobDescription job = new JobDescription();
        job.setId(10L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        when(jobDescriptionMapper.selectById(10L)).thenReturn(job);

        Resume resume = new Resume();
        resume.setId(20L);
        resume.setResumeName("我的简历");
        when(resumeMapper.selectById(20L)).thenReturn(resume);

        ApplicationDetailResponse response = applicationService.getDetail(1L);

        assertNotNull(response);
        assertEquals("腾讯", response.getCompanyName());
        assertEquals("Java后端开发实习生", response.getJobTitle());
        assertEquals("我的简历", response.getResumeName());
    }

    @Test
    void getDetailShouldIncludeAnalysisReportWhenPresentAndIgnoreMissingRelations() {
        mockLoginUser(1L);

        ApplicationRecord record = new ApplicationRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setJobId(10L);
        record.setResumeId(20L);
        record.setReportId(30L);
        record.setStatus("APPLIED");
        record.setPriority("HIGH");
        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        AnalysisReport report = new AnalysisReport();
        report.setId(30L);
        report.setMatchScore(86);
        report.setMatchLevel("HIGH");
        when(jobDescriptionMapper.selectById(10L)).thenReturn(null);
        when(resumeMapper.selectById(20L)).thenReturn(null);
        when(analysisReportMapper.selectById(30L)).thenReturn(report);

        ApplicationDetailResponse response = applicationService.getDetail(1L);

        assertEquals(86, response.getMatchScore());
        assertEquals("HIGH", response.getMatchLevel());
    }

    @Test
    void getDetailShouldThrowExceptionWhenNotFound() {
        mockLoginUser(1L);

        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            applicationService.getDetail(999L);
        });
    }

    @Test
    void updateStatusShouldUpdateRecord() {
        mockLoginUser(1L);

        ApplicationRecord record = new ApplicationRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setStatus("TO_APPLY");

        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        when(applicationRecordMapper.updateById(any(ApplicationRecord.class))).thenReturn(1);

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPLIED");

        Boolean result = applicationService.updateStatus(1L, request);

        assertEquals(true, result);
        ArgumentCaptor<ApplicationRecord> captor = ArgumentCaptor.forClass(ApplicationRecord.class);
        verify(applicationRecordMapper).updateById(captor.capture());
        assertEquals("APPLIED", captor.getValue().getStatus());
    }

    @Test
    void updateStatusShouldRejectInvalidStatus() {
        mockLoginUser(1L);

        ApplicationRecord record = new ApplicationRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("BAD");

        assertThrows(BusinessException.class, () -> applicationService.updateStatus(1L, request));
    }

    @Test
    void updateNoteShouldUpdateRecord() {
        mockLoginUser(1L);

        ApplicationRecord record = new ApplicationRecord();
        record.setId(1L);
        record.setUserId(1L);

        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        when(applicationRecordMapper.updateById(any(ApplicationRecord.class))).thenReturn(1);

        ApplicationNoteUpdateRequest request = new ApplicationNoteUpdateRequest();
        request.setNote("已完成一面");
        request.setReview("Spring Security需要复习");

        Boolean result = applicationService.updateNote(1L, request);

        assertEquals(true, result);
        ArgumentCaptor<ApplicationRecord> captor = ArgumentCaptor.forClass(ApplicationRecord.class);
        verify(applicationRecordMapper).updateById(captor.capture());
        assertEquals("已完成一面", captor.getValue().getNote());
        assertEquals("Spring Security需要复习", captor.getValue().getReview());
    }

    @Test
    void deleteShouldSoftDeleteRecord() {
        mockLoginUser(1L);

        ApplicationRecord record = new ApplicationRecord();
        record.setId(1L);
        record.setUserId(1L);

        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        Boolean result = applicationService.delete(1L);

        assertEquals(true, result);
        verify(applicationRecordMapper).deleteById(1L);
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "testuser", "USER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }
}
