package com.internpilot.service.impl;

import com.internpilot.service.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MockEmbeddingClient implements EmbeddingClient {

    @Override
    public List<Double> embed(String text) {
        List<Double> vector = new ArrayList<>();

        int dimension = 64;
        int hash = text == null ? 0 : text.hashCode();

        for (int i = 0; i < dimension; i++) {
            double value = ((hash >> (i % 16)) & 1) == 1 ? 1.0 : 0.0;
            vector.add(value);
        }

        return vector;
    }

    @Override
    public String getModel() {
        return "mock-embedding-64";
    }
}