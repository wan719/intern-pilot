package com.internpilot.ai.prompt.template;

import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.scenario.AiScenarioEnum;
import org.springframework.stereotype.Component;

@Component
public class JobRecommendationPromptTemplateV1 implements AiPromptTemplate {

    @Override
    public AiScenarioEnum scenario() {
        return AiScenarioEnum.JOB_RECOMMENDATION;
    }

    @Override
    public String version() {
        return "JOB_RECOMMENDATION_v1";
    }

    @Override
    public String systemPrompt() {
        return "You are InternPilot's job recommendation assistant. Return only valid JSON in Simplified Chinese.";
    }

    @Override
    public String buildUserPrompt(AiPromptContext context) {
        return """
                Build job recommendations from the resume and job pool.
                Return JSON: {"recommendations":[{"jobId":1,"matchScore":86,"reason":"","matchedTags":[],"learningSuggestions":[],"applySuggestion":""}]}

                Resume:
                %s

                Jobs:
                %s
                """.formatted(nullToEmpty(context.getResumeContent()), nullToEmpty(context.getJobContent()));
    }

    @Override
    public AiOutputFormat outputFormat() {
        return AiOutputFormat.JSON_OBJECT;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
