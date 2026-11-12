package com.internpilot.ai.prompt.template;

import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.scenario.AiScenarioEnum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI Prompt Template")
public interface AiPromptTemplate {

    AiScenarioEnum scenario();

    String version();

    String systemPrompt();
    
    String buildUserPrompt(AiPromptContext context);

    AiOutputFormat outputFormat();
}
