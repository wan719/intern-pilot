package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "简历版本类型枚举，定义系统中简历的不同版本类型，每个版本类型对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum ResumeVersionTypeEnum {//简历版本类型枚举，定义系统中简历的不同版本类型，
// 每个版本类型对应一个唯一的code和描述

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