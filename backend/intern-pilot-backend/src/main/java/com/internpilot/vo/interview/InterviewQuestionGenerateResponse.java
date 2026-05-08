package com.internpilot.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "生成AI面试题响应")
public class InterviewQuestionGenerateResponse {

    @Schema(description = "面试题报告ID")
    private Long reportId;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "AI分析报告ID")
    private Long analysisReportId;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "是否复用历史报告")
    private Boolean cacheHit;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
