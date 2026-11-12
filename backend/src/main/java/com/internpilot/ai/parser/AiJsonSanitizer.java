package com.internpilot.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.internpilot.exception.AiServiceException;

import java.util.Collection;

public class AiJsonSanitizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiJsonSanitizer() {
    }

    public static String extractJsonObject(String rawText) {
        return extractJson(rawText, '{', '}');
    }

    public static String extractJsonArray(String rawText) {
        return extractJson(rawText, '[', ']');
    }

    public static <T> T parseObject(String rawText, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(extractJsonObject(rawText), clazz);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "AI response JSON parse failed.");
        }
    }

    public static ObjectNode sanitizeObject(String rawText) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(extractJsonObject(rawText));
            if (!node.isObject()) {
                throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "AI response content is not a JSON object.");
            }
            return (ObjectNode) node;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "AI response JSON parse failed.");
        }
    }

    public static void ensureText(ObjectNode node, String field, String fallback) {
        if (node == null || field == null) {
            return;
        }
        if (!node.has(field) || node.get(field).isNull() || node.get(field).asText().isBlank()) {
            node.put(field, fallback == null ? "" : fallback);
        }
    }

    public static void ensureArray(ObjectNode node, String field) {
        if (node == null || field == null) {
            return;
        }
        if (!node.has(field) || node.get(field).isNull() || !node.get(field).isArray()) {
            node.set(field, OBJECT_MAPPER.createArrayNode());
        }
    }

    public static void ensureArray(ObjectNode node, String field, Collection<String> fallback) {
        if (node == null || field == null) {
            return;
        }
        if (!node.has(field) || node.get(field).isNull() || !node.get(field).isArray()) {
            ArrayNode array = OBJECT_MAPPER.createArrayNode();
            if (fallback != null) {
                fallback.forEach(array::add);
            }
            node.set(field, array);
        }
    }

    public static void clampInt(ObjectNode node, String field, int min, int max, int fallback) {
        if (node == null || field == null) {
            return;
        }
        int value = fallback;
        if (node.has(field) && node.get(field).canConvertToInt()) {
            value = node.get(field).asInt();
        }
        node.put(field, Math.max(min, Math.min(max, value)));
    }

    public static String toJson(ObjectNode node) {
        try {
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "AI response JSON serialization failed.");
        }
    }

    private static String extractJson(String rawText, char open, char close) {
        if (rawText == null || rawText.isBlank()) {
            throw new AiServiceException("AI_RESPONSE_EMPTY", "AI response content is empty.");
        }

        String text = stripMarkdownFence(rawText.trim());
        int start = text.indexOf(open);
        int end = text.lastIndexOf(close);
        if (start < 0 || end < 0 || end <= start) {
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "AI response content is not valid JSON.");
        }
        return text.substring(start, end + 1);
    }

    private static String stripMarkdownFence(String text) {
        String value = text;
        if (value.startsWith("```json")) {
            value = value.substring(7).trim();
        } else if (value.startsWith("```JSON")) {
            value = value.substring(7).trim();
        } else if (value.startsWith("```")) {
            value = value.substring(3).trim();
        }
        if (value.endsWith("```")) {
            value = value.substring(0, value.length() - 3).trim();
        }
        return value;
    }
}
