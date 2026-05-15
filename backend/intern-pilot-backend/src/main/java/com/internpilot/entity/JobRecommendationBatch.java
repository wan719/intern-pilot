package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_recommendation_batch")//职位推荐批次表，每次职位推荐会生成一个批次，批次包含多个职位推荐结果
@Schema(description = "职位推荐批次实体类，包含职位推荐批次的详细信息和关联的职位推荐结果")
public class JobRecommendationBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long resumeVersionId;

    private String title;

    private Integer jobCount;

    private Integer recommendedCount;

    private String strategy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}