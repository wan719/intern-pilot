package com.internpilot.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
@Schema(description = "向量工具类，提供了计算余弦相似度、将向量序列化成JSON字符串以及将JSON字符串反序列化成向量等功能，用于在职位推荐和匹配过程中处理向量数据")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class VectorUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        int size = Math.min(a.size(), b.size());

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < size; i++) {
            double x = a.get(i);
            double y = b.get(i);

            dot += x * y;
            normA += x * x;
            normB += y * y;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static String toJson(List<Double> vector) {
        try {
            return OBJECT_MAPPER.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("向量序列化失败", e);
        }
    }

    public static List<Double> fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}