package com.internpilot.vo.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建投递记录响应")
public class ApplicationCreateResponse {

    @Schema(description = "投递记录ID")
    private Long applicationId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "投递状态")
    private String status;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
