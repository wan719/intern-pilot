package com.internpilot.vo.recommendation;

import lombok.Data;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "岗位推荐项响应")
public class JobRecommendationItemResponse {

    @Schema(description = "推荐项ID")
    private Long itemId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "分析报告ID")
    private Long analysisReportId;
    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位标题")
    private String jobTitle;

    @Schema(description = "岗位类型")
    private String jobType;

    @Schema(description = "工作地点")
    private String location;

    @Schema(description = "薪资范围")
    private String salaryRange;

    @Schema(description = "来源平台")
    private String sourcePlatform;

    @Schema(description = "推荐分数")
    private Integer recommendationScore;

    @Schema(description = "推荐等级")
    private String recommendationLevel;

    @Schema(description = "技能匹配分数")
    private Integer skillMatchScore;

    @Schema(description = "AI匹配分数")
    private Integer aiMatchScore;

    @Schema(description = "岗位类型分数")
    private Integer jobTypeScore;

    @Schema(description = "匹配的技能列表")
    private List<String> matchedSkills;

    @Schema(description = "缺失的技能列表")
    private List<String> missingSkills;

    @Schema(description = "推荐理由列表")
    private List<String> reasons;

    @Schema(description = "是否已申请")
    private Integer isApplied;
    
    @Schema(description = "排序序号")
    private Integer sortOrder;
}