package com.internpilot.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.ai.config.AiModelProperties;
import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.config.AiProperties;
import com.internpilot.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "deepseek", matchIfMissing = true)
public class DeepSeekAiClient implements AiClient {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final AiModelProperties modelProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DeepSeekAiClient(
            AiProperties aiProperties,
            RestTemplate restTemplate,
            AiModelProperties modelProperties) {
        this.aiProperties = aiProperties;
        this.restTemplate = restTemplate;
        this.modelProperties = modelProperties == null ? new AiModelProperties() : modelProperties;
    }

    public DeepSeekAiClient(AiProperties aiProperties, RestTemplate restTemplate) {
        this(aiProperties, restTemplate, new AiModelProperties());
    }

    @Override
    public String chat(String prompt) {
        AiScenarioEnum scenario = detectScenario(prompt);
        return chat(AiChatRequest.builder()
                .scenario(scenario)
                .model(selectModel(scenario))
                .fallbackModel(nonBlank(modelProperties.getFallbackModel(), aiProperties.getModel()))
                .promptVersion("LEGACY")
                .promptHash(hashText(prompt))
                .cacheHit(false)
                .systemPrompt(systemPrompt(scenario))
                .userPrompt(prompt)
                .outputFormat(requiresJsonResponse(scenario) ? AiOutputFormat.JSON_OBJECT : AiOutputFormat.PLAIN_TEXT)
                .allowFallback(true)
                .build());
    }

