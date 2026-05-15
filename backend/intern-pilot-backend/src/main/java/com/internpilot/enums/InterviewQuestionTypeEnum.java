package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "面试题类型枚举，定义系统中不同类型的面试题，每个类型对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum InterviewQuestionTypeEnum {//面试题类型枚举，定义系统中不同类型的面试题，
// 每个类型对应一个唯一的code和描述

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