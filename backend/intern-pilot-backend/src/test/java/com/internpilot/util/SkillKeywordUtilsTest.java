package com.internpilot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillKeywordUtilsTest {

    @Test
    void extractSkills_shouldFindJavaBackendSkills() {
        String text = "我熟悉 Java、Spring Boot、MySQL、Redis 和 Spring Security。";

        List<String> skills = SkillKeywordUtils.extractSkills(text);

        assertThat(skills).contains("Java", "Spring Boot", "MySQL", "Redis", "Spring Security");
    }

    @Test
    void calculateSkillScore_shouldCalculateCorrectly() {
        int score = SkillKeywordUtils.calculateSkillScore(
                List.of("Java", "Spring Boot"),
                List.of("Java", "Spring Boot", "MySQL", "Redis")
        );

        assertThat(score).isEqualTo(50);
    }
}