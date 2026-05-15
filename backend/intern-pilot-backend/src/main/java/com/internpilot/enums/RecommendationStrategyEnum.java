package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "推荐策略枚举，定义系统中岗位推荐的不同策略，每个策略对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum RecommendationStrategyEnum {//推荐策略枚举，定义系统中岗位推荐的不同策略，
// 每个策略对应一个唯一的code和描述

    RULE_BASED("RULE_BASED", "规则推荐"),
    AI_SCORE_BASED("AI_SCORE_BASED", "AI分数推荐"),
    HYBRID("HYBRID", "混合推荐"),
    EMBEDDING("EMBEDDING", "向量相似度推荐");

    private final String code;
    private final String description;

    private RecommendationStrategyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
}
