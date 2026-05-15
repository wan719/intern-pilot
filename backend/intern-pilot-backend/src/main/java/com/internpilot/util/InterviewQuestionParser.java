package com.internpilot.util;

import com.internpilot.dto.interview.AiInterviewQuestionResult;
import com.internpilot.enums.InterviewQuestionTypeEnum;
import com.internpilot.enums.QuestionDifficultyEnum;
import com.internpilot.exception.BusinessException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InterviewQuestionParser {

    private InterviewQuestionParser() {
    }

    public static AiInterviewQuestionResult parse(String rawResponse) {
        AiInterviewQuestionResult result = JsonUtils.parseAiJson(rawResponse, AiInterviewQuestionResult.class);

        if (result.getQuestions() == null || result.getQuestions().isEmpty()) {
            throw new BusinessException("AI 未生成有效面试题");
        }

        List<AiInterviewQuestionResult.QuestionItem> validQuestions = new ArrayList<>();
        int sortOrder = 1;

        for (AiInterviewQuestionResult.QuestionItem item : result.getQuestions()) {
            if (item.getQuestion() == null || item.getQuestion().isBlank()) {
                continue;
            }

            item.setQuestionType(normalizeQuestionType(item.getQuestionType()));
            item.setDifficulty(normalizeDifficulty(item.getDifficulty()));
            item.setAnswerPoints(nullToEmpty(item.getAnswerPoints()));
            item.setRelatedSkills(nullToEmpty(item.getRelatedSkills()));
            item.setFollowUps(nullToEmpty(item.getFollowUps()));
            item.setKeywords(nullToEmpty(item.getKeywords()));
            item.setSortOrder(sortOrder++);

            validQuestions.add(item);
        }

        if (validQuestions.isEmpty()) {
            throw new BusinessException("AI 未生成有效面试题");
        }

        result.setQuestions(validQuestions);
        return result;
    }

    private static String normalizeQuestionType(String type) {
        if (InterviewQuestionTypeEnum.isValid(type)) {
            return type;
        }
        return InterviewQuestionTypeEnum.JOB_SKILL.getCode();
    }

    private static String normalizeDifficulty(String difficulty) {
        if (QuestionDifficultyEnum.isValid(difficulty)) {
            return difficulty;
        }
        return QuestionDifficultyEnum.MEDIUM.getCode();
    }

    private static List<String> nullToEmpty(List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }
}