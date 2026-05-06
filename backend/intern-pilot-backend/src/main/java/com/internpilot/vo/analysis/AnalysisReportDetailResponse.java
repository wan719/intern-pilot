package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "分析报告详情响应")
public class AnalysisReportDetailResponse {

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历名称")
    private String resumeName;

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

    @Schema(description = "AI 服务商")
    private String aiProvider;

    @Schema(description = "AI 模型")
    private String aiModel;

    @Schema(description = "是否命中缓存")
    private Boolean cacheHit;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
