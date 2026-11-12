package com.internpilot.util;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "推荐分数计算器，根据技能匹配分数、AI匹配分数和岗位类型分数等信息计算最终的推荐分数，用于评估学生与岗位的匹配程度")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class RecommendationScoreCalculator {

    public static int calculateFinalScore(
            Integer skillMatchScore,
            Integer aiMatchScore,
            Integer jobTypeScore
    ) {
        int skill = skillMatchScore == null ? 0 : skillMatchScore;
        int ai = aiMatchScore == null ? 0 : aiMatchScore;
        int jobType = jobTypeScore == null ? 60 : jobTypeScore;

        if (ai > 0) {
            return clamp((int) Math.round(skill * 0.5 + ai * 0.4 + jobType * 0.1));
        }

        return clamp((int) Math.round(skill * 0.8 + jobType * 0.2));
    }

    private static int clamp(int score) {
        if (score < 0) {
            return 0;
        }

        if (score > 100) {
            return 100;
        }

        return score;
    }
}