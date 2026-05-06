package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;

public interface JobService {

    JobCreateResponse create(JobCreateRequest request);

    PageResult<JobListResponse> list(
            String keyword,
            String jobType,
            String location,
            Integer pageNum,
            Integer pageSize
    );

    JobDetailResponse getDetail(Long id);

    Boolean update(Long id, JobUpdateRequest request);

    Boolean delete(Long id);
}
