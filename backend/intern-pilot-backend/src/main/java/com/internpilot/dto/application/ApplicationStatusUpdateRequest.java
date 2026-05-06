package com.internpilot.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改投递状态请求")
public class ApplicationStatusUpdateRequest {

    @Schema(description = "投递状态", example = "APPLIED")
    @NotBlank(message = "投递状态不能为空")
    private String status;
}
