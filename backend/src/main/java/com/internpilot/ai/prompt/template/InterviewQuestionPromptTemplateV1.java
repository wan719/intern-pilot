package com.internpilot.ai.prompt.template;

import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.prompt.InterviewQuestionPromptBuilder;
import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import org.springframework.stereotype.Component;

@Component
public class InterviewQuestionPromptTemplateV1 implements AiPromptTemplate {

    @Override
    public AiScenarioEnum scenario() {
        return AiScenarioEnum.INTERVIEW_QUESTION_GENERATION;
    }

    @Override
    public String version() {
        return "INTERVIEW_QUESTION_GENERATION_v1";
    }

    @Override
    public String systemPrompt() {
        return "You are InternPilot's interview-question generator. Return only valid JSON. "
                + "Use Simplified Chinese for questions, answers, hints, and sources.";
    }

    @Override
    public String buildUserPrompt(AiPromptContext context) {
        Object request = context.metadataValue("request");
        return InterviewQuestionPromptBuilder.build(
                context.getResumeContent(),
                context.getJobContent(),
                context.getAnalysisReport(),
                request instanceof InterviewQuestionGenerateRequest typed ? typed : null);
    }

    @Override
    public AiOutputFormat outputFormat() {
        return AiOutputFormat.JSON_OBJECT;
    }
}