    @Override
    public String chat(AiChatRequest request) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new AiServiceException(
                    "AI_SERVICE_UNAVAILABLE",
                    "DEEPSEEK_API_KEY is not configured. Please set the environment variable.");
        }

        AiChatRequest safeRequest = request == null ? AiChatRequest.builder().build() : request;
        AiScenarioEnum scenario = safeRequest.getScenario() == null ? AiScenarioEnum.UNKNOWN : safeRequest.getScenario();
        String primaryModel = nonBlank(safeRequest.getModel(), selectModel(scenario));
        String fallbackModel = nonBlank(safeRequest.getFallbackModel(), modelProperties.getFallbackModel());
        int maxAttempts = modelProperties.getRetry().isEnabled()
                ? Math.max(1, modelProperties.getRetry().getMaxAttempts())
                : 1;

        AiServiceException lastException = null;
        int retryCount = 0;
        boolean fallbackUsed = false;

        for (String model : candidateModels(primaryModel, fallbackModel, safeRequest.isAllowFallback())) {
            fallbackUsed = !model.equals(primaryModel);
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                long start = System.currentTimeMillis();
                try {
                    String result = callOnce(safeRequest, scenario, model);
                    long durationMs = System.currentTimeMillis() - start;
                    log.info("AI call diag: userId={}, provider=deepseek, scenario={}, model={}, promptVersion={}, "
                                    + "promptHash={}, responseHash={}, cacheHit={}, durationMs={}, retryCount={}, fallbackUsed={}, success=true, errorCode=",
                            safeRequest.getUserId(), scenario, model, safeRequest.getPromptVersion(),
                            safeRequest.getPromptHash(), hashText(result), safeRequest.getCacheHit(), durationMs,
                            retryCount, fallbackUsed);
                    return result;
                } catch (AiServiceException e) {
                    lastException = e;
                    long durationMs = System.currentTimeMillis() - start;
                    boolean retryable = isRetryable(e);
                    log.warn("AI call diag: userId={}, provider=deepseek, scenario={}, model={}, promptVersion={}, "
                                    + "promptHash={}, responseHash={}, cacheHit={}, durationMs={}, retryCount={}, fallbackUsed={}, "
                                    + "success=false, errorCode={}, retryable={}",
                            safeRequest.getUserId(), scenario, model, safeRequest.getPromptVersion(),
                            safeRequest.getPromptHash(), "", safeRequest.getCacheHit(), durationMs, retryCount,
                            fallbackUsed, e.getErrorCode(), retryable);
                    if (!retryable || attempt >= maxAttempts) {
                        break;
                    }
                    retryCount++;
                }
            }
        }

        throw lastException == null
                ? new AiServiceException("AI_SERVICE_UNAVAILABLE", "DeepSeek API is unavailable.")
                : lastException;
    }

    Map<String, Object> buildRequestBody(String prompt, AiScenarioEnum scenario, String model) {
        return buildRequestBody(AiChatRequest.builder()
                .scenario(scenario)
                .model(model)
                .systemPrompt(systemPrompt(scenario))
                .userPrompt(prompt)
                .outputFormat(requiresJsonResponse(scenario) ? AiOutputFormat.JSON_OBJECT : AiOutputFormat.PLAIN_TEXT)
                .build(), scenario, model);
    }

    Map<String, Object> buildRequestBody(AiChatRequest request, AiScenarioEnum scenario, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", nonBlank(request.getSystemPrompt(), systemPrompt(scenario))),
                Map.of("role", "user", "content", userPrompt(request.getUserPrompt(), scenario))));
        body.put("temperature", 0.2);
        if (requiresJsonResponse(request, scenario)) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    AiScenarioEnum detectScenario(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return AiScenarioEnum.UNKNOWN;
        }

        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("interview_question_regeneration")) {
            return AiScenarioEnum.INTERVIEW_QUESTION_REGENERATION;
        }
        if (lowerPrompt.contains("rag") || lowerPrompt.contains("knowledge base")) {
            return AiScenarioEnum.RAG_QA;
        }
        if (lowerPrompt.contains("recommend") || lowerPrompt.contains("job recommendation")) {
            return AiScenarioEnum.JOB_RECOMMENDATION;
        }
        if (lowerPrompt.contains("optimize")
                || lowerPrompt.contains("optimized resume")
                || lowerPrompt.contains("resume optimization")) {
            return AiScenarioEnum.RESUME_OPTIMIZATION;
        }
        if (lowerPrompt.contains("interview")
                || lowerPrompt.contains("questiontype")
                || lowerPrompt.contains("follow-up")
                || lowerPrompt.contains("followup")) {
            return AiScenarioEnum.INTERVIEW_QUESTION_GENERATION;
        }
        if (prompt.contains("matchScore")
                || lowerPrompt.contains("matching report")
                || lowerPrompt.contains("match analysis")
                || (lowerPrompt.contains("resume") && lowerPrompt.contains("job"))
                || (lowerPrompt.contains("json") && prompt.contains("JD"))) {
            return AiScenarioEnum.RESUME_JOB_ANALYSIS;
        }
        return AiScenarioEnum.UNKNOWN;
    }

    String selectModel(AiScenarioEnum scenario) {
        String configured = modelProperties.getScenarioModel().get(scenario == null ? "" : scenario.name());
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        if (scenario == AiScenarioEnum.RAG_QA || scenario == AiScenarioEnum.RESUME_JOB_ANALYSIS
                || scenario == AiScenarioEnum.RESUME_OPTIMIZATION) {
            return nonBlank(aiProperties.getProModel(), nonBlank(modelProperties.getDefaultModel(), "deepseek-v4-pro"));
        }
        return nonBlank(modelProperties.getDefaultModel(), nonBlank(aiProperties.getModel(), "deepseek-v4-flash"));
    }

    boolean requiresJsonResponse(AiScenarioEnum scenario) {
        return scenario == AiScenarioEnum.RESUME_JOB_ANALYSIS
                || scenario == AiScenarioEnum.INTERVIEW_QUESTION_GENERATION
                || scenario == AiScenarioEnum.INTERVIEW_QUESTION_REGENERATION
                || scenario == AiScenarioEnum.JOB_RECOMMENDATION
                || scenario == AiScenarioEnum.RAG_QA;
    }

    private boolean requiresJsonResponse(AiChatRequest request, AiScenarioEnum scenario) {
        if (request.getOutputFormat() != null) {
            return request.getOutputFormat() == AiOutputFormat.JSON_OBJECT
                    || request.getOutputFormat() == AiOutputFormat.JSON_ARRAY;
        }
        return requiresJsonResponse(scenario);
    }

    private String callOnce(AiChatRequest request, AiScenarioEnum scenario, String model) {
        String url = normalizeBaseUrl(aiProperties.getBaseUrl()) + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildRequestBody(request, scenario, model), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new AiServiceException("AI_SERVICE_UNAVAILABLE", "DeepSeek API request failed.");
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new AiServiceException("AI_RESPONSE_EMPTY", "DeepSeek API returned an empty response.");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new AiServiceException("AI_RESPONSE_EMPTY", "DeepSeek API returned empty message content.");
            }
            return content;
        } catch (AiServiceException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new AiServiceException("AI_SERVICE_TIMEOUT", "DeepSeek API request timed out.");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new AiServiceException("AI_SERVICE_AUTH_FAILED", "DeepSeek API rejected the request.");
            }
            throw new AiServiceException("AI_SERVICE_UNAVAILABLE", "DeepSeek API is unavailable.");
        } catch (Exception e) {
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "DeepSeek API response could not be parsed.");
        }
    }

    private List<String> candidateModels(String primaryModel, String fallbackModel, boolean allowFallback) {
        if (!allowFallback || fallbackModel == null || fallbackModel.isBlank() || fallbackModel.equals(primaryModel)) {
            return List.of(primaryModel);
        }
        return List.of(primaryModel, fallbackModel);
    }

    private boolean isRetryable(AiServiceException e) {
        return "AI_SERVICE_TIMEOUT".equals(e.getErrorCode())
                || "AI_SERVICE_UNAVAILABLE".equals(e.getErrorCode())
                || "AI_RESPONSE_EMPTY".equals(e.getErrorCode());
    }

    private String systemPrompt(AiScenarioEnum scenario) {
        if (!requiresJsonResponse(scenario)) {
            return "You are InternPilot's AI assistant. You MUST respond in Simplified Chinese. "
                    + "Only technical terms may remain in English. Follow the user's task precisely.";
        }
        return "You are InternPilot's AI assistant. You MUST respond in Simplified Chinese. "
                + "All field values must be in Chinese except technical terms. "
                + "Return only valid JSON. Do not include Markdown code fences. "
                + "Do not include explanations outside JSON.";
    }

    private String userPrompt(String prompt, AiScenarioEnum scenario) {
        String value = prompt == null ? "" : prompt;
        if (!requiresJsonResponse(scenario)) {
            return value + "\n\nYou must respond in Simplified Chinese except technical terms.";
        }
        return value + "\n\nOutput constraints: return only valid JSON; "
                + "do not use Markdown code fences; do not add any extra explanation text. "
                + "All field values MUST be in Simplified Chinese except technical terms.";
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = nonBlank(baseUrl, "https://api.deepseek.com");
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String hashText(String text) {
        return DigestUtils.md5DigestAsHex((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
    }
}
