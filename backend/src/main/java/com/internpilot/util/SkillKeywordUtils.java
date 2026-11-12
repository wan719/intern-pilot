package com.internpilot.util;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "技能关键词工具类，提供了从文本中提取技能关键词、匹配简历技能与岗位技能、计算技能匹配分数等功能，用于在简历分析和岗位匹配过程中评估学生的技能与岗位要求的匹配程度")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
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