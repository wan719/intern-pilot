package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "匹配度枚举，定义简历与岗位匹配的不同级别，每个级别对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum MatchLevelEnum {//匹配度枚举，定义简历与岗位匹配的不同级别，
// 每个级别对应一个唯一的code和描述

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

    /**
     * 根据匹配分数获取对应的匹配度枚举实例
     *
     * @param score 匹配分数
     * @return 对应的匹配度枚举实例
     */
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
