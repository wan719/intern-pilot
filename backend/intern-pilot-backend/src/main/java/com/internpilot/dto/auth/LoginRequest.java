package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "手机号 / 邮箱 / 用户名", example = "demo@internpilot.local")
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过 100 个字符")
    private String account;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须在 6 到 72 位之间")
    private String password;
}
