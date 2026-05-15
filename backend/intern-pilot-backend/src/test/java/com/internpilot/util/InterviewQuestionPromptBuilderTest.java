package com.internpilot.util;

import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewQuestionPromptBuilderTest {

    @Test
    void buildShouldReturnPromptWithDefaultsWhenRequestIsNull() {
        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                null);

        assertNotNull(prompt);
        assertTrue(prompt.contains("简历内容"));
        assertTrue(prompt.contains("岗位JD"));
        assertTrue(prompt.contains("分析报告"));
        assertTrue(prompt.contains("8 道题"));
        assertTrue(prompt.contains("JAVA_BASIC"));
        assertTrue(prompt.contains("EASY"));
        assertTrue(prompt.contains("MEDIUM"));
        assertTrue(prompt.contains("HARD"));
    }

    @Test
    void buildShouldUseCustomQuestionCount() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setQuestionCount(5);

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(prompt.contains("5 道题"));
    }

    @Test
    void buildShouldClampQuestionCountToMin() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setQuestionCount(1);

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(prompt.contains("3 道题"));
    }

    @Test
    void buildShouldClampQuestionCountToMax() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setQuestionCount(100);

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(prompt.contains("20 道题"));
    }

    @Test
    void buildShouldUseCustomCategories() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setCategories(List.of("JAVA_BASIC", "MYSQL"));

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(prompt.contains("JAVA_BASIC"));
        assertTrue(prompt.contains("MYSQL"));
    }

    @Test
    void buildShouldUseCustomDifficulties() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setDifficulties(List.of("EASY"));

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(prompt.contains("EASY"));
    }

    @Test
    void buildShouldIncludeAnswerWhenIncludeAnswerIsTrue() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setIncludeAnswer(true);

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(prompt.contains("参考答案"));
    }

    @Test
    void buildShouldExcludeAnswerWhenIncludeAnswerIsFalse() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setIncludeAnswer(false);

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(!prompt.contains("每道题需要包含参考答案"));
    }

    @Test
    void buildShouldIncludeFollowUpsWhenIncludeFollowUpsIsTrue() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setIncludeFollowUps(true);

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(prompt.contains("追问问题"));
    }

    @Test
    void buildShouldExcludeFollowUpsWhenIncludeFollowUpsIsFalse() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setIncludeFollowUps(false);

        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                request);

        assertTrue(!prompt.contains("每道题需要包含 2-3 个追问问题"));
    }

    @Test
    void buildShouldIncludeKeywordsAndSource() {
        String prompt = InterviewQuestionPromptBuilder.build(
                "简历内容",
                "岗位JD",
                "分析报告",
                null);

        assertTrue(prompt.contains("关键词"));
        assertTrue(prompt.contains("生成依据"));
    }
}