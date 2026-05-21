package com.internpilot.ai.scenario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Unified AI scenario enum，ai场景枚举")
public enum AiScenarioEnum {

    RESUME_JOB_ANALYSIS("RESUME_JOB_ANALYSIS", "Resume-job match analysis",
            "Analyze the match between one resume and one job."),

    JOB_RECOMMENDATION("JOB_RECOMMENDATION", "Job recommendation",
            "Recommend jobs based on resume and job pool."),

    INTERVIEW_QUESTION_GENERATION("INTERVIEW_QUESTION_GENERATION", "Interview question generation",
            "Generate interview questions from resume and job."),

    INTERVIEW_QUESTION_REGENERATION("INTERVIEW_QUESTION_REGENERATION", "Interview question regeneration",
            "Regenerate interview questions from an existing report."),

    RAG_QA("RAG_QA", "RAG question answering",
            "Answer questions from RAG knowledge context."),

    RESUME_OPTIMIZATION("RESUME_OPTIMIZATION", "Resume optimization",
            "Optimize a resume for a target job."),

    UNKNOWN("UNKNOWN", "Unknown", "Unknown AI scenario.");

    private final String code;
    private final String description;
    private final String usage;

    AiScenarioEnum(String code, String description, String usage) {
        this.code = code;
        this.description = description;
        this.usage = usage;
    }

    public static AiScenarioEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        for (AiScenarioEnum scenario : values()) {
            if (scenario.code.equalsIgnoreCase(code) || scenario.name().equalsIgnoreCase(code)) {
                return scenario;
            }
        }
        return UNKNOWN;
    }
}
