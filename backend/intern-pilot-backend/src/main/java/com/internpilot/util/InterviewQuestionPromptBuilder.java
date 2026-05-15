package com.internpilot.util;

import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import org.springframework.util.StringUtils;

import java.util.List;

public class InterviewQuestionPromptBuilder {

    private static final int DEFAULT_QUESTION_COUNT = 8;
    private static final int MAX_QUESTION_COUNT = 20;
    private static final int MIN_QUESTION_COUNT = 3;

    private static final List<String> ALL_CATEGORIES = List.of(
            "JAVA_BASIC", "SPRING_BOOT", "SPRING_SECURITY",
            "MYSQL", "REDIS", "PROJECT", "HR", "RESUME", "JOB_SKILL"
    );

    private static final List<String> ALL_DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private InterviewQuestionPromptBuilder() {
    }

    public static String build(
            String resumeText,
            String jobDescription,
            String analysisReport,
            InterviewQuestionGenerateRequest request) {

        int questionCount = resolveQuestionCount(request);
        List<String> categories = resolveCategories(request);
        List<String> difficulties = resolveDifficulties(request);
        boolean includeAnswer = request == null || !Boolean.FALSE.equals(request.getIncludeAnswer());
        boolean includeFollowUps = request == null || !Boolean.FALSE.equals(request.getIncludeFollowUps());

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个资深 Java 后端面试官、技术招聘官和大学生实习求职导师。\n\n");
        prompt.append("请根据下面的【学生简历文本】、【目标岗位 JD】和【AI 匹配分析报告】，为该学生生成一套适合该岗位的实习面试准备题。\n\n");
        prompt.append("要求：\n");
        prompt.append("1. 题目要贴合岗位 JD；\n");
        prompt.append("2. 题目要结合学生简历中的项目经历；\n");
        prompt.append("3. 项目追问题要针对简历中的具体项目；\n");
        prompt.append("4. 如果分析报告中提到缺失技能，需要生成对应补强题；\n");
        prompt.append("5. 题目难度要符合大学生暑期实习面试；\n");
        prompt.append("6. 答案要简洁但有要点；\n");
        prompt.append("7. 不要生成过于宽泛的问题；\n");
        prompt.append("8. 必须严格返回 JSON；\n");
        prompt.append("9. 不要返回 Markdown；\n");
        prompt.append("10. 不要使用 ```json 代码块包裹。\n\n");

        prompt.append("请生成 ").append(questionCount).append(" 道题，题目类型包含：\n");
        for (String category : categories) {
            prompt.append("- ").append(category).append("\n");
        }
        prompt.append("\n");

        prompt.append("难度分布：\n");
        for (String difficulty : difficulties) {
            prompt.append("- ").append(difficulty).append("\n");
        }
        prompt.append("\n");

        if (includeAnswer) {
            prompt.append("每道题需要包含参考答案。\n");
        }
        if (includeFollowUps) {
            prompt.append("每道题需要包含 2-3 个追问问题。\n");
        }
        prompt.append("每道题需要包含关键词列表和生成依据。\n\n");

        prompt.append("返回格式如下：\n\n");
        prompt.append("""
                {
                  "title": "腾讯 Java后端开发实习生 面试题准备",
                  "questions": [
                    {
                      "questionType": "SPRING_SECURITY",
                      "difficulty": "MEDIUM",
                      "question": "请介绍 Spring Security 的过滤器链执行流程。",
                      "answer": "Spring Security 通过一组过滤器对请求进行认证和授权处理...",
                      "answerPoints": [
                        "请求先经过 SecurityFilterChain",
                        "JWT 项目中会经过自定义 JwtAuthenticationFilter",
                        "认证成功后将 Authentication 放入 SecurityContext"
                      ],
                      "relatedSkills": [
                        "Spring Security",
                        "JWT",
                        "Filter"
                      ],
                      "followUps": [
                        "JWT 和 Session 认证有什么区别？",
                        "如何实现 Token 刷新机制？"
                      ],
                      "keywords": ["Spring Security", "JWT", "过滤器链"],
                      "source": "根据岗位 Spring Security 技能要求生成",
                      "sortOrder": 1
                    }
                  ]
                }
                """);

        prompt.append("\n【学生简历文本】\n").append(resumeText).append("\n\n");
        prompt.append("【目标岗位 JD】\n").append(jobDescription).append("\n\n");
        prompt.append("【AI 匹配分析报告】\n").append(analysisReport);

        return prompt.toString();
    }

    private static int resolveQuestionCount(InterviewQuestionGenerateRequest request) {
        if (request == null || request.getQuestionCount() == null) {
            return DEFAULT_QUESTION_COUNT;
        }
        int count = request.getQuestionCount();
        if (count < MIN_QUESTION_COUNT) {
            return MIN_QUESTION_COUNT;
        }
        if (count > MAX_QUESTION_COUNT) {
            return MAX_QUESTION_COUNT;
        }
        return count;
    }

    private static List<String> resolveCategories(InterviewQuestionGenerateRequest request) {
        if (request == null || request.getCategories() == null || request.getCategories().isEmpty()) {
            return ALL_CATEGORIES;
        }
        List<String> filtered = request.getCategories().stream()
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .distinct()
                .toList();
        return filtered.isEmpty() ? ALL_CATEGORIES : filtered;
    }

    private static List<String> resolveDifficulties(InterviewQuestionGenerateRequest request) {
        if (request == null || request.getDifficulties() == null || request.getDifficulties().isEmpty()) {
            return ALL_DIFFICULTIES;
        }
        List<String> filtered = request.getDifficulties().stream()
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .distinct()
                .toList();
        return filtered.isEmpty() ? ALL_DIFFICULTIES : filtered;
    }
}