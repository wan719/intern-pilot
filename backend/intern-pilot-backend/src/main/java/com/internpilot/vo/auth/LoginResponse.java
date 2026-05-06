package com.internpilot.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "过期时间，单位秒", example = "86400")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private AuthUserResponse user;
}
