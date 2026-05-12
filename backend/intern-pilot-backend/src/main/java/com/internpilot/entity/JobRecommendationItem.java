package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_recommendation_item")
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