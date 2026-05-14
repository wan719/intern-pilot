package com.internpilot.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

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
            String promptVersion
    ) {
        String rawKey = userId + ":" +
                resumeId + ":" +
                (resumeVersionId == null ? 0 : resumeVersionId) + ":" +
                (resumeUpdatedAt == null ? "" : resumeUpdatedAt) + ":" +
                jobId + ":" +
                (jobUpdatedAt == null ? "" : jobUpdatedAt) + ":" +
                ragEnabled + ":" +
                promptVersion;

        String hash = DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
        return CACHE_KEY_PREFIX + hash;
    }
}