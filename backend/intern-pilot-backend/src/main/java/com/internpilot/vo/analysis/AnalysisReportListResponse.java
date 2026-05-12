package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "分析报告列表响应")
public class AnalysisReportListResponse {

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历版本ID")
    private Long resumeVersionId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "匹配分数")
    private Integer matchScore;

    @Schema(description = "匹配等级")
    private String matchLevel;

    @Schema(description = "是否命中缓存")
    private Boolean cacheHit;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
