package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.recommendation.JobRecommendationGenerateRequest;
import com.internpilot.vo.recommendation.JobRecommendationBatchDetailResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchListResponse;
import com.internpilot.vo.recommendation.JobRecommendationGenerateResponse;

public interface JobRecommendationService {

    JobRecommendationGenerateResponse generate(JobRecommendationGenerateRequest request);

    PageResult<JobRecommendationBatchListResponse> list(Integer pageNum, Integer pageSize);

    JobRecommendationBatchDetailResponse getDetail(Long batchId);

    Boolean delete(Long batchId);
}