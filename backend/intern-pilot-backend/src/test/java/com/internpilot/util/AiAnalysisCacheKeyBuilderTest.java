package com.internpilot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAnalysisCacheKeyBuilderTest {

    @Test
    void buildShouldReturnSameKeyForSameInput() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );

        assertEquals(key1, key2);
    }

    @Test
    void buildShouldReturnDifferentKeyWhenResumeUpdatedAtChanges() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T11:00:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );

        assertNotEquals(key1, key2);
    }

    @Test
    void buildShouldReturnDifferentKeyWhenJobUpdatedAtChanges() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T11:00:00", true, "v1", "deepseek-v4-flash"
        );

        assertNotEquals(key1, key2);
    }

    @Test
    void buildShouldReturnDifferentKeyWhenResumeIdChanges() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 11L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );

        assertNotEquals(key1, key2);
    }

    @Test
    void buildShouldReturnDifferentKeyWhenJobIdChanges() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                9L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );

        assertNotEquals(key1, key2);
    }

    @Test
    void buildShouldReturnDifferentKeyWhenPromptVersionChanges() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v2", "deepseek-v4-flash"
        );

        assertNotEquals(key1, key2);
    }

    @Test
    void buildShouldReturnDifferentKeyWhenRagEnabledChanges() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", false, "v1", "deepseek-v4-flash"
        );

        assertNotEquals(key1, key2);
    }

    @Test
    void buildShouldReturnDifferentKeyWhenModelChanges() {
        String key1 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );
        String key2 = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-pro"
        );

        assertNotEquals(key1, key2);
    }

    @Test
    void buildShouldStartWithCacheKeyPrefix() {
        String key = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );

        assertTrue(key.startsWith("ai:analysis:result:"));
    }

    @Test
    void buildShouldHandleNullResumeVersionId() {
        String key = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, null, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", "deepseek-v4-flash"
        );

        assertNotNull(key);
        assertTrue(key.startsWith("ai:analysis:result:"));
    }

    @Test
    void buildShouldHandleNullTimestamps() {
        String key = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, null,
                8L, null, true, "v1", "deepseek-v4-flash"
        );

        assertNotNull(key);
        assertTrue(key.startsWith("ai:analysis:result:"));
    }

    @Test
    void buildShouldHandleNullModel() {
        String key = AiAnalysisCacheKeyBuilder.build(
                1L, 10L, 3L, "2026-05-14T10:30:00",
                8L, "2026-05-14T10:40:00", true, "v1", null
        );

        assertNotNull(key);
        assertTrue(key.startsWith("ai:analysis:result:"));
    }

    private void assertNotNull(String key) {
        if (key == null) {
            throw new AssertionError("Expected non-null key");
        }
    }
}
