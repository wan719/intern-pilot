package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum CaptchaTargetTypeEnum {

    PHONE("PHONE"),
    EMAIL("EMAIL");

    private final String code;

    CaptchaTargetTypeEnum(String code) {
        this.code = code;
    }

    public static boolean isSupported(String code) {
        return PHONE.code.equalsIgnoreCase(code) || EMAIL.code.equalsIgnoreCase(code);
    }
}
