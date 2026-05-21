package com.internpilot.ai.prompt.template;

import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.scenario.AiScenarioEnum;
import org.springframework.stereotype.Component;

@Component
public class RagQaPromptTemplateV1 implements AiPromptTemplate {

    @Override
    public AiScenarioEnum scenario() {
        return AiScenarioEnum.RAG_QA;
    }

    @Override
    public String version() {
        return "RAG_QA_v1";
    }

    @Override
    public String systemPrompt() {
        return "You answer only from the supplied RAG context. Return only valid JSON in Simplified Chinese.";
    }

    @Override
    public String buildUserPrompt(AiPromptContext context) {
        return """
                Answer the user question using only the knowledge context.
                If the context is insufficient, say it cannot be confirmed from the knowledge base.
                Return JSON: {"answer":"","evidence":[],"confidence":"HIGH|MEDIUM|LOW","suggestions":[]}

                Knowledge context:
                %s

                User question:
                %s
                """.formatted(nullToEmpty(context.getRagContext()), nullToEmpty(context.getUserQuestion()));
    }

    @Override
    public AiOutputFormat outputFormat() {
        return AiOutputFormat.JSON_OBJECT;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
