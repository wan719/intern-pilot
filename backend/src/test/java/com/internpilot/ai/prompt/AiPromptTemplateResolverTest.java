package com.internpilot.ai.prompt;

import com.internpilot.ai.prompt.template.AiPromptTemplateResolver;
import com.internpilot.ai.prompt.template.InterviewQuestionPromptTemplateV1;
import com.internpilot.ai.prompt.template.InterviewQuestionRegenerationPromptTemplateV1;
import com.internpilot.ai.prompt.template.JobRecommendationPromptTemplateV1;
import com.internpilot.ai.prompt.template.RagQaPromptTemplateV1;
import com.internpilot.ai.prompt.template.ResumeJobAnalysisPromptTemplateV2;
import com.internpilot.ai.prompt.template.ResumeOptimizationPromptTemplateV1;
import com.internpilot.ai.scenario.AiScenarioEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptTemplateResolverTest {

    @Test
    void resolveShouldReturnVersionedTemplateForScenario() {
        AiPromptTemplateResolver resolver = new AiPromptTemplateResolver(List.of(
                new ResumeJobAnalysisPromptTemplateV2(),
                new JobRecommendationPromptTemplateV1(),
                new InterviewQuestionPromptTemplateV1(),
                new InterviewQuestionRegenerationPromptTemplateV1(),
                new RagQaPromptTemplateV1(),
                new ResumeOptimizationPromptTemplateV1()));

        assertEquals("RESUME_JOB_ANALYSIS_v2", resolver.resolve(AiScenarioEnum.RESUME_JOB_ANALYSIS).version());
        assertEquals("JOB_RECOMMENDATION_v1", resolver.resolve(AiScenarioEnum.JOB_RECOMMENDATION).version());
        assertEquals("INTERVIEW_QUESTION_GENERATION_v1",
                resolver.resolve(AiScenarioEnum.INTERVIEW_QUESTION_GENERATION).version());
        assertEquals("INTERVIEW_QUESTION_REGENERATION_v1",
                resolver.resolve(AiScenarioEnum.INTERVIEW_QUESTION_REGENERATION).version());
        assertEquals("RAG_QA_v1", resolver.resolve(AiScenarioEnum.RAG_QA).version());
        assertEquals("RESUME_OPTIMIZATION_v1", resolver.resolve(AiScenarioEnum.RESUME_OPTIMIZATION).version());
        assertTrue(resolver.resolve(AiScenarioEnum.RAG_QA).systemPrompt().contains("RAG"));
    }
}
