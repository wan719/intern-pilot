package com.internpilot.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "回复反馈请求")
public class FeedbackReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 3000, message = "回复内容长度不能超过 3000 个字符")
    @Schema(description = "回复内容", example = "感谢您的反馈，我们已收到并正在处理")
    private String reply;
}
