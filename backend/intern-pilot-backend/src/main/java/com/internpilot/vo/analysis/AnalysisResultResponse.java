package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "AI 分析结果响应")
public class AnalysisResultResponse {

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "匹配分数")
    private Integer matchScore;

    @Schema(description = "匹配等级")
    private String matchLevel;

    @Schema(description = "简历优势")
    private List<String> strengths;

    @Schema(description = "简历短板")
    private List<String> weaknesses;

    @Schema(description = "缺失技能")
    private List<String> missingSkills;

    @Schema(description = "简历优化建议")
    private List<String> suggestions;

    @Schema(description = "面试准备建议")
    private List<String> interviewTips;

    @Schema(description = "是否命中缓存")
    private Boolean cacheHit;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
