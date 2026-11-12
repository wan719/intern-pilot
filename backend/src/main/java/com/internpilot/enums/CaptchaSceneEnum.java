package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "验证码场景枚举")
public enum CaptchaSceneEnum {

    PHONE_REGISTER("PHONE_REGISTER", "手机号注册"),
    EMAIL_REGISTER("EMAIL_REGISTER", "邮箱注册");

    private final String code;
    private final String description;

    CaptchaSceneEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}