package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.rag.RagKnowledgeCreateRequest;
import com.internpilot.dto.rag.RagKnowledgeUpdateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
import com.internpilot.entity.RagKnowledgeChunk;
import com.internpilot.entity.RagKnowledgeDocument;
import com.internpilot.enums.RagKnowledgeTypeEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.RagKnowledgeChunkMapper;
import com.internpilot.mapper.RagKnowledgeDocumentMapper;
import com.internpilot.service.EmbeddingClient;
import com.internpilot.service.RagKnowledgeService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.util.TextChunkUtils;
import com.internpilot.util.VectorUtils;
import com.internpilot.vo.rag.RagKnowledgeChunkResponse;
import com.internpilot.vo.rag.RagKnowledgeDetailResponse;
import com.internpilot.vo.rag.RagKnowledgeListResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagKnowledgeServiceImpl implements RagKnowledgeService {

    private final RagKnowledgeDocumentMapper documentMapper;
    private final RagKnowledgeChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;

    @Override
    @Transactional
    public Long create(RagKnowledgeCreateRequest request) {
        validateKnowledgeType(request.getKnowledgeType());

        RagKnowledgeDocument document = new RagKnowledgeDocument();
        document.setTitle(request.getTitle().trim());
        document.setDirection(request.getDirection().trim());
        document.setKnowledgeType(request.getKnowledgeType().trim());
        document.setContent(request.getContent());
        document.setSummary(buildSummary(request.getSummary(), request.getContent()));
        document.setEnabled(1);
        document.setCreatedBy(SecurityUtils.getCurrentUserId());
        documentMapper.insert(document);

        rebuildChunks(document);
        return document.getId();
    }

    @Override
    @Transactional
    public Boolean update(Long documentId, RagKnowledgeUpdateRequest request) {
        validateKnowledgeType(request.getKnowledgeType());
        RagKnowledgeDocument document = getDocumentOrThrow(documentId);
        document.setTitle(request.getTitle().trim());
        document.setDirection(request.getDirection().trim());
        document.setKnowledgeType(request.getKnowledgeType().trim());
        document.setContent(request.getContent());
        document.setSummary(buildSummary(request.getSummary(), request.getContent()));
        document.setEnabled(request.getEnabled() == null ? 1 : normalizeEnabled(request.getEnabled()));
        documentMapper.updateById(document);
        rebuildChunks(document);
        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long documentId) {
        RagKnowledgeDocument document = getDocumentOrThrow(documentId);
        chunkMapper.delete(new LambdaQueryWrapper<RagKnowledgeChunk>()
                .eq(RagKnowledgeChunk::getDocumentId, document.getId()));
        documentMapper.deleteById(document.getId());
        return true;
    }

    @Override
    @Transactional
    public Boolean rebuildChunks(Long documentId) {
        rebuildChunks(getDocumentOrThrow(documentId));
        return true;
    }

    @Override
    public PageResult<RagKnowledgeListResponse> list(
            String direction,
            String knowledgeType,
            Integer enabled,
            Integer pageNum,
            Integer pageSize
    ) {
        LambdaQueryWrapper<RagKnowledgeDocument> wrapper = new LambdaQueryWrapper<RagKnowledgeDocument>()
                .eq(RagKnowledgeDocument::getDeleted, 0);
        if (StringUtils.hasText(direction)) {
            wrapper.like(RagKnowledgeDocument::getDirection, direction.trim());
        }
        if (StringUtils.hasText(knowledgeType)) {
            wrapper.eq(RagKnowledgeDocument::getKnowledgeType, knowledgeType.trim());
        }
        if (enabled != null) {
            wrapper.eq(RagKnowledgeDocument::getEnabled, normalizeEnabled(enabled));
        }
        wrapper.orderByDesc(RagKnowledgeDocument::getUpdatedAt).orderByDesc(RagKnowledgeDocument::getId);

        Page<RagKnowledgeDocument> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<RagKnowledgeDocument> resultPage = documentMapper.selectPage(page, wrapper);
        List<RagKnowledgeListResponse> records = resultPage.getRecords().stream()
                .map(this::toListResponse)
                .toList();
        return new PageResult<>(records, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize(), resultPage.getPages());
    }

    @Override
    public RagKnowledgeDetailResponse getDetail(Long documentId) {
        RagKnowledgeDocument document = getDocumentOrThrow(documentId);
        List<RagKnowledgeChunkResponse> chunks = chunkMapper.selectList(new LambdaQueryWrapper<RagKnowledgeChunk>()
                        .eq(RagKnowledgeChunk::getDocumentId, document.getId())
                        .eq(RagKnowledgeChunk::getDeleted, 0)
                        .orderByAsc(RagKnowledgeChunk::getChunkIndex))
                .stream()
                .map(this::toChunkResponse)
                .toList();
        RagKnowledgeDetailResponse response = new RagKnowledgeDetailResponse();
        response.setDocumentId(document.getId());
        response.setTitle(document.getTitle());
        response.setDirection(document.getDirection());
        response.setKnowledgeType(document.getKnowledgeType());
        response.setContent(document.getContent());
        response.setSummary(document.getSummary());
        response.setEnabled(document.getEnabled());
        response.setChunks(chunks);
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }

    @Override
    public List<RagSearchResultResponse> search(RagSearchRequest request) {
        List<Double> queryVector = embeddingClient.embed(request.getQuery());
        int topK = normalizeTopK(request.getTopK());
        LambdaQueryWrapper<RagKnowledgeChunk> wrapper = new LambdaQueryWrapper<RagKnowledgeChunk>()
                .eq(RagKnowledgeChunk::getEnabled, 1)
                .eq(RagKnowledgeChunk::getDeleted, 0);
        if (StringUtils.hasText(request.getDirection())) {
            wrapper.like(RagKnowledgeChunk::getDirection, request.getDirection().trim());
        }
        if (StringUtils.hasText(request.getKnowledgeType())) {
            wrapper.eq(RagKnowledgeChunk::getKnowledgeType, request.getKnowledgeType().trim());
        }

        return chunkMapper.selectList(wrapper).stream()
                .map(chunk -> toSearchResult(chunk, queryVector))
                .sorted(Comparator.comparing(RagSearchResultResponse::getSimilarity).reversed())
                .limit(topK)
                .toList();
    }

    private void rebuildChunks(RagKnowledgeDocument document) {
        chunkMapper.delete(new LambdaQueryWrapper<RagKnowledgeChunk>()
                .eq(RagKnowledgeChunk::getDocumentId, document.getId()));

        List<String> chunks = TextChunkUtils.splitToChunks(document.getContent());
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            RagKnowledgeChunk chunk = new RagKnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setDirection(document.getDirection());
            chunk.setKnowledgeType(document.getKnowledgeType());
            chunk.setChunkIndex(i + 1);
            chunk.setContent(content);
            chunk.setEmbedding(VectorUtils.toJson(embeddingClient.embed(content)));
            chunk.setEmbeddingModel(embeddingClient.getModel());
            chunk.setEnabled(document.getEnabled());
            chunkMapper.insert(chunk);
        }
    }

    private RagSearchResultResponse toSearchResult(RagKnowledgeChunk chunk, List<Double> queryVector) {
        RagKnowledgeDocument document = documentMapper.selectById(chunk.getDocumentId());
        RagSearchResultResponse response = new RagSearchResultResponse();
        response.setChunkId(chunk.getId());
        response.setDocumentId(chunk.getDocumentId());
        response.setTitle(document == null ? "" : document.getTitle());
        response.setDirection(chunk.getDirection());
        response.setKnowledgeType(chunk.getKnowledgeType());
        response.setContent(chunk.getContent());
        response.setSimilarity(VectorUtils.cosineSimilarity(queryVector, VectorUtils.fromJson(chunk.getEmbedding())));
        return response;
    }

    private RagKnowledgeListResponse toListResponse(RagKnowledgeDocument document) {
        RagKnowledgeListResponse response = new RagKnowledgeListResponse();
        response.setDocumentId(document.getId());
        response.setTitle(document.getTitle());
        response.setDirection(document.getDirection());
        response.setKnowledgeType(document.getKnowledgeType());
        response.setSummary(document.getSummary());
        response.setEnabled(document.getEnabled());
        response.setChunkCount(countChunks(document.getId()));
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }

    private RagKnowledgeChunkResponse toChunkResponse(RagKnowledgeChunk chunk) {
        RagKnowledgeChunkResponse response = new RagKnowledgeChunkResponse();
        response.setChunkId(chunk.getId());
        response.setDocumentId(chunk.getDocumentId());
        response.setChunkIndex(chunk.getChunkIndex());
        response.setContent(chunk.getContent());
        response.setEmbeddingModel(chunk.getEmbeddingModel());
        response.setEnabled(chunk.getEnabled());
        return response;
    }

    private int countChunks(Long documentId) {
        Long count = chunkMapper.selectCount(new LambdaQueryWrapper<RagKnowledgeChunk>()
                .eq(RagKnowledgeChunk::getDocumentId, documentId)
                .eq(RagKnowledgeChunk::getDeleted, 0));
        return count == null ? 0 : count.intValue();
    }

    private RagKnowledgeDocument getDocumentOrThrow(Long documentId) {
        RagKnowledgeDocument document = documentMapper.selectOne(new LambdaQueryWrapper<RagKnowledgeDocument>()
                .eq(RagKnowledgeDocument::getId, documentId)
                .eq(RagKnowledgeDocument::getDeleted, 0)
                .last("LIMIT 1"));
        if (document == null) {
            throw new BusinessException("RAG知识文档不存在");
        }
        return document;
    }

    private String buildSummary(String summary, String content) {
        if (StringUtils.hasText(summary)) {
            return summary.trim();
        }
        String compact = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return compact.length() <= 160 ? compact : compact.substring(0, 160) + "...";
    }

    private void validateKnowledgeType(String knowledgeType) {
        if (!RagKnowledgeTypeEnum.isValid(knowledgeType)) {
            throw new BusinessException("RAG知识类型不合法");
        }
    }

    private int normalizeEnabled(Integer enabled) {
        return Integer.valueOf(0).equals(enabled) ? 0 : 1;
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK < 1) {
            return 5;
        }
        return Math.min(topK, 20);
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100);
    }
}
