package com.internpilot.ai.router;

import com.internpilot.ai.config.AiModelProperties;
import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAiModelRouter implements AiModelRouter {

    private final AiModelProperties modelProperties;
    private final AiProperties aiProperties;

    @Override
    public String route(AiScenarioEnum scenario) {
        AiScenarioEnum safeScenario = scenario == null ? AiScenarioEnum.UNKNOWN : scenario;
        String model = modelProperties.getScenarioModel().get(safeScenario.name());
        if (hasText(model)) {
            return model;
        }
        if (hasText(modelProperties.getDefaultModel())) {
            return modelProperties.getDefaultModel();
        }
        return hasText(aiProperties.getModel()) ? aiProperties.getModel() : "deepseek-v4-flash";
    }

    @Override
    public String fallback(AiScenarioEnum scenario) {
        if (hasText(modelProperties.getFallbackModel())) {
            return modelProperties.getFallbackModel();
        }
        return route(AiScenarioEnum.UNKNOWN);
    }

    @Override
    public boolean allowFallback(AiScenarioEnum scenario) {
        return scenario != AiScenarioEnum.UNKNOWN && hasText(fallback(scenario));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
