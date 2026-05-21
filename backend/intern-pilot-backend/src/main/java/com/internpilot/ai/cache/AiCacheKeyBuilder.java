package com.internpilot.ai.cache;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class AiCacheKeyBuilder {

    private static final String CACHE_KEY_PREFIX = "ai:cache:";

    public String build(AiCacheKeyContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        String scenario = context.getScenario() == null ? "UNKNOWN" : context.getScenario().name();
        String rawKey = String.join(":",
                scenario,
                safe(context.getModel()),
                safe(context.getPromptVersion()),
                safe(context.getUserId()),
                safe(context.getResumeId()),
                safe(context.getResumeVersionId()),
                firstText(context.getResumeUpdatedAtText(), context.getResumeUpdatedAt()),
                safe(context.getJobId()),
                firstText(context.getJobUpdatedAtText(), context.getJobUpdatedAt()),
                safe(context.getPromptHash()),
                normalizeExtra(context.getExtra()));
        String hash = DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
        return CACHE_KEY_PREFIX + scenario + ":" + hash;
    }

    private String normalizeExtra(Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return "";
        }
        return new TreeMap<>(extra).toString();
    }

    private String firstText(String value, Object fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return safe(fallback);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
