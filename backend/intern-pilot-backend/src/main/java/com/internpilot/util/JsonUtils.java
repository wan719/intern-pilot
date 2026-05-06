package com.internpilot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.exception.AiServiceException;

import java.util.Collections;
import java.util.List;

public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static <T> T parseAiJson(String rawText, Class<T> clazz) {
        try {
            String json = extractJson(rawText);
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI 返回结果解析失败");
        }
    }

    public static String toJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("JSON 序列化失败");
        }
    }

    public static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("JSON 反序列化失败");
        }
    }

    public static List<String> toStringList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            JavaType type = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class);
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("JSON 反序列化失败");
        }
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new AiServiceException("AI 返回内容为空");
        }

        String text = rawText.trim();

        if (text.startsWith("```json")) {
            text = text.substring(7).trim();
        }
        if (text.startsWith("```")) {
            text = text.substring(3).trim();
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            throw new AiServiceException("AI 返回内容不是合法 JSON");
        }

        return text.substring(start, end + 1);
    }
}
