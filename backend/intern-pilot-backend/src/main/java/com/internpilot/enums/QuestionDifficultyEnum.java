package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum QuestionDifficultyEnum {

    EASY("EASY", "简单"),
    MEDIUM("MEDIUM", "中等"),
    HARD("HARD", "较难");

    private final String code;
    private final String description;

    QuestionDifficultyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (QuestionDifficultyEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}