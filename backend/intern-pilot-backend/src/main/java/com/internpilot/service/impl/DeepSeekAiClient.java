package com.internpilot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.config.AiProperties;
import com.internpilot.enums.AiScenarioEnum;
import com.internpilot.exception.AiServiceException;
import com.internpilot.service.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "deepseek", matchIfMissing = true)
@RequiredArgsConstructor
public class DeepSeekAiClient implements AiClient {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(String prompt) {
        String safePrompt = prompt == null ? "" : prompt;
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new AiServiceException(
                    "AI_SERVICE_UNAVAILABLE",
                    "DEEPSEEK_API_KEY is not configured. Please set the environment variable.");
        }

        String url = normalizeBaseUrl(aiProperties.getBaseUrl()) + "/chat/completions";
        AiScenarioEnum scenario = detectScenario(prompt);
        String model = selectModel(scenario);

        log.info("DeepSeek diag: provider=deepseek, model={}, scenario={}, promptLength={}",
                model, scenario, safePrompt.length());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildRequestBody(safePrompt, scenario, model), headers);

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
        } catch (RestClientException e) {
            throw new AiServiceException("AI_SERVICE_UNAVAILABLE", "DeepSeek API is unavailable.");
        } catch (Exception e) {
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "DeepSeek API response could not be parsed.");
        }
    }

    Map<String, Object> buildRequestBody(String prompt, AiScenarioEnum scenario, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt(scenario)),
                Map.of("role", "user", "content", userPrompt(prompt, scenario))));
        body.put("temperature", 0.2);
        if (requiresJsonResponse(scenario)) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    AiScenarioEnum detectScenario(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return AiScenarioEnum.UNKNOWN;
        }

        String lowerPrompt = prompt.toLowerCase();
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
        if (scenario == AiScenarioEnum.RAG_QA) {
            return nonBlank(aiProperties.getProModel(), "deepseek-v4-pro");
        }
        return nonBlank(aiProperties.getModel(), "deepseek-v4-flash");
    }

    boolean requiresJsonResponse(AiScenarioEnum scenario) {
        return scenario == AiScenarioEnum.RESUME_JOB_ANALYSIS
                || scenario == AiScenarioEnum.INTERVIEW_QUESTION_GENERATION
                || scenario == AiScenarioEnum.JOB_RECOMMENDATION
                || scenario == AiScenarioEnum.RAG_QA;
    }

    private String systemPrompt(AiScenarioEnum scenario) {
        if (!requiresJsonResponse(scenario)) {
            return "You are InternPilot's AI assistant. You MUST respond in Simplified Chinese (简体中文). "
                    + "Only technical terms may remain in English. Follow the user's task precisely.";
        }
        return "You are InternPilot's AI assistant. You MUST respond in Simplified Chinese (简体中文). "
                + "All field values must be in Chinese except technical terms. "
                + "Return only valid JSON. Do not include Markdown code fences. "
                + "Do not include explanations outside JSON.";
    }

    private String userPrompt(String prompt, AiScenarioEnum scenario) {
        String value = prompt == null ? "" : prompt;
        if (!requiresJsonResponse(scenario)) {
            return value + "\n\n你必须使用简体中文回答。除技术名词外，不要输出英文。";
        }
        return value + "\n\nOutput constraints: return only valid JSON; "
                + "do not use Markdown code fences; do not add any extra explanation text. "
                + "All field values MUST be in Simplified Chinese (简体中文) except technical terms.";
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
}
