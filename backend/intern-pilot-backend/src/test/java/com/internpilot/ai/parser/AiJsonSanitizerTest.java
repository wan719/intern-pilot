package com.internpilot.ai.parser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
