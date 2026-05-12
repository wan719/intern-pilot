package com.internpilot.service;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embed(String text);

    String getModel();
}