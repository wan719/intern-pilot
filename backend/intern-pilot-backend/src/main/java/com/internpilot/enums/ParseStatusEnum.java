package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ParseStatusEnum {

    SUCCESS("SUCCESS", "解析成功"),
    FAILED("FAILED", "解析失败"),
    PENDING("PENDING", "等待解析");

    private final String code;
    private final String description;

    ParseStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
