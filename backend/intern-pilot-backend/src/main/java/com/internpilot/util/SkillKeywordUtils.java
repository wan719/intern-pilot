package com.internpilot.util;

import java.util.ArrayList;
import java.util.List;

public class SkillKeywordUtils {

    private static final List<String> SKILL_KEYWORDS = List.of(
            "Java",
            "Spring Boot",
            "Spring Security",
            "Spring MVC",
            "MyBatis",
            "MyBatis Plus",
            "MySQL",
            "Redis",
            "JWT",
            "Docker",
            "Linux",
            "Git",
            "Vue",
            "TypeScript",
            "WebSocket",
            "RabbitMQ",
            "Kafka",
            "Nginx",
            "JUnit",
            "Mockito",
            "RESTful",
            "Swagger",
            "Knife4j",
            "Python",
            "PyTorch",
            "OpenCV",
            "YOLO",
            "LLM",
            "Prompt",
            "RAG",
            "向量数据库",
            "Embedding"
    );

    public static List<String> extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String lowerText = text.toLowerCase();
        List<String> result = new ArrayList<>();

        for (String keyword : SKILL_KEYWORDS) {
            if (lowerText.contains(keyword.toLowerCase())) {
                result.add(keyword);
            }
        }

        return result;
    }

    public static List<String> matchedSkills(List<String> resumeSkills, List<String> jobSkills) {
        return jobSkills.stream()
                .filter(resumeSkills::contains)
                .toList();
    }

    public static List<String> missingSkills(List<String> resumeSkills, List<String> jobSkills) {
        return jobSkills.stream()
                .filter(skill -> !resumeSkills.contains(skill))
                .toList();
    }

    public static int calculateSkillScore(List<String> matchedSkills, List<String> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) {
            return 60;
        }

        return (int) Math.round(matchedSkills.size() * 100.0 / jobSkills.size());
    }
}