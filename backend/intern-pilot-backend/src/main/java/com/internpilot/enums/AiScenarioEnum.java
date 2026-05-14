package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum AiScenarioEnum {

    RESUME_JOB_ANALYSIS("RESUME_JOB_ANALYSIS", "简历岗位匹配分析"),

    RESUME_OPTIMIZATION("RESUME_OPTIMIZATION", "简历优化"),

    INTERVIEW_QUESTION_GENERATION("INTERVIEW_QUESTION_GENERATION", "面试题生成"),

    JOB_RECOMMENDATION("JOB_RECOMMENDATION", "岗位推荐"),

    RAG_QA("RAG_QA", "RAG知识库问答"),

    UNKNOWN("UNKNOWN", "未知场景");

    private final String code;
    private final String description;

    AiScenarioEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}