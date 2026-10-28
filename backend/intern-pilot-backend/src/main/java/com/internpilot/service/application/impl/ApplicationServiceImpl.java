package com.internpilot.service.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.common.ResultCode;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationFromReportRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.dto.application.ApplicationPriorityUpdateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.ApplicationRecord;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.enums.ApplicationPriorityEnum;
import com.internpilot.enums.ApplicationStatusEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.service.application.ApplicationService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationFromReportResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;
import com.internpilot.vo.application.ApplicationPlanContextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRecordMapper applicationRecordMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final ResumeMapper resumeMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final ApplicationFromReportWriter fromReportWriter;

    @Override
    @Transactional
    public ApplicationCreateResponse create(ApplicationCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        AnalysisReport report = null;
        if (request.getReportId() != null) {
            report = getUserReportOrThrow(request.getReportId(), currentUserId, true);
        }
        JobDescription job = getUserJobOrThrow(request.getJobId(), currentUserId, true);
        if (report != null && !request.getJobId().equals(report.getJobId())) {
            throw new BusinessException("分析报告与岗位或简历不匹配");
        }
        if (request.getResumeId() != null) {
            getUserResumeOrThrow(request.getResumeId(), currentUserId, true);
            if (report != null && !request.getResumeId().equals(report.getResumeId())) {
                throw new BusinessException("分析报告与岗位或简历不匹配");
            }
        }

        Long existingCount = applicationRecordMapper.selectCount(
                new LambdaQueryWrapper<ApplicationRecord>()
                        .eq(ApplicationRecord::getUserId, currentUserId)
                        .eq(ApplicationRecord::getJobId, request.getJobId())
                        .eq(ApplicationRecord::getDeleted, 0)
        );
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException("该岗位已存在投递记");
        }

        String status = StringUtils.hasText(request.getStatus())
                ? request.getStatus()
                : ApplicationStatusEnum.TO_APPLY.getCode();
        if (!ApplicationStatusEnum.isValid(status)) {
            throw new BusinessException("投递状态不合法");
        }

        String priority = StringUtils.hasText(request.getPriority())
                ? request.getPriority()
                : ApplicationPriorityEnum.MEDIUM.getCode();
        if (!ApplicationPriorityEnum.isValid(priority)) {
            throw new BusinessException("投递优先级不合");
        }

        ApplicationRecord record = new ApplicationRecord();
        record.setUserId(currentUserId);
        record.setJobId(job.getId());
        record.setResumeId(request.getResumeId());
        record.setReportId(request.getReportId());
        record.setStatus(status);
        record.setApplyDate(request.getApplyDate());
        record.setInterviewDate(request.getInterviewDate());
        record.setNote(request.getNote());
        record.setPriority(priority);

        applicationRecordMapper.insert(record);
        return toCreateResponse(record);
    }

    @Override
    public ApplicationPlanContextResponse getPlanContext(Long reportId) {
        if (reportId == null || reportId <= 0) {
            throw new BusinessException("报告 ID 不合法");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AnalysisReport report;
        JobDescription job;
        try {
            report = getUserReportOrThrow(reportId, currentUserId);
            job = getUserJobOrThrow(report.getJobId(), currentUserId);
        } catch (BusinessException exception) {
            throw new BusinessException(ResultCode.NOT_FOUND, "来源不可用或无权访问");
        }
        ApplicationPlanContextResponse response = new ApplicationPlanContextResponse();
        response.setReportId(reportId);

        ApplicationRecord existing = applicationRecordMapper.selectOne(
                new LambdaQueryWrapper<ApplicationRecord>()
                        .eq(ApplicationRecord::getUserId, currentUserId)
                        .eq(ApplicationRecord::getJobId, job.getId())
                        .eq(ApplicationRecord::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            response.setOutcome("EXISTING");
            response.setApplicationId(existing.getId());
            return response;
        }

        ApplicationPlanContextResponse.JobSummary jobSummary = new ApplicationPlanContextResponse.JobSummary();
        jobSummary.setId(job.getId());
        jobSummary.setCompanyName(job.getCompanyName());
        jobSummary.setJobTitle(job.getJobTitle());
        response.setJob(jobSummary);

        Resume resume = report.getResumeId() == null ? null : resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, report.getResumeId())
                        .eq(Resume::getUserId, currentUserId)
                        .eq(Resume::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (resume == null) {
            response.setOutcome("RESUME_UNAVAILABLE");
            response.setReason("报告关联简历不可用于新建投递计划");
            return response;
        }

        response.setOutcome("READY");
        ApplicationPlanContextResponse.ResumeSummary resumeSummary = new ApplicationPlanContextResponse.ResumeSummary();
        resumeSummary.setId(resume.getId());
        resumeSummary.setResumeName(resume.getResumeName());
        response.setResume(resumeSummary);
        return response;
    }

    @Override
    public ApplicationFromReportResponse createFromReport(ApplicationFromReportRequest request) {
        if (request == null || request.getReportId() == null || request.getReportId() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报告 ID 必须为正整数");
        }
        if (request.getNote() != null && request.getNote().codePointCount(0, request.getNote().length()) > 2000) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "备注不能超过 2000 个字符");
        }
        if (StringUtils.hasText(request.getPriority()) && !ApplicationPriorityEnum.isValid(request.getPriority())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "投递优先级不合法");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        try {
            return fromReportWriter.write(currentUserId, request);
        } catch (DuplicateKeyException exception) {
            String detail = exception.getMostSpecificCause().getMessage();
            if (detail == null || !detail.contains("uk_app_user_active_job")) {
                throw exception;
            }
            // This read occurs only after the writer's transaction has finished/rolled back.
            AnalysisReport report = getUserReportOrThrow(request.getReportId(), currentUserId);
            JobDescription job = getUserJobOrThrow(report.getJobId(), currentUserId);
            ApplicationRecord existing = applicationRecordMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationRecord>()
                            .eq(ApplicationRecord::getUserId, currentUserId)
                            .eq(ApplicationRecord::getJobId, job.getId())
                            .eq(ApplicationRecord::getDeleted, 0)
                            .last("LIMIT 1")
            );
            if (existing == null) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "投递结果待确认，请稍后刷新或重试");
            }
            return ApplicationFromReportResponse.of("EXISTING", existing.getId());
        }
    }

    @Override
    public PageResult<ApplicationListResponse> list(
            String status,
            String keyword,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (StringUtils.hasText(status) && !ApplicationStatusEnum.isValid(status)) {
            throw new BusinessException("投递状态不合法");
        }

        LambdaQueryWrapper<ApplicationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationRecord::getUserId, currentUserId)
                .eq(ApplicationRecord::getDeleted, 0);

        if (StringUtils.hasText(status)) {
            wrapper.eq(ApplicationRecord::getStatus, status);
        }

        wrapper.orderByDesc(ApplicationRecord::getUpdatedAt);

        Page<ApplicationRecord> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<ApplicationRecord> resultPage = applicationRecordMapper.selectPage(page, wrapper);

        List<ApplicationListResponse> records = resultPage.getRecords().stream()
                .map(this::toListResponse)
                .filter(item -> matchKeyword(item, keyword))
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public ApplicationDetailResponse getDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);
        return toDetailResponse(record);
    }

    @Override
    @Transactional
    public Boolean updateStatus(Long id, ApplicationStatusUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        if (!ApplicationStatusEnum.isValid(request.getStatus())) {
            throw new BusinessException("投递状态不合法");
        }

        record.setStatus(request.getStatus());
        applicationRecordMapper.updateById(record);
        return true;
    }

    @Override
    @Transactional
    public Boolean updatePriority(Long id, ApplicationPriorityUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        if (!ApplicationPriorityEnum.isValid(request.getPriority())) {
            throw new BusinessException("投递优先级不合法");
        }

        // Reuse the current-user/active-record ownership check. Never bind another field from the request.
        record.setPriority(request.getPriority());
        applicationRecordMapper.updateById(record);
        return true;
    }

    @Override
    @Transactional
    public Boolean updateNote(Long id, ApplicationNoteUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        record.setNote(request.getNote());
        record.setReview(request.getReview());
        record.setInterviewDate(request.getInterviewDate());
        applicationRecordMapper.updateById(record);
        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        applicationRecordMapper.deleteById(record.getId());
        return true;
    }

    private ApplicationRecord getUserApplicationOrThrow(Long id, Long userId) {
        ApplicationRecord record = applicationRecordMapper.selectOne(
                new LambdaQueryWrapper<ApplicationRecord>()
                        .eq(ApplicationRecord::getId, id)
                        .eq(ApplicationRecord::getUserId, userId)
                        .eq(ApplicationRecord::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (record == null) {
            throw new BusinessException("投递记录不存在或无权限访问");
        }
        return record;
    }

    private JobDescription getUserJobOrThrow(Long jobId, Long userId) {
        return getUserJobOrThrow(jobId, userId, false);
    }

    private JobDescription getUserJobOrThrow(Long jobId, Long userId, boolean forUpdate) {
        JobDescription job = jobDescriptionMapper.selectOne(
                new LambdaQueryWrapper<JobDescription>()
                        .eq(JobDescription::getId, jobId)
                        .eq(JobDescription::getUserId, userId)
                        .eq(JobDescription::getDeleted, 0)
                        .last(forUpdate ? "FOR UPDATE" : "LIMIT 1")
        );

        if (job == null) {
            throw new BusinessException("岗位不存在或无权限访");
        }
        return job;
    }

    private Resume getUserResumeOrThrow(Long resumeId, Long userId) {
        return getUserResumeOrThrow(resumeId, userId, false);
    }

    private Resume getUserResumeOrThrow(Long resumeId, Long userId, boolean forUpdate) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getDeleted, 0)
                        .last(forUpdate ? "FOR UPDATE" : "LIMIT 1")
        );

        if (resume == null) {
            throw new BusinessException("简历不存在或无权限访问");
        }
        return resume;
    }

    private AnalysisReport getUserReportOrThrow(Long reportId, Long userId) {
        return getUserReportOrThrow(reportId, userId, false);
    }

    private AnalysisReport getUserReportOrThrow(Long reportId, Long userId, boolean forUpdate) {
        AnalysisReport report = analysisReportMapper.selectOne(
                new LambdaQueryWrapper<AnalysisReport>()
                        .eq(AnalysisReport::getId, reportId)
                        .eq(AnalysisReport::getUserId, userId)
                        .eq(AnalysisReport::getDeleted, 0)
                        .last(forUpdate ? "FOR UPDATE" : "LIMIT 1")
        );

        if (report == null) {
            throw new BusinessException("分析报告不存在或无权限访");
        }
        return report;
    }

    private ApplicationCreateResponse toCreateResponse(ApplicationRecord record) {
        ApplicationCreateResponse response = new ApplicationCreateResponse();
        response.setApplicationId(record.getId());
        response.setJobId(record.getJobId());
        response.setResumeId(record.getResumeId());
        response.setReportId(record.getReportId());
        response.setStatus(record.getStatus());
        response.setPriority(record.getPriority());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private ApplicationListResponse toListResponse(ApplicationRecord record) {
        ApplicationListResponse response = new ApplicationListResponse();
        response.setApplicationId(record.getId());
        response.setJobId(record.getJobId());
        response.setResumeId(record.getResumeId());
        response.setReportId(record.getReportId());
        response.setStatus(record.getStatus());
        response.setPriority(record.getPriority());
        response.setApplyDate(record.getApplyDate());
        response.setInterviewDate(record.getInterviewDate());
        response.setNote(record.getNote());
        response.setUpdatedAt(record.getUpdatedAt());

        JobDescription job = jobDescriptionMapper.selectById(record.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        return response;
    }

    private ApplicationDetailResponse toDetailResponse(ApplicationRecord record) {
        ApplicationDetailResponse response = new ApplicationDetailResponse();
        response.setApplicationId(record.getId());
        response.setJobId(record.getJobId());
        response.setResumeId(record.getResumeId());
        response.setReportId(record.getReportId());
        response.setStatus(record.getStatus());
        response.setPriority(record.getPriority());
        response.setApplyDate(record.getApplyDate());
        response.setInterviewDate(record.getInterviewDate());
        response.setNote(record.getNote());
        response.setReview(record.getReview());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        JobDescription job = jobDescriptionMapper.selectById(record.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        if (record.getResumeId() != null) {
            Resume resume = resumeMapper.selectById(record.getResumeId());
            if (resume != null) {
                response.setResumeName(resume.getResumeName());
            }
        }

        if (record.getReportId() != null) {
            AnalysisReport report = analysisReportMapper.selectById(record.getReportId());
            if (report != null) {
                response.setMatchScore(report.getMatchScore());
                response.setMatchLevel(report.getMatchLevel());
            }
        }

        return response;
    }

    private boolean matchKeyword(ApplicationListResponse item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase();
        return containsIgnoreCase(item.getCompanyName(), lowerKeyword)
                || containsIgnoreCase(item.getJobTitle(), lowerKeyword);
    }

    private boolean containsIgnoreCase(String source, String lowerKeyword) {
        return source != null && source.toLowerCase().contains(lowerKeyword);
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : pageSize.longValue();
    }
}
