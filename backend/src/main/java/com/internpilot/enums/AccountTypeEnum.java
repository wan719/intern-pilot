package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "账号类型枚举")
public enum AccountTypeEnum {

    PHONE("PHONE", "手机号"),
    EMAIL("EMAIL", "邮箱"),
    USERNAME("USERNAME", "用户名"),
    SYSTEM("SYSTEM", "系统账号");

    private final String code;
    private final String description;

    AccountTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        for (AccountTypeEnum type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}