package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "AI优化简历版本请求")
public class ResumeVersionOptimizeRequest {

    @Schema(description = "来源版本ID", example = "1")
    @NotNull(message = "来源版本ID不能为空")
    private Long sourceVersionId;

    @Schema(description = "目标岗位ID", example = "1")
    @NotNull(message = "目标岗位ID不能为空")
    private Long targetJobId;

    @Schema(description = "AI分析报告ID", example = "1")
    private Long aiReportId;

    @Schema(description = "新版本名称", example = "腾讯Java后端定制版")
    private String versionName;

    @Schema(description = "额外优化要求", example = "突出Spring Boot和Redis项目经验")
    private String extraRequirement;
}