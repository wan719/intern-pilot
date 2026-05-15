package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_recommendation_item")//职位推荐结果表，每条记录对应一个职位推荐结果，关联到职位推荐批次表
@Schema(description = "职位推荐结果实体类，包含职位推荐结果的详细信息和关联的职位推荐批次")
public class JobRecommendationItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Long userId;

    private Long jobId;

    private Long analysisReportId;

    private Integer recommendationScore;

    private String recommendationLevel;

    private Integer skillMatchScore;

    private Integer aiMatchScore;

    private Integer jobTypeScore;

    private String matchedSkills;

    private String missingSkills;

    private String reasons;

    private Integer isApplied;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}