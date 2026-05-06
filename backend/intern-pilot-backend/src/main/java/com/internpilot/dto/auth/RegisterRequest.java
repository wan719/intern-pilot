package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "用户名", example = "wan")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 位之间")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度必须在 6 到 30 位之间")
    private String password;

    @Schema(description = "确认密码", example = "123456")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "邮箱", example = "wan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "学校", example = "西南大学")
    private String school;

    @Schema(description = "专业", example = "软件工程")
    private String major;

    @Schema(description = "年级", example = "大二")
    private String grade;
}
