package com.internpilot.util;

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