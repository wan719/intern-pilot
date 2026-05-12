package com.internpilot.vo.recommendation;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "岗位推荐批次详情响应")
public class JobRecommendationBatchDetailResponse {
    @Schema(description = "推荐批次ID")
    private Long batchId;
    
    @Schema(description = "用户ID")
    private Long resumeId;

    @Schema(description = "简历版本ID")
    private Long resumeVersionId;

    @Schema(description = "批次标题")
    private String title;

    @Schema(description = "岗位数量")
    private Integer jobCount;

    @Schema(description = "推荐数量")
    private Integer recommendedCount;

    @Schema(description = "推荐策略")
    private String strategy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "推荐项列表")
    private List<JobRecommendationItemResponse> items;
}