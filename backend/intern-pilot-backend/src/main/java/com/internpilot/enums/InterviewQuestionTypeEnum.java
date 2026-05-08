package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum InterviewQuestionTypeEnum {

    JAVA_BASIC("JAVA_BASIC", "Java基础"),
    SPRING_BOOT("SPRING_BOOT", "Spring Boot"),
    SPRING_SECURITY("SPRING_SECURITY", "Spring Security"),
    MYSQL("MYSQL", "MySQL"),
    REDIS("REDIS", "Redis"),
    PROJECT("PROJECT", "项目追问"),
    ALGORITHM("ALGORITHM", "算法与数据结构"),
    SYSTEM_DESIGN("SYSTEM_DESIGN", "系统设计"),
    HR("HR", "HR面试"),
    RESUME("RESUME", "简历深挖"),
    JOB_SKILL("JOB_SKILL", "岗位技能专项");

    private final String code;
    private final String description;

    InterviewQuestionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (InterviewQuestionTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}