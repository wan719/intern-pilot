package com.internpilot.ai.parser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.internpilot.exception.AiServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiJsonSanitizerTest {

    @Test
    void extractJsonObjectShouldStripMarkdownFenceAndText() {
        String raw = "Here is JSON:\n```json\n{\"matchScore\":120,\"summary\":\"ok\"}\n```\nthanks";

        String json = AiJsonSanitizer.extractJsonObject(raw);

        assertEquals("{\"matchScore\":120,\"summary\":\"ok\"}", json);
    }

    @Test
    void sanitizerShouldFillMissingFieldsAndClampScores() {
        ObjectNode node = AiJsonSanitizer.sanitizeObject("{\"matchScore\":130}");

        AiJsonSanitizer.clampInt(node, "matchScore", 0, 100, 60);
        AiJsonSanitizer.ensureText(node, "matchLevel", "MEDIUM");
        AiJsonSanitizer.ensureArray(node, "strengths", List.of("fallback"));

        assertEquals(100, node.get("matchScore").asInt());
        assertEquals("MEDIUM", node.get("matchLevel").asText());
        assertTrue(node.get("strengths").isArray());
        assertEquals("fallback", node.get("strengths").get(0).asText());
    }

    @Test
    void extractJsonShouldHandleArraysAndFenceVariants() {
        assertEquals("[1,2]", AiJsonSanitizer.extractJsonArray("```JSON\n[1,2]\n```"));
        assertEquals("{\"ok\":true}", AiJsonSanitizer.extractJsonObject("```\n{\"ok\":true}\n```"));
        assertEquals("[{\"a\":1}]", AiJsonSanitizer.extractJsonArray("prefix [{\"a\":1}] suffix"));
    }

    @Test
    void sanitizerShouldRejectEmptyInvalidAndNonObjectResponses() {
        assertThrows(AiServiceException.class, () -> AiJsonSanitizer.extractJsonObject(null));
        assertThrows(AiServiceException.class, () -> AiJsonSanitizer.extractJsonObject("   "));
        assertThrows(AiServiceException.class, () -> AiJsonSanitizer.extractJsonObject("no json here"));
        assertThrows(AiServiceException.class, () -> AiJsonSanitizer.sanitizeObject("[1,2]"));
        assertThrows(AiServiceException.class, () -> AiJsonSanitizer.parseObject("{bad", ObjectNode.class));
    }

    @Test
    void ensureHelpersShouldBeNoopForNullInputsAndPreserveValidValues() {
        ObjectNode node = AiJsonSanitizer.sanitizeObject("{\"name\":\"ok\",\"items\":[\"a\"],\"score\":\"bad\"}");

        AiJsonSanitizer.ensureText(null, "name", "fallback");
        AiJsonSanitizer.ensureText(node, null, "fallback");
        AiJsonSanitizer.ensureText(node, "name", "fallback");
        AiJsonSanitizer.ensureArray(null, "items");
        AiJsonSanitizer.ensureArray(node, null);
        AiJsonSanitizer.ensureArray(node, "items");
        AiJsonSanitizer.ensureArray(node, "missingItems");
        AiJsonSanitizer.ensureArray(node, "fallbackItems", null);
        AiJsonSanitizer.clampInt(null, "score", 0, 100, 60);
        AiJsonSanitizer.clampInt(node, null, 0, 100, 60);
        AiJsonSanitizer.clampInt(node, "score", 0, 100, 60);

        assertEquals("ok", node.get("name").asText());
        assertEquals("a", node.get("items").get(0).asText());
        assertTrue(node.get("missingItems").isArray());
        assertTrue(node.get("fallbackItems").isArray());
        assertEquals(60, node.get("score").asInt());
        assertTrue(AiJsonSanitizer.toJson(node).contains("\"name\":\"ok\""));
    }
}
