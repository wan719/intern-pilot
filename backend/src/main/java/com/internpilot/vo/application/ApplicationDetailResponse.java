package com.internpilot.vo.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "投递记录详情响应")
public class ApplicationDetailResponse {

    @Schema(description = "投递记录ID")
    private Long applicationId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "匹配分数")
    private Integer matchScore;

    @Schema(description = "匹配等级")
    private String matchLevel;

    @Schema(description = "投递状态")
    private String status;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "投递日期")
    private LocalDate applyDate;

    @Schema(description = "面试时间")
    private LocalDateTime interviewDate;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "面试或笔试复盘")
    private String review;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
