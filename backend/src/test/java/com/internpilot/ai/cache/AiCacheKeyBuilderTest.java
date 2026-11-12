package com.internpilot.ai.cache;

import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.constant.RedisKeyConstants;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCacheKeyBuilderTest {

    private final AiCacheKeyBuilder builder = new AiCacheKeyBuilder();

    @Test
    void buildShouldIncludeScenarioModelPromptVersionAndPromptHash() {
        AiCacheKeyContext base = AiCacheKeyContext.builder()
                .scenario(AiScenarioEnum.RESUME_JOB_ANALYSIS)
                .model("deepseek-v4-pro")
                .promptVersion("RESUME_JOB_ANALYSIS_v2")
                .userId(1L)
                .resumeId(2L)
                .resumeVersionId(3L)
                .jobId(4L)
                .promptHash("hash-a")
                .extra(Map.of("ragEnabled", true))
                .build();

        String key = builder.build(base);
        String changedPrompt = builder.build(AiCacheKeyContext.builder()
                .scenario(AiScenarioEnum.RESUME_JOB_ANALYSIS)
                .model("deepseek-v4-pro")
                .promptVersion("RESUME_JOB_ANALYSIS_v2")
                .userId(1L)
                .resumeId(2L)
                .resumeVersionId(3L)
                .jobId(4L)
                .promptHash("hash-b")
                .extra(Map.of("ragEnabled", true))
                .build());
        String changedModel = builder.build(AiCacheKeyContext.builder()
                .scenario(AiScenarioEnum.RESUME_JOB_ANALYSIS)
                .model("deepseek-v4-flash")
                .promptVersion("RESUME_JOB_ANALYSIS_v2")
                .userId(1L)
                .resumeId(2L)
                .resumeVersionId(3L)
                .jobId(4L)
                .promptHash("hash-a")
                .extra(Map.of("ragEnabled", true))
                .build());

        assertTrue(key.startsWith(RedisKeyConstants.AI_CACHE_PREFIX + "RESUME_JOB_ANALYSIS:"));
        assertNotEquals(key, changedPrompt);
        assertNotEquals(key, changedModel);
    }
}
