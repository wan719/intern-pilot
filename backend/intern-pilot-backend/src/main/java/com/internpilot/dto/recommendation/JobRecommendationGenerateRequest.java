package com.internpilot.dto.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "生成岗位推荐请求")
public class JobRecommendationGenerateRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @Schema(description = "简历版本ID", example = "1")
    private Long resumeVersionId;

    @Schema(description = "是否包含已投递岗位", example = "false")
    private Boolean includeApplied = false;

    @Schema(description = "最多推荐数量", example = "10")
    @Min(value = 1, message = "推荐数量不能少于 1")
    @Max(value = 50, message = "推荐数量不能超过 50")
    private Integer limit = 10;
}
