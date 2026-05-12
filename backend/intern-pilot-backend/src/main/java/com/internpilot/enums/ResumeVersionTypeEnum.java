package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ResumeVersionTypeEnum {

    ORIGINAL("ORIGINAL", "原始版本"),
    MANUAL("MANUAL", "手动编辑版本"),
    AI_OPTIMIZED("AI_OPTIMIZED", "AI优化版本"),
    JOB_TARGETED("JOB_TARGETED", "岗位定制版本"),
    IMPORTED("IMPORTED", "导入版本");

    private final String code;
    private final String description;

    ResumeVersionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (ResumeVersionTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}