package com.internpilot.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "面试题报告列表响应")
public class InterviewQuestionListResponse {

    @Schema(description = "面试题报告ID")
    private Long reportId;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}