package com.internpilot.ai.router;

import com.internpilot.ai.scenario.AiScenarioEnum;

public interface AiModelRouter {

    String route(AiScenarioEnum scenario);

    String fallback(AiScenarioEnum scenario);

    boolean allowFallback(AiScenarioEnum scenario);
}
