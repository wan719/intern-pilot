package com.internpilot.ai.prompt.template;

import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.prompt.PromptUtils;
import com.internpilot.ai.scenario.AiScenarioEnum;
import org.springframework.stereotype.Component;

@Component
public class ResumeOptimizationPromptTemplateV1 implements AiPromptTemplate {

    @Override
    public AiScenarioEnum scenario() {
        return AiScenarioEnum.RESUME_OPTIMIZATION;
    }

    @Override
    public String version() {
        return "RESUME_OPTIMIZATION_v1";
    }

    @Override
    public String systemPrompt() {
        return "You are InternPilot's resume optimizer. Return only the optimized resume in Simplified Chinese.";
    }

    @Override
    public String buildUserPrompt(AiPromptContext context) {
        return PromptUtils.buildResumeOptimizePrompt(
                context.getResumeContent(),
                context.getJobContent(),
                context.getAnalysisReport(),
                context.getExtraInstruction());
    }

    @Override
    public AiOutputFormat outputFormat() {
        return AiOutputFormat.PLAIN_TEXT;
    }
}
