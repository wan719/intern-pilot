package com.internpilot.ai.client;

import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.ai.config.AiModelProperties;
import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.config.AiProperties;
import com.internpilot.exception.AiServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeepSeekAiClientTest {

        private AiProperties aiProperties;
        private RestTemplate restTemplate;
        private DeepSeekAiClient deepSeekAiClient;

        @BeforeEach
        void setUp() {
                aiProperties = new AiProperties();
                aiProperties.setBaseUrl("https://api.deepseek.com/");
                aiProperties.setApiKey("test-key");
                aiProperties.setModel("deepseek-v4-flash");
                aiProperties.setProModel("deepseek-v4-pro");
                restTemplate = mock(RestTemplate.class);
                deepSeekAiClient = new DeepSeekAiClient(aiProperties, restTemplate);
        }

        @Test
        void buildRequestBodyShouldUseOpenAiCompatibleChatCompletionsShape() {
                Map<String, Object> body = deepSeekAiClient.buildRequestBody(
                                "Please generate a matchScore JSON report.",
                                AiScenarioEnum.RESUME_JOB_ANALYSIS,
                                "deepseek-v4-flash");

                assertEquals("deepseek-v4-flash", body.get("model"));
                assertEquals(0.2, body.get("temperature"));
                assertEquals(Map.of("type", "json_object"), body.get("response_format"));
                assertTrue(body.get("messages") instanceof List<?>);
        }

        @Test
        void selectModelShouldUseProForComplexScenarios() {
                assertEquals("deepseek-v4-pro", deepSeekAiClient.selectModel(AiScenarioEnum.RESUME_JOB_ANALYSIS));
                assertEquals("deepseek-v4-flash",
                                deepSeekAiClient.selectModel(AiScenarioEnum.INTERVIEW_QUESTION_GENERATION));
                assertEquals("deepseek-v4-pro", deepSeekAiClient.selectModel(AiScenarioEnum.RESUME_OPTIMIZATION));
                assertEquals("deepseek-v4-flash", deepSeekAiClient.selectModel(AiScenarioEnum.JOB_RECOMMENDATION));
                assertEquals("deepseek-v4-flash", deepSeekAiClient.selectModel(AiScenarioEnum.UNKNOWN));
                assertEquals("deepseek-v4-pro", deepSeekAiClient.selectModel(AiScenarioEnum.RAG_QA));
        }

        @Test
        void jsonResponseFormatShouldOnlyBeAddedForStructuredScenarios() {
                assertTrue(deepSeekAiClient
                                .buildRequestBody("matchScore", AiScenarioEnum.RESUME_JOB_ANALYSIS, "deepseek-v4-flash")
                                .containsKey("response_format"));
                assertTrue(deepSeekAiClient
                                .buildRequestBody("questionType", AiScenarioEnum.INTERVIEW_QUESTION_GENERATION,
                                                "deepseek-v4-flash")
                                .containsKey("response_format"));
                assertTrue(deepSeekAiClient
                                .buildRequestBody("job recommendation", AiScenarioEnum.JOB_RECOMMENDATION,
                                                "deepseek-v4-flash")
                                .containsKey("response_format"));
                assertTrue(deepSeekAiClient
                                .buildRequestBody("rag knowledge base", AiScenarioEnum.RAG_QA, "deepseek-v4-pro")
                                .containsKey("response_format"));
                assertFalse(deepSeekAiClient
                                .buildRequestBody("optimize resume", AiScenarioEnum.RESUME_OPTIMIZATION,
                                                "deepseek-v4-flash")
                                .containsKey("response_format"));
        }

        @Test
        void chatShouldThrowClearErrorWhenApiKeyMissing() {
                aiProperties.setApiKey("");

                AiServiceException exception = assertThrows(
                                AiServiceException.class,
                                () -> deepSeekAiClient.chat("matchScore"));

                assertEquals("AI_SERVICE_UNAVAILABLE", exception.getErrorCode());
                assertTrue(exception.getMessage().contains("DEEPSEEK_API_KEY"));
        }

        @Test
        void chatShouldSendAuthorizationHeaderAndParseMessageContent() {
                when(restTemplate.exchange(
                                eq("https://api.deepseek.com/chat/completions"),
                                eq(HttpMethod.POST),
                                any(HttpEntity.class),
                                eq(String.class))).thenReturn(new ResponseEntity<>("""
                                                {"choices":[{"message":{"content":"{\\"ok\\":true}"}}]}
                                                """, HttpStatus.OK));

                String result = deepSeekAiClient.chat("Please generate a matchScore JSON report.");

                assertEquals("{\"ok\":true}", result);
                @SuppressWarnings("unchecked")
                ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = (ArgumentCaptor<HttpEntity<Map<String, Object>>>) (ArgumentCaptor<?>) ArgumentCaptor
                                .forClass(HttpEntity.class);
                verify(restTemplate).exchange(
                                eq("https://api.deepseek.com/chat/completions"),
                                eq(HttpMethod.POST),
                                entityCaptor.capture(),
                                eq(String.class));
                HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();
                assertNotNull(entity);
                assertEquals("Bearer test-key", entity.getHeaders().getFirst("Authorization"));
                assertEquals("deepseek-v4-pro", entity.getBody().get("model"));
        }

        @Test
        void systemPromptShouldContainChineseLanguageConstraint() {
                Map<String, Object> body = deepSeekAiClient.buildRequestBody(
                                "analyze resume",
                                AiScenarioEnum.RESUME_JOB_ANALYSIS,
                                "deepseek-v4-flash");

                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
                String systemContent = messages.get(0).get("content");
                assertTrue(systemContent.contains("Simplified Chinese"),
                                "System prompt should contain Chinese language constraint");
        }

        @Test
        void userPromptShouldContainChineseLanguageConstraintForNonJsonScenario() {
                Map<String, Object> body = deepSeekAiClient.buildRequestBody(
                                "optimize resume",
                                AiScenarioEnum.RESUME_OPTIMIZATION,
                                "deepseek-v4-flash");

                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
                String userContent = messages.get(1).get("content");
                assertTrue(userContent.contains("Simplified Chinese"),
                                "User prompt for non-JSON scenario should contain Chinese language constraint");
        }

        @Test
        void requestBodyShouldContainRealPrompt() {
                String prompt = "Analyze Java Spring Boot resume against Docker job requirements";
                Map<String, Object> body = deepSeekAiClient.buildRequestBody(
                                prompt,
                                AiScenarioEnum.RESUME_JOB_ANALYSIS,
                                "deepseek-v4-flash");

                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
                String userContent = messages.get(1).get("content");
                assertTrue(userContent.contains("Java Spring Boot"),
                                "Request body should contain the real prompt content");
                assertTrue(userContent.contains("Docker"),
                                "Request body should contain the real prompt content");
        }

        @Test
        void detectScenarioShouldCoverEveryHeuristic() {
                assertEquals(AiScenarioEnum.UNKNOWN, deepSeekAiClient.detectScenario(null));
                assertEquals(AiScenarioEnum.UNKNOWN, deepSeekAiClient.detectScenario(" "));
                assertEquals(AiScenarioEnum.INTERVIEW_QUESTION_REGENERATION,
                                deepSeekAiClient.detectScenario("INTERVIEW_QUESTION_REGENERATION"));
                assertEquals(AiScenarioEnum.RAG_QA, deepSeekAiClient.detectScenario("use rag knowledge base"));
                assertEquals(AiScenarioEnum.JOB_RECOMMENDATION,
                                deepSeekAiClient.detectScenario("job recommendation please"));
                assertEquals(AiScenarioEnum.RESUME_OPTIMIZATION,
                                deepSeekAiClient.detectScenario("resume optimization"));
                assertEquals(AiScenarioEnum.INTERVIEW_QUESTION_GENERATION,
                                deepSeekAiClient.detectScenario("follow-up interview"));
                assertEquals(AiScenarioEnum.RESUME_JOB_ANALYSIS,
                                deepSeekAiClient.detectScenario("matching report"));
                assertEquals(AiScenarioEnum.RESUME_JOB_ANALYSIS,
                                deepSeekAiClient.detectScenario("resume and job"));
                assertEquals(AiScenarioEnum.RESUME_JOB_ANALYSIS,
                                deepSeekAiClient.detectScenario("json with JD"));
                assertEquals(AiScenarioEnum.UNKNOWN, deepSeekAiClient.detectScenario("plain assistant request"));
        }

        @Test
        void selectModelShouldPreferConfiguredScenarioAndFallbackDefaults() {
                AiModelProperties modelProperties = new AiModelProperties();
                modelProperties.getScenarioModel().put("RAG_QA", "custom-rag");
                DeepSeekAiClient configuredClient = new DeepSeekAiClient(aiProperties, restTemplate, modelProperties);

                assertEquals("custom-rag", configuredClient.selectModel(AiScenarioEnum.RAG_QA));

                aiProperties.setModel("");
                aiProperties.setProModel("");
                modelProperties.setDefaultModel("");
                assertEquals("deepseek-v4-pro", configuredClient.selectModel(AiScenarioEnum.RESUME_JOB_ANALYSIS));
                assertEquals("deepseek-v4-flash", configuredClient.selectModel(null));
        }

        @Test
        void chatShouldRetryRetryableErrorsThenUseFallbackModel() {
                AiModelProperties modelProperties = new AiModelProperties();
                modelProperties.setFallbackModel("fallback-model");
                modelProperties.getRetry().setEnabled(true);
                modelProperties.getRetry().setMaxAttempts(2);
                DeepSeekAiClient configuredClient = new DeepSeekAiClient(aiProperties, restTemplate, modelProperties);

                when(restTemplate.exchange(
                                eq("https://api.deepseek.com/chat/completions"),
                                eq(HttpMethod.POST),
                                any(HttpEntity.class),
                                eq(String.class)))
                                .thenReturn(new ResponseEntity<>("", HttpStatus.OK))
                                .thenThrow(new ResourceAccessException("timeout"))
                                .thenReturn(new ResponseEntity<>("""
                                                {"choices":[{"message":{"content":"fallback-ok"}}]}
                                                """, HttpStatus.OK));

                String result = configuredClient.chat(AiChatRequest.builder()
                                .scenario(AiScenarioEnum.RAG_QA)
                                .model("primary-model")
                                .fallbackModel("fallback-model")
                                .systemPrompt("system")
                                .userPrompt("prompt")
                                .promptVersion("v1")
                                .promptHash("hash")
                                .outputFormat(AiOutputFormat.JSON_OBJECT)
                                .allowFallback(true)
                                .build());

                assertEquals("fallback-ok", result);
                verify(restTemplate, times(3)).exchange(
                                eq("https://api.deepseek.com/chat/completions"),
                                eq(HttpMethod.POST),
                                any(HttpEntity.class),
                                eq(String.class));
        }

        @Test
        void chatShouldNotRetryAuthErrorAndShouldMapServerAndParseErrors() {
                when(restTemplate.exchange(
                                eq("https://api.deepseek.com/chat/completions"),
                                eq(HttpMethod.POST),
                                any(HttpEntity.class),
                                eq(String.class))).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

                AiServiceException auth = assertThrows(AiServiceException.class,
                                () -> deepSeekAiClient.chat("matchScore"));
                assertEquals("AI_SERVICE_AUTH_FAILED", auth.getErrorCode());
                verify(restTemplate, times(1)).exchange(
                                eq("https://api.deepseek.com/chat/completions"),
                                eq(HttpMethod.POST),
                                any(HttpEntity.class),
                                eq(String.class));

                restTemplate = mock(RestTemplate.class);
                deepSeekAiClient = new DeepSeekAiClient(aiProperties, restTemplate);
                when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));
                AiServiceException server = assertThrows(AiServiceException.class,
                                () -> deepSeekAiClient.chat("matchScore"));
                assertEquals("AI_SERVICE_UNAVAILABLE", server.getErrorCode());

                restTemplate = mock(RestTemplate.class);
                deepSeekAiClient = new DeepSeekAiClient(aiProperties, restTemplate);
                when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("not-json", HttpStatus.OK));
                AiServiceException parse = assertThrows(AiServiceException.class,
                                () -> deepSeekAiClient.chat("matchScore"));
                assertEquals("AI_RESPONSE_PARSE_FAILED", parse.getErrorCode());
        }

        @Test
        void chatShouldHandleNullRequestAndNon2xxResponse() {
                when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));

                AiServiceException exception = assertThrows(AiServiceException.class,
                                () -> deepSeekAiClient.chat((AiChatRequest) null));

                assertEquals("AI_SERVICE_UNAVAILABLE", exception.getErrorCode());
        }
}
