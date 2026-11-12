package com.internpilot.recommendation.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.recommendation.entity.JobRecommendationItem;

@Mapper
public interface JobRecommendationItemMapper extends BaseMapper<JobRecommendationItem>{
    
}
