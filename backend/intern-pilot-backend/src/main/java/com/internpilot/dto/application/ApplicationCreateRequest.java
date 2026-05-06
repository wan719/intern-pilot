package com.internpilot.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建投递记录请求")
public class ApplicationCreateRequest {

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位 ID 不能为空")
    private Long jobId;

    @Schema(description = "简历ID", example = "1")
    private Long resumeId;

    @Schema(description = "分析报告ID", example = "1")
    private Long reportId;

    @Schema(description = "投递状态", example = "TO_APPLY")
    private String status;

    @Schema(description = "投递日期", example = "2026-05-06")
    private LocalDate applyDate;

    @Schema(description = "面试时间", example = "2026-05-10T14:00:00")
    private LocalDateTime interviewDate;

    @Schema(description = "备注", example = "准备投递该岗位")
    private String note;

    @Schema(description = "优先级", example = "HIGH")
    private String priority;
}
