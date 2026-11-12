package com.internpilot.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.recommendation.entity.JobRecommendationBatch;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobRecommendationBatchMapper extends BaseMapper<JobRecommendationBatch> {
}