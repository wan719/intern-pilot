package com.internpilot.enums;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "推荐级别枚举，定义系统中岗位推荐的不同级别，每个级别对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum RecommendationLevelEnum {//推荐级别枚举，定义系统中岗位推荐的不同级别，
// 每个级别对应一个唯一的code和描述

    HIGH("HIGH", "强烈推荐"),
    MEDIUM_HIGH("MEDIUM_HIGH", "较推荐"),
    MEDIUM("MEDIUM", "一般推荐"),
    LOW("LOW", "低推荐"),
    NOT_RECOMMENDED("NOT_RECOMMENDED", "不推荐");

    private final String code;
    private final String description;

    private RecommendationLevelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String ofScore(Integer score) {
        if (score == null) {
            return NOT_RECOMMENDED.code;
        }

        if (score >= 85) {
            return HIGH.code;
        }

        if (score >= 70) {
            return MEDIUM_HIGH.code;
        }

        if (score >= 60) {
            return MEDIUM.code;
        }

        if (score >= 40) {
            return LOW.code;
        }

        return NOT_RECOMMENDED.code;
    }
}
