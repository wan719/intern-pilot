package com.internpilot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.exception.AiServiceException;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;
@Schema(description = "JSON工具类，提供了将AI生成的原始文本解析成JSON对象、将对象序列化成JSON字符串以及将JSON字符串反序列化成对象等功能，并包含针对AI响应内容的特殊处理逻辑")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static <T> T parseAiJson(String rawText, Class<T> clazz) {
        try {
            String json = extractJson(rawText);
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "AI response JSON parse failed.");
        }
    }

    public static String toJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("JSON serialization failed.");
        }
    }

    public static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("JSON deserialization failed.");
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
            throw new AiServiceException("JSON deserialization failed.");
        }
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new AiServiceException("AI_RESPONSE_EMPTY", "AI response content is empty.");
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
            throw new AiServiceException("AI_RESPONSE_PARSE_FAILED", "AI response content is not valid JSON.");
        }

        return text.substring(start, end + 1);
    }
}
