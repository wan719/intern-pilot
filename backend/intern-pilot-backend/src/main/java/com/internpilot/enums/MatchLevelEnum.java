package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum MatchLevelEnum {

    HIGH("HIGH", "高匹配"),
    MEDIUM_HIGH("MEDIUM_HIGH", "较高匹配"),
    MEDIUM("MEDIUM", "中等匹配"),
    LOW("LOW", "低匹配"),
    VERY_LOW("VERY_LOW", "很低匹配");

    private final String code;
    private final String description;

    MatchLevelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String fromScore(Integer score) {
        if (score == null) {
            return MEDIUM.getCode();
        }
        if (score >= 85) {
            return HIGH.getCode();
        } else if (score >= 70) {
            return MEDIUM_HIGH.getCode();
        } else if (score >= 60) {
            return MEDIUM.getCode();
        } else if (score >= 40) {
            return LOW.getCode();
        } else {
            return VERY_LOW.getCode();
        }
    }
}
