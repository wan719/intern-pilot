package com.internpilot.service.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.common.ResultCode;
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
import com.internpilot.vo.application.ApplicationFromReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApplicationFromReportWriter {

    private final AnalysisReportMapper analysisReportMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final ApplicationRecordMapper applicationRecordMapper;
    private final ResumeMapper resumeMapper;

    /** A separate proxy-bound write transaction keeps conflict recovery outside the failed transaction. */
    @Transactional
    public ApplicationFromReportResponse write(Long userId, ApplicationFromReportRequest request) {
        AnalysisReport report = analysisReportMapper.selectOne(new LambdaQueryWrapper<AnalysisReport>()
                .eq(AnalysisReport::getId, request.getReportId())
                .eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getDeleted, 0)
                .last("FOR UPDATE"));
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "来源不可用或无权访问");
        }

        JobDescription job = jobDescriptionMapper.selectOne(new LambdaQueryWrapper<JobDescription>()
                .eq(JobDescription::getId, report.getJobId())
                .eq(JobDescription::getUserId, userId)
                .eq(JobDescription::getDeleted, 0)
                .last("FOR UPDATE"));
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "来源不可用或无权访问");
        }

        ApplicationRecord existing = applicationRecordMapper.selectOne(new LambdaQueryWrapper<ApplicationRecord>()
                .eq(ApplicationRecord::getUserId, userId)
                .eq(ApplicationRecord::getJobId, job.getId())
                .eq(ApplicationRecord::getDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            return ApplicationFromReportResponse.of("EXISTING", existing.getId());
        }

        if (report.getResumeId() == null) {
            throw new BusinessException(ResultCode.CONFLICT, "报告关联简历不可用于新建投递计划");
        }
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, report.getResumeId())
                .eq(Resume::getUserId, userId)
                .eq(Resume::getDeleted, 0)
                .last("FOR UPDATE"));
        if (resume == null) {
            throw new BusinessException(ResultCode.CONFLICT, "报告关联简历不可用于新建投递计划");
        }

        ApplicationRecord record = new ApplicationRecord();
        record.setUserId(userId);
        record.setReportId(report.getId());
        record.setJobId(job.getId());
        record.setResumeId(resume.getId());
        record.setStatus("TO_APPLY");
        record.setApplyDate(null);
        record.setPriority(StringUtils.hasText(request.getPriority()) ? request.getPriority() : "MEDIUM");
        record.setNote(StringUtils.hasText(request.getNote()) ? request.getNote() : null);
        applicationRecordMapper.insert(record);
        return ApplicationFromReportResponse.of("CREATED", record.getId());
    }
}
