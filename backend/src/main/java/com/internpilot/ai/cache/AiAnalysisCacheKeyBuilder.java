package com.internpilot.ai.cache;

import org.springframework.util.DigestUtils;

import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.constant.RedisKeyConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.StandardCharsets;
@Schema(description = "AI分析结果缓存键构建器，根据用户ID、简历ID、职位ID等信息构建唯一的缓存键，用于存储和检索AI分析结果")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class AiAnalysisCacheKeyBuilder {

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
        return build(userId, resumeId, resumeVersionId, resumeUpdatedAt, jobId, jobUpdatedAt,
                ragEnabled, promptVersion, model, "");
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
            String model,
            String promptHash
    ) {
        return build(AiScenarioEnum.RESUME_JOB_ANALYSIS, userId, resumeId, resumeVersionId, resumeUpdatedAt,
                jobId, jobUpdatedAt, ragEnabled, promptVersion, model, promptHash);
    }

    public static String build(
            AiScenarioEnum scenario,
            Long userId,
            Long resumeId,
            Long resumeVersionId,
            String resumeUpdatedAt,
            Long jobId,
            String jobUpdatedAt,
            boolean ragEnabled,
            String promptVersion,
            String model,
            String promptHash
    ) {
        String safeScenario = scenario == null ? AiScenarioEnum.UNKNOWN.name() : scenario.name();
        String rawKey = safeScenario + ":" +
                userId + ":" +
                resumeId + ":" +
                (resumeVersionId == null ? 0 : resumeVersionId) + ":" +
                (resumeUpdatedAt == null ? "" : resumeUpdatedAt) + ":" +
                jobId + ":" +
                (jobUpdatedAt == null ? "" : jobUpdatedAt) + ":" +
                ragEnabled + ":" +
                promptVersion + ":" +
                (model == null ? "" : model) + ":" +
                (promptHash == null ? "" : promptHash);

        String hash = DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
        return RedisKeyConstants.AI_ANALYSIS_RESULT_PREFIX + hash;
    }
}
