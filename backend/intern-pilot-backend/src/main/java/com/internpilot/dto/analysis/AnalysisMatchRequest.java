package com.internpilot.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "简历岗位匹配分析请求")
public class AnalysisMatchRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位 ID 不能为空")
    private Long jobId;

    @Schema(description = "是否强制重新分析", example = "false")
    private Boolean forceRefresh = false;
}
