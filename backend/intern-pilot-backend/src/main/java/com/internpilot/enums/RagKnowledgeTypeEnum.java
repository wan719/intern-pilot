package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum RagKnowledgeTypeEnum {

    JOB_DIRECTION("JOB_DIRECTION", "岗位方向介绍"),
    SKILL_REQUIREMENT("SKILL_REQUIREMENT", "技能要求"),
    INTERVIEW_POINT("INTERVIEW_POINT", "面试重点"),
    RESUME_ADVICE("RESUME_ADVICE", "简历优化建议"),
    LEARNING_PATH("LEARNING_PATH", "学习路线"),
    PROJECT_SUGGESTION("PROJECT_SUGGESTION", "项目建议"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    RagKnowledgeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (RagKnowledgeTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}