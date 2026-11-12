package com.internpilot.ai.cache;

import com.internpilot.ai.scenario.AiScenarioEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AiCacheKeyContext {

    private AiScenarioEnum scenario;
    private String model;
    private String promptVersion;
    private Long userId;
    private Long resumeId;
    private Long resumeVersionId;
    private LocalDateTime resumeUpdatedAt;
    private String resumeUpdatedAtText;
    private Long jobId;
    private LocalDateTime jobUpdatedAt;
    private String jobUpdatedAtText;
    private String promptHash;
    private Map<String, Object> extra;
}
