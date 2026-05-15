package com.internpilot.service.impl;

import com.internpilot.config.AiProperties;
import com.internpilot.enums.AiScenarioEnum;
import com.internpilot.exception.AiServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.mockito.ArgumentCaptor;

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
        void selectModelShouldUseProOnlyForRagQa() {
                assertEquals("deepseek-v4-flash", deepSeekAiClient.selectModel(AiScenarioEnum.RESUME_JOB_ANALYSIS));
                assertEquals("deepseek-v4-flash",
                                deepSeekAiClient.selectModel(AiScenarioEnum.INTERVIEW_QUESTION_GENERATION));
                assertEquals("deepseek-v4-flash", deepSeekAiClient.selectModel(AiScenarioEnum.RESUME_OPTIMIZATION));
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
                assertEquals("deepseek-v4-flash", entity.getBody().get("model"));
        }

        @Test
        void systemPromptShouldContainChineseLanguageConstraint() {
                Map<String, Object> body = deepSeekAiClient.buildRequestBody(
                                "分析简历匹配度",
                                AiScenarioEnum.RESUME_JOB_ANALYSIS,
                                "deepseek-v4-flash");

                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
                String systemContent = messages.get(0).get("content");
                assertTrue(systemContent.contains("简体中文"),
                                "System prompt should contain Chinese language constraint");
        }

        @Test
        void userPromptShouldContainChineseLanguageConstraintForNonJsonScenario() {
                Map<String, Object> body = deepSeekAiClient.buildRequestBody(
                                "优化简历",
                                AiScenarioEnum.RESUME_OPTIMIZATION,
                                "deepseek-v4-flash");

                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
                String userContent = messages.get(1).get("content");
                assertTrue(userContent.contains("简体中文"),
                                "User prompt for non-JSON scenario should contain Chinese language constraint");
        }

        @Test
        void requestBodyShouldContainRealPrompt() {
                String prompt = "分析学生简历与岗位JD的匹配度，简历内容：熟悉Java Spring Boot，岗位要求：熟悉Java Spring Boot Docker";
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
}
