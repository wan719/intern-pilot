package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.application.ApplicationFromReportRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.ApplicationRecord;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.service.application.impl.ApplicationFromReportWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationFromReportWriterTest {
    @Mock AnalysisReportMapper reportMapper;
    @Mock JobDescriptionMapper jobMapper;
    @Mock ApplicationRecordMapper applicationMapper;
    @Mock ResumeMapper resumeMapper;
    @InjectMocks ApplicationFromReportWriter writer;

    @Test
    void createsOnlyPendingPlanFromOwnedReport() {
        AnalysisReport report = report();
        JobDescription job = job();
        Resume resume = resume();
        when(reportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume);
        doAnswer(invocation -> {
            ApplicationRecord record = invocation.getArgument(0);
            record.setId(77L);
            return 1;
        }).when(applicationMapper).insert(any(ApplicationRecord.class));
        ApplicationFromReportRequest request = request();
        request.setNote("准备完善项目经历");

        var response = writer.write(1L, request);

        assertEquals("CREATED", response.getOutcome());
        assertEquals(77L, response.getApplicationId());
        ArgumentCaptor<ApplicationRecord> inserted = ArgumentCaptor.forClass(ApplicationRecord.class);
        verify(applicationMapper).insert(inserted.capture());
        ApplicationRecord record = inserted.getValue();
        assertEquals(1L, record.getUserId());
        assertEquals(30L, record.getReportId());
        assertEquals(10L, record.getJobId());
        assertEquals(20L, record.getResumeId());
        assertEquals("TO_APPLY", record.getStatus());
        assertEquals("MEDIUM", record.getPriority());
        assertEquals("准备完善项目经历", record.getNote());
        assertNull(record.getApplyDate());
    }

    @Test
    void existingRecordIsReturnedWithoutResumeLookupOrMutation() {
        when(reportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report());
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job());
        ApplicationRecord existing = new ApplicationRecord();
        existing.setId(88L);
        existing.setStatus("INTERVIEWING");
        existing.setNote("原备注");
        when(applicationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        var response = writer.write(1L, request());

        assertEquals("EXISTING", response.getOutcome());
        assertEquals(88L, response.getApplicationId());
        verify(resumeMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(applicationMapper, never()).insert(any(ApplicationRecord.class));
        verify(applicationMapper, never()).updateById(any(ApplicationRecord.class));
        assertEquals("原备注", existing.getNote());
    }

    @Test
    void unavailableResumeCannotCreatePlan() {
        when(reportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report());
        when(jobMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job());
        assertEquals(409, assertThrows(BusinessException.class, () -> writer.write(1L, request())).getCode());
        verify(applicationMapper, never()).insert(any(ApplicationRecord.class));
    }

    @Test
    void unavailableReportDoesNotExposeJobOrResume() {
        assertEquals(404, assertThrows(BusinessException.class, () -> writer.write(1L, request())).getCode());
        verify(jobMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(resumeMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    private ApplicationFromReportRequest request() {
        ApplicationFromReportRequest request = new ApplicationFromReportRequest();
        request.setReportId(30L);
        return request;
    }

    private AnalysisReport report() {
        AnalysisReport report = new AnalysisReport();
        report.setId(30L);
        report.setJobId(10L);
        report.setResumeId(20L);
        return report;
    }

    private JobDescription job() {
        JobDescription job = new JobDescription();
        job.setId(10L);
        return job;
    }

    private Resume resume() {
        Resume resume = new Resume();
        resume.setId(20L);
        return resume;
    }
}
