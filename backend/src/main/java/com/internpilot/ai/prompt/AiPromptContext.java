package com.internpilot.ai.prompt;

import com.internpilot.ai.scenario.AiScenarioEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
@Builder
@Schema(description = "Context for AI prompt generation,ai提示词生成的上下文对象")
public class AiPromptContext {

    private AiScenarioEnum scenario;
    private String userProfile;
    private String resumeContent;
    private String jobContent;
    private String analysisReport;
    private String ragContext;
    private String userQuestion;
    private String extraInstruction;
    private Map<String, Object> metadata;

    public Object metadataValue(String key) {
        Map<String, Object> safeMetadata = metadata == null ? Collections.emptyMap() : metadata;
        return safeMetadata.get(key);
    }
}
