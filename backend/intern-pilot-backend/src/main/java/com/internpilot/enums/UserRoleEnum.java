package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("USER", "普通用户"),
    ADMIN("ADMIN", "系统管理员");

    private final String code;
    private final String description;

    UserRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
