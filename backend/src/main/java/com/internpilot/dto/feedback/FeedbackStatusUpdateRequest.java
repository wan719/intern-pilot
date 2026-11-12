package com.internpilot.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "更新反馈状态请求")
public class FeedbackStatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    @Schema(description = "反馈状态", example = "PROCESSING")
    private String status;
}