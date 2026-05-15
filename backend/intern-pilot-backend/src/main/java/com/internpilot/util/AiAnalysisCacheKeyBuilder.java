package com.internpilot.util;

import org.springframework.util.DigestUtils;

import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.StandardCharsets;
@Schema(description = "AI分析结果缓存键构建器，根据用户ID、简历ID、职位ID等信息构建唯一的缓存键，用于存储和检索AI分析结果")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class AiAnalysisCacheKeyBuilder {

    private static final String CACHE_KEY_PREFIX = "ai:analysis:result:";

    private AiAnalysisCacheKeyBuilder() {
    }

    public static String build(
            Long userId,
            Long resumeId,
            Long resumeVersionId,
            String resumeUpdatedAt,
            Long jobId,
            String jobUpdatedAt,
            boolean ragEnabled,
            String promptVersion,
            String model
    ) {
        String rawKey = userId + ":" +
                resumeId + ":" +
                (resumeVersionId == null ? 0 : resumeVersionId) + ":" +
                (resumeUpdatedAt == null ? "" : resumeUpdatedAt) + ":" +
                jobId + ":" +
                (jobUpdatedAt == null ? "" : jobUpdatedAt) + ":" +
                ragEnabled + ":" +
                promptVersion + ":" +
                (model == null ? "" : model);

        String hash = DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
        return CACHE_KEY_PREFIX + hash;
    }
}