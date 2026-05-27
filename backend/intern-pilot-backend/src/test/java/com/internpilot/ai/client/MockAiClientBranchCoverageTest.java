package com.internpilot.ai.client;

import com.internpilot.ai.scenario.AiScenarioEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockAiClientBranchCoverageTest {

    private final MockAiClient aiClient = new MockAiClient();

    @Test
    void chatRequestShouldHandleNullExplicitScenarioAndPromptDetection() {
        assertThat(aiClient.chat((AiChatRequest) null)).contains("Mock AI");

        assertThat(aiClient.chat(AiChatRequest.builder()
                .scenario(AiScenarioEnum.INTERVIEW_QUESTION_REGENERATION)
                .build())).contains("questions");

        assertThat(aiClient.chat(AiChatRequest.builder()
                .userPrompt("rag knowledge base")
                .build())).contains("Spring Boot");

        assertThat(aiClient.chat(AiChatRequest.builder()
                .userPrompt("match analysis")
                .build())).contains("matchScore");
    }

    @Test
    void stringPromptDetectionShouldCoverRemainingBranches() {
        assertThat(aiClient.chat("job recommendation")).contains("recommendations");
        assertThat(aiClient.chat("optimized resume")).contains("InternPilot");
        assertThat(aiClient.chat("questionType followup")).contains("questionType");
        assertThat(aiClient.chat("json JD")).contains("matchScore");
        assertThat(aiClient.chat("knowledge base")).contains("Spring Boot");
    }

    @Test
    void mockEmbeddingShouldReturnStableBinaryVectorForNullAndText() {
        MockEmbeddingClient embeddingClient = new MockEmbeddingClient();

        List<Double> nullVector = embeddingClient.embed(null);
        List<Double> textVector = embeddingClient.embed("InternPilot");

        assertThat(nullVector).hasSize(64).containsOnly(0.0);
        assertThat(textVector).hasSize(64).allMatch(value -> value == 0.0 || value == 1.0);
        assertThat(embeddingClient.getModel()).isEqualTo("mock-embedding-64");
    }

    @Test
    void aiScenarioFromCodeShouldBeCaseInsensitiveAndFallbackToUnknown() {
        assertThat(AiScenarioEnum.fromCode(null)).isEqualTo(AiScenarioEnum.UNKNOWN);
        assertThat(AiScenarioEnum.fromCode(" ")).isEqualTo(AiScenarioEnum.UNKNOWN);
        assertThat(AiScenarioEnum.fromCode("resume_job_analysis")).isEqualTo(AiScenarioEnum.RESUME_JOB_ANALYSIS);
        assertThat(AiScenarioEnum.fromCode("RAG_QA")).isEqualTo(AiScenarioEnum.RAG_QA);
        assertThat(AiScenarioEnum.fromCode("missing")).isEqualTo(AiScenarioEnum.UNKNOWN);
    }
}
