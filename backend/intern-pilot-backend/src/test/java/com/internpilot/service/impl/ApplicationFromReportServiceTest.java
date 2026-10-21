package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.ApplicationRecord;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.dto.application.ApplicationFromReportRequest;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.service.application.impl.ApplicationServiceImpl;
import com.internpilot.service.application.impl.ApplicationFromReportWriter;
import com.internpilot.vo.application.ApplicationFromReportResponse;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.internpilot.exception.BusinessException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationFromReportServiceTest {

    @Mock private ApplicationRecordMapper applicationRecordMapper;
    @Mock private JobDescriptionMapper jobDescriptionMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private AnalysisReportMapper analysisReportMapper;
    @Mock private ApplicationFromReportWriter writer;
    @InjectMocks private ApplicationServiceImpl applicationService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void contextReturnsOwnedReportJobAndResumeWithoutWriting() {
        login(1L);
        AnalysisReport report = new AnalysisReport();
        report.setId(30L);
        report.setJobId(10L);
        report.setResumeId(20L);
        JobDescription job = new JobDescription();
        job.setId(10L);
        job.setCompanyName("示例公司");
        job.setJobTitle("Java 实习生");
        Resume resume = new Resume();
        resume.setId(20L);
        resume.setResumeName("后端实习简历");
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);
        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume);

        var result = applicationService.getPlanContext(30L);

        assertEquals("READY", result.getOutcome());
        assertEquals(30L, result.getReportId());
        assertEquals(10L, result.getJob().getId());
        assertEquals("示例公司", result.getJob().getCompanyName());
        assertEquals(20L, result.getResume().getId());
        assertEquals("后端实习简历", result.getResume().getResumeName());
        assertNull(result.getApplicationId());
        verify(applicationRecordMapper, org.mockito.Mockito.never()).insert(any(ApplicationRecord.class));
    }

    @Test
    void existingRecordWinsEvenWhenReportResumeWasDeleted() {
        login(1L);
        AnalysisReport report = new AnalysisReport();
        report.setId(30L);
        report.setJobId(10L);
        report.setResumeId(20L);
        JobDescription job = new JobDescription();
        job.setId(10L);
        ApplicationRecord existing = new ApplicationRecord();
        existing.setId(99L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);
        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        var result = applicationService.getPlanContext(30L);

        assertEquals("EXISTING", result.getOutcome());
        assertEquals(99L, result.getApplicationId());
        assertNull(result.getResume());
        assertNull(result.getJob());
        verify(resumeMapper, org.mockito.Mockito.never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void missingResumeReturnsOwnedJobForOrdinaryCreationWithoutStaleResume() {
        login(1L);
        AnalysisReport report = new AnalysisReport();
        report.setId(30L);
        report.setJobId(10L);
        report.setResumeId(20L);
        JobDescription job = new JobDescription();
        job.setId(10L);
        job.setCompanyName("示例公司");
        job.setJobTitle("Java 实习生");
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);

        var result = applicationService.getPlanContext(30L);

        assertEquals("RESUME_UNAVAILABLE", result.getOutcome());
        assertNotNull(result.getReason());
        assertEquals(10L, result.getJob().getId());
        assertEquals("示例公司", result.getJob().getCompanyName());
        assertEquals("Java 实习生", result.getJob().getJobTitle());
        assertNull(result.getResume());
    }

    @Test
    void inaccessibleReportDoesNotQueryOtherResources() {
        login(1L);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> applicationService.getPlanContext(30L)).getCode());
        verify(jobDescriptionMapper, org.mockito.Mockito.never()).selectOne(any(LambdaQueryWrapper.class));
        verify(applicationRecordMapper, org.mockito.Mockito.never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void blankReportIdNeverCreatesRecord() {
        login(1L);
        ApplicationFromReportRequest request = new ApplicationFromReportRequest();
        request.setReportId(0L);
        assertThrows(BusinessException.class, () -> applicationService.createFromReport(request));
        verify(applicationRecordMapper, org.mockito.Mockito.never()).insert(any(ApplicationRecord.class));
    }

    @Test
    void noteLimitCountsUnicodeCodePoints() {
        login(1L);
        ApplicationFromReportRequest request = new ApplicationFromReportRequest();
        request.setReportId(30L);
        request.setNote("🎯".repeat(2001));
        assertThrows(BusinessException.class, () -> applicationService.createFromReport(request));
        verify(applicationRecordMapper, org.mockito.Mockito.never()).insert(any(ApplicationRecord.class));
    }

    @Test
    void exactlyTwoThousandUnicodeCodePointsCanBeSubmitted() {
        login(1L);
        ApplicationFromReportRequest request = new ApplicationFromReportRequest();
        request.setReportId(30L);
        request.setNote("🎯".repeat(2000));
        when(writer.write(1L, request)).thenReturn(ApplicationFromReportResponse.of("CREATED", 77L));

        var result = applicationService.createFromReport(request);

        assertEquals("CREATED", result.getOutcome());
        assertEquals(77L, result.getApplicationId());
        verify(writer).write(1L, request);
    }

    @Test
    void targetedUniqueConflictReadsWinnerAfterFailedWrite() {
        login(1L);
        ApplicationFromReportRequest request = new ApplicationFromReportRequest();
        request.setReportId(30L);
        when(writer.write(1L, request)).thenThrow(new DuplicateKeyException("Duplicate entry for key uk_app_user_active_job"));
        AnalysisReport report = new AnalysisReport();
        report.setJobId(10L);
        JobDescription job = new JobDescription();
        job.setId(10L);
        ApplicationRecord winner = new ApplicationRecord();
        winner.setId(88L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);
        when(applicationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(winner);

        var result = applicationService.createFromReport(request);

        assertEquals("EXISTING", result.getOutcome());
        assertEquals(88L, result.getApplicationId());
        verify(applicationRecordMapper, org.mockito.Mockito.never()).insert(any(ApplicationRecord.class));
    }

    @Test
    void targetedConflictWithoutReadableWinnerIsRecoverableError() {
        login(1L);
        ApplicationFromReportRequest request = new ApplicationFromReportRequest();
        request.setReportId(30L);
        when(writer.write(1L, request)).thenThrow(new DuplicateKeyException("Duplicate entry for key uk_app_user_active_job"));
        AnalysisReport report = new AnalysisReport();
        report.setJobId(10L);
        JobDescription job = new JobDescription();
        job.setId(10L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);

        assertEquals(500, assertThrows(BusinessException.class,
                () -> applicationService.createFromReport(request)).getCode());
    }

    @Test
    void unrelatedUniqueConflictIsNotReinterpretedAsExisting() {
        login(1L);
        ApplicationFromReportRequest request = new ApplicationFromReportRequest();
        request.setReportId(30L);
        when(writer.write(1L, request)).thenThrow(new DuplicateKeyException("Duplicate entry for key other_unique"));

        assertThrows(DuplicateKeyException.class, () -> applicationService.createFromReport(request));
        verify(analysisReportMapper, org.mockito.Mockito.never()).selectOne(any(LambdaQueryWrapper.class));
    }

    private void login(Long userId) {
        CustomUserDetails user = new CustomUserDetails(userId, "testuser", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
