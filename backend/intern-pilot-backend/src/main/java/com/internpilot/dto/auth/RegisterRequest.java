package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "手机号或邮箱", example = "demo@example.com")
    @NotBlank(message = "手机号或邮箱不能为空")
    @Size(max = 100, message = "账号长度不能超过 100 个字符")
    private String account;

    @Schema(description = "账号类型：PHONE / EMAIL", example = "EMAIL")
    @NotBlank(message = "账号类型不能为空")
    @Size(max = 20, message = "账号类型长度不能超过 20 个字符")
    private String accountType;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须在 6 到 72 位之间")
    private String password;

    @Schema(description = "确认密码", example = "123456")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "验证码", example = "123456")
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 10, message = "验证码长度不合法")
    private String captchaCode;

    @Schema(description = "用户名", example = "wan")
    @Size(max = 50, message = "用户名长度不能超过 50 个字符")
    private String username;

    @Schema(description = "学校", example = "西南大学")
    @Size(max = 100, message = "学校长度不能超过 100 个字符")
    private String school;

    @Schema(description = "专业", example = "软件工程")
    @Size(max = 100, message = "专业长度不能超过 100 个字符")
    private String major;

    @Schema(description = "年级", example = "大二")
    @Size(max = 30, message = "年级长度不能超过 30 个字符")
    private String grade;
}
