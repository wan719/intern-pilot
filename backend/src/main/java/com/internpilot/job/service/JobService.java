package com.internpilot.job.service;

import com.internpilot.common.PageResult;
import com.internpilot.job.dto.JobCreateRequest;
import com.internpilot.job.dto.JobUpdateRequest;
import com.internpilot.job.vo.JobCreateResponse;
import com.internpilot.job.vo.JobDetailResponse;
import com.internpilot.job.vo.JobListResponse;

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
