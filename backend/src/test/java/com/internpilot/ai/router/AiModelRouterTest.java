package com.internpilot.ai.router;

import com.internpilot.ai.config.AiModelProperties;
import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelRouterTest {

    @Test
    void routeShouldUseScenarioModelBeforeDefaultModel() {
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("flash");
        properties.getScenarioModel().put("RAG_QA", "pro");

        DefaultAiModelRouter router = new DefaultAiModelRouter(properties, new AiProperties());

        assertEquals("pro", router.route(AiScenarioEnum.RAG_QA));
        assertEquals("flash", router.route(AiScenarioEnum.JOB_RECOMMENDATION));
    }

    @Test
    void fallbackShouldUseConfiguredFallbackModel() {
        AiModelProperties properties = new AiModelProperties();
        properties.setFallbackModel("fallback-flash");

        DefaultAiModelRouter router = new DefaultAiModelRouter(properties, new AiProperties());

        assertEquals("fallback-flash", router.fallback(AiScenarioEnum.RESUME_JOB_ANALYSIS));
        assertTrue(router.allowFallback(AiScenarioEnum.RESUME_JOB_ANALYSIS));
        assertFalse(router.allowFallback(AiScenarioEnum.UNKNOWN));
    }
}
