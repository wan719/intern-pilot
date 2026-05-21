package com.internpilot.ai.client;

public interface AiClient {

    String chat(String prompt);

    default String chat(AiChatRequest request) {
        return chat(request == null ? null : request.getUserPrompt());
    }
}
