package com.internpilot.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改投递记录优先级请求")
public class ApplicationPriorityUpdateRequest {
    @Schema(description = "优先级，仅允许 HIGH/MEDIUM/LOW", example = "HIGH")
    @NotBlank(message = "投递优先级不能为空")
    private String priority;
}
