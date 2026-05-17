package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "手机号 / 邮箱 / 用户名（系统账号）", example = "demo@internpilot.local")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}