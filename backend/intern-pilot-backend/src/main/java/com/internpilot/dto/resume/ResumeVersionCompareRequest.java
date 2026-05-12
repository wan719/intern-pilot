package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "简历版本对比请求")
public class ResumeVersionCompareRequest {

    @Schema(description = "旧版本ID")
    @NotNull(message = "旧版本ID不能为空")
    private Long oldVersionId;

    @Schema(description = "新版本ID")
    @NotNull(message = "新版本ID不能为空")
    private Long newVersionId;
}