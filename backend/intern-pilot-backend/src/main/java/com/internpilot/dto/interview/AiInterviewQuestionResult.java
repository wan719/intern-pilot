package com.internpilot.dto.interview;

import lombok.Data;

import java.util.List;

@Data
public class AiInterviewQuestionResult {

    private String title;

    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {

        private String questionType;

        private String difficulty;

        private String question;

        private String answer;

        private List<String> answerPoints;

        private List<String> relatedSkills;
    }
}