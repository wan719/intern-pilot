package com.internpilot.vo.recommendation;

import lombok.Data;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "生成岗位推荐响应")
public class JobRecommendationGenerateResponse {
    @Schema(description = "推荐批次ID")
    private Long batchId;
    
    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历版本ID")
    private Long resumeVersionId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "岗位数量")
    private Integer jobCount;

    @Schema(description = "推荐数量")
    private Integer recommendedCount;

    @Schema(description = "推荐策略")
    private String strategy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}