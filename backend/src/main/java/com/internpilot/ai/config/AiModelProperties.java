package com.internpilot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
@Schema(description = "Configuration properties for AI models")
public class AiModelProperties {

    private String defaultModel = "deepseek-v4-flash";
    private String fallbackModel = "deepseek-v4-flash";
    private Map<String, String> models = new HashMap<>();
    private Map<String, String> scenarioModel = new HashMap<>();
    private Retry retry = new Retry();

    @Data
    public static class Retry {
        private boolean enabled = true;
        private int maxAttempts = 2;
    }
}
