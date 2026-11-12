package com.internpilot.rag.service;

import com.internpilot.rag.vo.RagSearchResultResponse;

import java.util.List;

public interface RagRetrieveService {

    List<RagSearchResultResponse> retrieveForAnalysis(
            String resumeText,
            String jobDescription,
            Integer topK
    );

    String buildRagContext(List<RagSearchResultResponse> results);
}