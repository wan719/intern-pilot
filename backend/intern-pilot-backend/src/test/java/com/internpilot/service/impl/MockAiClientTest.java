package com.internpilot.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAiClientTest {

    private final MockAiClient mockAiClient = new MockAiClient();

    @Test
    void chatShouldReturnAnalysisJsonForResumeJobAnalysisPrompt() {
        String prompt = "Please generate a resume-job match analysis with matchScore.";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("matchScore"));
        assertTrue(result.contains("strengths"));
        assertTrue(result.contains("weaknesses"));
    }

    @Test
    void chatShouldReturnOptimizedResumeForResumeOptimizationPrompt() {
        String prompt = "Please optimize this resume and return an optimized resume version.";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("优化后的简历内容"));
        assertTrue(result.contains("InternPilot"));
    }

    @Test
    void chatShouldReturnQuestionsForInterviewQuestionPrompt() {
        String prompt = "Generate interview questions with questionType, followUps and keywords.";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("questions"));
        assertTrue(result.contains("questionType"));
        assertTrue(result.contains("SPRING_BOOT"));
        assertTrue(result.contains("relatedSkills"));
        assertTrue(result.contains("followUps"));
        assertTrue(result.contains("keywords"));
    }

    @Test
    void chatShouldReturnRecommendationsForJobRecommendationPrompt() {
        String prompt = "Generate job recommendations for this student resume.";
        String result = mockAiClient.chat(prompt);

        assertNotNull(result);
        assertTrue(result.contains("recommendations"));
        assertTrue(result.contains("星云科技"));
    }

    @Test
    void chatShouldReturnDefaultForUnknownPrompt() {
        String prompt = "Hello, please help me analyze this.";
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
        assertDoesNotThrow(() -> mockAiClient.chat("any text"));
        assertDoesNotThrow(() -> mockAiClient.chat(""));
        assertDoesNotThrow(() -> mockAiClient.chat(null));
        assertDoesNotThrow(() -> mockAiClient.chat("mixed match analysis and interview questions"));
    }
}
