package com.internpilot.rag.service;

import com.internpilot.common.PageResult;
import com.internpilot.rag.dto.RagKnowledgeCreateRequest;
import com.internpilot.rag.dto.RagKnowledgeUpdateRequest;
import com.internpilot.rag.dto.RagSearchRequest;
import com.internpilot.rag.vo.RagKnowledgeDetailResponse;
import com.internpilot.rag.vo.RagKnowledgeListResponse;
import com.internpilot.rag.vo.RagSearchResultResponse;

import java.util.List;

public interface RagKnowledgeService {

    Long create(RagKnowledgeCreateRequest request);

    Boolean update(Long documentId, RagKnowledgeUpdateRequest request);

    Boolean delete(Long documentId);

    Boolean rebuildChunks(Long documentId);

    PageResult<RagKnowledgeListResponse> list(
            String direction,
            String knowledgeType,
            Integer enabled,
            Integer pageNum,
            Integer pageSize
    );

    RagKnowledgeDetailResponse getDetail(Long documentId);

    List<RagSearchResultResponse> search(RagSearchRequest request);
}