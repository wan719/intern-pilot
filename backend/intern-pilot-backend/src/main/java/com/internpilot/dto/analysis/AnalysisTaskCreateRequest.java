package com.internpilot.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建AI分析任务请求")
public class AnalysisTaskCreateRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位ID不能为空")
    private Long jobId;

    @Schema(description = "简历版本ID，不传则使用当前版本")
    private Long resumeVersionId;

    @Schema(description = "是否强制重新分析", example = "false")
    private Boolean forceRefresh = false;
}
