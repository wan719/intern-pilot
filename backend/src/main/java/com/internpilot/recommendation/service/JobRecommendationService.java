package com.internpilot.recommendation.service;

import com.internpilot.common.PageResult;
import com.internpilot.recommendation.dto.JobRecommendationGenerateRequest;
import com.internpilot.recommendation.vo.JobRecommendationBatchDetailResponse;
import com.internpilot.recommendation.vo.JobRecommendationBatchListResponse;
import com.internpilot.recommendation.vo.JobRecommendationGenerateResponse;

public interface JobRecommendationService {

    JobRecommendationGenerateResponse generate(JobRecommendationGenerateRequest request);

    PageResult<JobRecommendationBatchListResponse> list(Integer pageNum, Integer pageSize);

    JobRecommendationBatchDetailResponse getDetail(Long batchId);

    Boolean delete(Long batchId);
}