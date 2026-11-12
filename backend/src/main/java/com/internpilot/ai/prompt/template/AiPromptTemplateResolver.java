package com.internpilot.ai.prompt.template;

import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.exception.AiServiceException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AiPromptTemplateResolver {

    private final Map<AiScenarioEnum, AiPromptTemplate> templates = new EnumMap<>(AiScenarioEnum.class);

    public AiPromptTemplateResolver(List<AiPromptTemplate> templateList) {
        for (AiPromptTemplate template : templateList) {
            templates.put(template.scenario(), template);
        }
    }

    public AiPromptTemplate resolve(AiScenarioEnum scenario) {
        AiPromptTemplate template = templates.get(scenario);
        if (template == null) {
            throw new AiServiceException("AI_PROMPT_TEMPLATE_NOT_FOUND",
                    "No AI prompt template configured for scenario " + scenario);
        }
        return template;
    }
}
