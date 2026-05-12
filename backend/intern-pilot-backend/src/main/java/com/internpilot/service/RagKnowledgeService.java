package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.rag.RagKnowledgeCreateRequest;
import com.internpilot.dto.rag.RagKnowledgeUpdateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
import com.internpilot.vo.rag.RagKnowledgeDetailResponse;
import com.internpilot.vo.rag.RagKnowledgeListResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;

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