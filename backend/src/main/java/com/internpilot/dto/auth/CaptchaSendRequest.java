package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发送验证码请求")
public class CaptchaSendRequest {

    @Schema(description = "手机号或邮箱", example = "demo@example.com")
    @NotBlank(message = "手机号或邮箱不能为空")
    @Size(max = 100, message = "验证码接收账号长度不能超过 100 个字符")
    private String target;

    @Schema(description = "账号类型：PHONE / EMAIL", example = "EMAIL")
    @NotBlank(message = "账号类型不能为空")
    @Size(max = 20, message = "账号类型长度不能超过 20 个字符")
    private String type;
}
