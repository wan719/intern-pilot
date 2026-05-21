package com.internpilot.ai.prompt.template;

import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.prompt.PromptUtils;
import com.internpilot.ai.scenario.AiScenarioEnum;
import org.springframework.stereotype.Component;

@Component
public class ResumeJobAnalysisPromptTemplateV2 implements AiPromptTemplate {

    @Override
    public AiScenarioEnum scenario() {
        return AiScenarioEnum.RESUME_JOB_ANALYSIS;
    }

    @Override
    public String version() {
        return "RESUME_JOB_ANALYSIS_v2";
    }

    @Override
    public String systemPrompt() {
        return "You are InternPilot's resume-job match analyst. Return only valid JSON. "
                + "Use Simplified Chinese for values except technical terms. Scores must be integers from 0 to 100.";
    }

    @Override
    public String buildUserPrompt(AiPromptContext context) {
        return PromptUtils.buildAnalysisPrompt(
                context.getResumeContent(),
                context.getJobContent(),
                context.getRagContext());
    }

    @Override
    public AiOutputFormat outputFormat() {
        return AiOutputFormat.JSON_OBJECT;
    }
}
