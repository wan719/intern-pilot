package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "AI分析任务详情响应")
public class AnalysisTaskDetailResponse {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历版本ID")
    private Long resumeVersionId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "报告ID")
    private Long reportId;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "当前进度")
    private Integer progress;

    @Schema(description = "当前消息")
    private String message;

    @Schema(description = "失败原因")
    private String errorMessage;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
