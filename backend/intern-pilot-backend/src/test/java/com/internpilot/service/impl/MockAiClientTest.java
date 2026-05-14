package com.internpilot.service.impl;

import com.internpilot.enums.AiScenarioEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAiClientTest {

    private final MockAiClient mockAiClient = new MockAiClient();

    @Test
    void chatShouldReturnAnalysisJsonForResumeJobAnalysisPrompt() {
        String prompt = "请根据学生简历和目标岗位JD进行匹配分析，输出matchScore等字段";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("matchScore"));
        assertTrue(result.contains("strengths"));
        assertTrue(result.contains("weaknesses"));
    }

    @Test
    void chatShouldReturnOptimizedResumeForResumeOptimizationPrompt() {
        String prompt = "请根据原始简历内容优化简历，生成一份更适合该岗位投递的简历优化版本";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("优化后的简历内容"));
        assertTrue(result.contains("InternPilot"));
    }

    @Test
    void chatShouldReturnQuestionsForInterviewQuestionPrompt() {
        String prompt = "请根据学生简历和目标岗位JD生成面试题";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("questions"));
        assertTrue(result.contains("HashMap"));
    }

    @Test
    void chatShouldReturnRecommendationsForJobRecommendationPrompt() {
        String prompt = "请根据学生简历进行岗位推荐";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("recommendations"));
        assertTrue(result.contains("腾讯"));
    }

    @Test
    void chatShouldReturnDefaultForUnknownPrompt() {
        String prompt = "你好，请帮我分析一下";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("Mock AI 默认响应"));
    }

    @Test
    void chatShouldReturnDefaultForBlankPrompt() {
        String result = mockAiClient.chat("");

        assertNotNull(result);
        assertTrue(result.contains("Mock AI 默认响应"));
    }

    @Test
    void chatShouldReturnDefaultForNullPrompt() {
        String result = mockAiClient.chat(null);

        assertNotNull(result);
        assertTrue(result.contains("Mock AI 默认响应"));
    }

    @Test
    void chatShouldNotThrowForAnyPrompt() {
        assertDoesNotThrow(() -> mockAiClient.chat("任意文本"));
        assertDoesNotThrow(() -> mockAiClient.chat(""));
        assertDoesNotThrow(() -> mockAiClient.chat(null));
        assertDoesNotThrow(() -> mockAiClient.chat("包含匹配分析和面试题的混合文本"));
    }
}