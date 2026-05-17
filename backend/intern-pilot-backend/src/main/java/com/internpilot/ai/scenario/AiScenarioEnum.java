package com.internpilot.ai.scenario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "AI场景枚举，定义系统中不同的AI应用场景，每个场景对应一个唯一的code和描")
public enum AiScenarioEnum {//AI场景枚举，定义系统中不同的AI应用场景
// 每个场景对应一个唯一的code和描

    RESUME_JOB_ANALYSIS("RESUME_JOB_ANALYSIS", "简历岗位匹配分"),

    RESUME_OPTIMIZATION("RESUME_OPTIMIZATION", "简历优"),

    INTERVIEW_QUESTION_GENERATION("INTERVIEW_QUESTION_GENERATION", "面试题生"),

    JOB_RECOMMENDATION("JOB_RECOMMENDATION", "岗位推荐"),

    RAG_QA("RAG_QA", "RAG知识库问"),

    UNKNOWN("UNKNOWN", "未知场景");

    private final String code;
    private final String description;

    AiScenarioEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}