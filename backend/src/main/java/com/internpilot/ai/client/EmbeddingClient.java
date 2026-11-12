package com.internpilot.ai.client;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embed(String text);

    String getModel();
}