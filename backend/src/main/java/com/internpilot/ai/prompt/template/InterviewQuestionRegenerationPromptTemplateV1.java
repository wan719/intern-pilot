package com.internpilot.ai.prompt.template;

import com.internpilot.ai.scenario.AiScenarioEnum;
import org.springframework.stereotype.Component;

@Component
public class InterviewQuestionRegenerationPromptTemplateV1 extends InterviewQuestionPromptTemplateV1 {

    @Override
    public AiScenarioEnum scenario() {
        return AiScenarioEnum.INTERVIEW_QUESTION_REGENERATION;
    }

    @Override
    public String version() {
        return "INTERVIEW_QUESTION_REGENERATION_v1";
    }
}
