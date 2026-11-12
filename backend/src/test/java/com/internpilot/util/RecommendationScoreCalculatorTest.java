package com.internpilot.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationScoreCalculatorTest {

    @Test
    void calculateFinalScore_shouldUseHybrid_whenAiScoreExists() {
        int score = RecommendationScoreCalculator.calculateFinalScore(80, 90, 70);

        assertThat(score).isEqualTo(83);
    }

    @Test
    void calculateFinalScore_shouldUseRuleScore_whenAiScoreMissing() {
        int score = RecommendationScoreCalculator.calculateFinalScore(80, 0, 60);

        assertThat(score).isEqualTo(76);
    }
}