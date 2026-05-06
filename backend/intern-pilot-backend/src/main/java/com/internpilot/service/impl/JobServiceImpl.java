package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.entity.JobDescription;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.service.JobService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobDescriptionMapper jobDescriptionMapper;

    @Override
    @Transactional
    public JobCreateResponse create(JobCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        JobDescription job = new JobDescription();
        job.setUserId(currentUserId);
        job.setCompanyName(request.getCompanyName());
        job.setJobTitle(request.getJobTitle());
        job.setJobType(request.getJobType());
        job.setLocation(request.getLocation());
        job.setSourcePlatform(request.getSourcePlatform());
        job.setJobUrl(request.getJobUrl());
        job.setJdContent(request.getJdContent());
        job.setSkillRequirements(request.getSkillRequirements());
        job.setSalaryRange(request.getSalaryRange());
        job.setWorkDaysPerWeek(request.getWorkDaysPerWeek());
        job.setInternshipDuration(request.getInternshipDuration());

        jobDescriptionMapper.insert(job);
        return toCreateResponse(job);
    }

    @Override
    public PageResult<JobListResponse> list(
            String keyword,
            String jobType,
            String location,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<JobDescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobDescription::getUserId, currentUserId)
                .eq(JobDescription::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(JobDescription::getCompanyName, keyword)
                    .or()
                    .like(JobDescription::getJobTitle, keyword));
        }
        if (StringUtils.hasText(jobType)) {
            wrapper.eq(JobDescription::getJobType, jobType);
        }
        if (StringUtils.hasText(location)) {
            wrapper.like(JobDescription::getLocation, location);
        }

        wrapper.orderByDesc(JobDescription::getCreatedAt);

        Page<JobDescription> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<JobDescription> resultPage = jobDescriptionMapper.selectPage(page, wrapper);

        List<JobListResponse> records = resultPage.getRecords().stream()
                .map(this::toListResponse)
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
    public JobDetailResponse getDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobDescription job = getUserJobOrThrow(id, currentUserId);
        return toDetailResponse(job);
    }

    @Override
    @Transactional
    public Boolean update(Long id, JobUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobDescription job = getUserJobOrThrow(id, currentUserId);

        job.setCompanyName(request.getCompanyName());
        job.setJobTitle(request.getJobTitle());
        job.setJobType(request.getJobType());
        job.setLocation(request.getLocation());
        job.setSourcePlatform(request.getSourcePlatform());
        job.setJobUrl(request.getJobUrl());
        job.setJdContent(request.getJdContent());
        job.setSkillRequirements(request.getSkillRequirements());
        job.setSalaryRange(request.getSalaryRange());
        job.setWorkDaysPerWeek(request.getWorkDaysPerWeek());
        job.setInternshipDuration(request.getInternshipDuration());

        jobDescriptionMapper.updateById(job);
        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobDescription job = getUserJobOrThrow(id, currentUserId);
        job.setDeleted(1);
        jobDescriptionMapper.updateById(job);
        return true;
    }

    private JobDescription getUserJobOrThrow(Long jobId, Long userId) {
        JobDescription job = jobDescriptionMapper.selectOne(
                new LambdaQueryWrapper<JobDescription>()
                        .eq(JobDescription::getId, jobId)
                        .eq(JobDescription::getUserId, userId)
                        .eq(JobDescription::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (job == null) {
            throw new BusinessException("岗位不存在或无权限访问");
        }
        return job;
    }

    private JobCreateResponse toCreateResponse(JobDescription job) {
        JobCreateResponse response = new JobCreateResponse();
        response.setJobId(job.getId());
        response.setCompanyName(job.getCompanyName());
        response.setJobTitle(job.getJobTitle());
        response.setJobType(job.getJobType());
        response.setLocation(job.getLocation());
        response.setSourcePlatform(job.getSourcePlatform());
        response.setCreatedAt(job.getCreatedAt());
        return response;
    }

    private JobListResponse toListResponse(JobDescription job) {
        JobListResponse response = new JobListResponse();
        response.setJobId(job.getId());
        response.setCompanyName(job.getCompanyName());
        response.setJobTitle(job.getJobTitle());
        response.setJobType(job.getJobType());
        response.setLocation(job.getLocation());
        response.setSourcePlatform(job.getSourcePlatform());
        response.setSalaryRange(job.getSalaryRange());
        response.setWorkDaysPerWeek(job.getWorkDaysPerWeek());
        response.setInternshipDuration(job.getInternshipDuration());
        response.setCreatedAt(job.getCreatedAt());
        return response;
    }

    private JobDetailResponse toDetailResponse(JobDescription job) {
        JobDetailResponse response = new JobDetailResponse();
        response.setJobId(job.getId());
        response.setCompanyName(job.getCompanyName());
        response.setJobTitle(job.getJobTitle());
        response.setJobType(job.getJobType());
        response.setLocation(job.getLocation());
        response.setSourcePlatform(job.getSourcePlatform());
        response.setJobUrl(job.getJobUrl());
        response.setJdContent(job.getJdContent());
        response.setSkillRequirements(job.getSkillRequirements());
        response.setSalaryRange(job.getSalaryRange());
        response.setWorkDaysPerWeek(job.getWorkDaysPerWeek());
        response.setInternshipDuration(job.getInternshipDuration());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : pageSize.longValue();
    }
}
