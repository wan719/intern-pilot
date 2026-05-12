package com.internpilot.service;

import com.internpilot.vo.rag.RagSearchResultResponse;

import java.util.List;

public interface RagRetrieveService {

    List<RagSearchResultResponse> retrieveForAnalysis(
            String resumeText,
            String jobDescription,
            Integer topK
    );

    String buildRagContext(List<RagSearchResultResponse> results);
}