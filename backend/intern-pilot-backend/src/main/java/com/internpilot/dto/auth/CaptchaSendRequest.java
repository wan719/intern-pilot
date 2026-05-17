package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发送验证码请求")
public class CaptchaSendRequest {

    @Schema(description = "手机号或邮箱", example = "demo@example.com")
    @NotBlank(message = "手机号或邮箱不能为空")
    private String target;

    @Schema(description = "账号类型：PHONE / EMAIL", example = "EMAIL")
    @NotBlank(message = "账号类型不能为空")
    private String type;
}