package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum RecommendationStrategyEnum {
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
