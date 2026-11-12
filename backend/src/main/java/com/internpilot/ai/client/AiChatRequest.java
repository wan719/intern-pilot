package com.internpilot.ai.client;

import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.scenario.AiScenarioEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatRequest {

    private AiScenarioEnum scenario;
    private String model;
    private String fallbackModel;
    private String promptVersion;
    private Long userId;
    private String promptHash;
    private Boolean cacheHit;
    private String systemPrompt;
    private String userPrompt;
    private AiOutputFormat outputFormat;
    private boolean allowFallback;
}
