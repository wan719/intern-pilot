package com.internpilot.util;

import com.internpilot.dto.interview.AiInterviewQuestionResult;
import com.internpilot.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewQuestionParserTest {

    @Test
    void parseShouldReturnValidResult() {
        String rawResponse = """
                {
                  "title": "测试面试题",
                  "questions": [
                    {
                      "questionType": "JAVA_BASIC",
                      "difficulty": "EASY",
                      "question": "什么是多态？",
                      "answer": "多态是面向对象的三大特性之一...",
                      "answerPoints": ["编译时多态", "运行时多态"],
                      "relatedSkills": ["Java", "OOP"],
                      "followUps": ["追问1", "追问2"],
                      "keywords": ["多态", "OOP"],
                      "source": "根据岗位要求生成"
                    }
                  ]
                }
                """;

        AiInterviewQuestionResult result = InterviewQuestionParser.parse(rawResponse);

        assertNotNull(result);
        assertEquals("测试面试题", result.getTitle());
        assertEquals(1, result.getQuestions().size());
        assertEquals("JAVA_BASIC", result.getQuestions().get(0).getQuestionType());
        assertEquals("EASY", result.getQuestions().get(0).getDifficulty());
        assertEquals(2, result.getQuestions().get(0).getFollowUps().size());
        assertEquals(2, result.getQuestions().get(0).getKeywords().size());
        assertEquals("根据岗位要求生成", result.getQuestions().get(0).getSource());
        assertEquals(1, result.getQuestions().get(0).getSortOrder());
    }

    @Test
    void parseShouldNormalizeInvalidQuestionType() {
        String rawResponse = """
                {
                  "title": "测试",
                  "questions": [
                    {
                      "questionType": "INVALID_TYPE",
                      "difficulty": "EASY",
                      "question": "测试问题",
                      "answer": "测试答案"
                    }
                  ]
                }
                """;

        AiInterviewQuestionResult result = InterviewQuestionParser.parse(rawResponse);

        assertEquals("JOB_SKILL", result.getQuestions().get(0).getQuestionType());
    }

    @Test
    void parseShouldNormalizeInvalidDifficulty() {
        String rawResponse = """
                {
                  "title": "测试",
                  "questions": [
                    {
                      "questionType": "JAVA_BASIC",
                      "difficulty": "INVALID",
                      "question": "测试问题",
                      "answer": "测试答案"
                    }
                  ]
                }
                """;

        AiInterviewQuestionResult result = InterviewQuestionParser.parse(rawResponse);

        assertEquals("MEDIUM", result.getQuestions().get(0).getDifficulty());
    }

    @Test
    void parseShouldFilterEmptyQuestions() {
        String rawResponse = """
                {
                  "title": "测试",
                  "questions": [
                    {
                      "questionType": "JAVA_BASIC",
                      "difficulty": "EASY",
                      "question": "",
                      "answer": "空问题"
                    },
                    {
                      "questionType": "MYSQL",
                      "difficulty": "MEDIUM",
                      "question": "有效问题",
                      "answer": "有效答案"
                    }
                  ]
                }
                """;

        AiInterviewQuestionResult result = InterviewQuestionParser.parse(rawResponse);

        assertEquals(1, result.getQuestions().size());
        assertEquals("有效问题", result.getQuestions().get(0).getQuestion());
    }

    @Test
    void parseShouldHandleNullFields() {
        String rawResponse = """
                {
                  "title": "测试",
                  "questions": [
                    {
                      "questionType": "JAVA_BASIC",
                      "difficulty": "EASY",
                      "question": "测试问题",
                      "answer": "测试答案"
                    }
                  ]
                }
                """;

        AiInterviewQuestionResult result = InterviewQuestionParser.parse(rawResponse);

        assertNotNull(result.getQuestions().get(0).getAnswerPoints());
        assertNotNull(result.getQuestions().get(0).getRelatedSkills());
        assertNotNull(result.getQuestions().get(0).getFollowUps());
        assertNotNull(result.getQuestions().get(0).getKeywords());
        assertTrue(result.getQuestions().get(0).getAnswerPoints().isEmpty());
        assertTrue(result.getQuestions().get(0).getFollowUps().isEmpty());
    }

    @Test
    void parseShouldThrowWhenNoQuestions() {
        String rawResponse = """
                {
                  "title": "测试",
                  "questions": []
                }
                """;

        assertThrows(BusinessException.class, () -> InterviewQuestionParser.parse(rawResponse));
    }

    @Test
    void parseShouldThrowWhenAllQuestionsEmpty() {
        String rawResponse = """
                {
                  "title": "测试",
                  "questions": [
                    {
                      "questionType": "JAVA_BASIC",
                      "difficulty": "EASY",
                      "question": "",
                      "answer": ""
                    }
                  ]
                }
                """;

        assertThrows(BusinessException.class, () -> InterviewQuestionParser.parse(rawResponse));
    }

    @Test
    void parseShouldAssignSequentialSortOrder() {
        String rawResponse = """
                {
                  "title": "测试",
                  "questions": [
                    {
                      "questionType": "JAVA_BASIC",
                      "difficulty": "EASY",
                      "question": "问题1",
                      "answer": "答案1"
                    },
                    {
                      "questionType": "MYSQL",
                      "difficulty": "MEDIUM",
                      "question": "问题2",
                      "answer": "答案2"
                    },
                    {
                      "questionType": "REDIS",
                      "difficulty": "HARD",
                      "question": "问题3",
                      "answer": "答案3"
                    }
                  ]
                }
                """;

        AiInterviewQuestionResult result = InterviewQuestionParser.parse(rawResponse);

        assertEquals(3, result.getQuestions().size());
        assertEquals(1, result.getQuestions().get(0).getSortOrder());
        assertEquals(2, result.getQuestions().get(1).getSortOrder());
        assertEquals(3, result.getQuestions().get(2).getSortOrder());
    }
}