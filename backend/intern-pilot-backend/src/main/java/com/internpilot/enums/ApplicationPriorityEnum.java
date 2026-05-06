package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ApplicationPriorityEnum {

    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String description;

    ApplicationPriorityEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (ApplicationPriorityEnum priority : values()) {
            if (priority.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
