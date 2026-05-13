package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.rag.RagKnowledgeCreateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
import com.internpilot.entity.RagKnowledgeChunk;
import com.internpilot.entity.RagKnowledgeDocument;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.RagKnowledgeChunkMapper;
import com.internpilot.mapper.RagKnowledgeDocumentMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.service.EmbeddingClient;
import com.internpilot.vo.rag.RagSearchResultResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagKnowledgeServiceTest {

    @Mock
    private RagKnowledgeDocumentMapper documentMapper;

    @Mock
    private RagKnowledgeChunkMapper chunkMapper;

    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private RagKnowledgeServiceImpl ragKnowledgeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldPersistDocumentAndBuildChunks() {
        mockLoginUser(7L);
        when(embeddingClient.getModel()).thenReturn("mock-embedding");
        when(embeddingClient.embed(anyString())).thenReturn(List.of(0.8, 0.2));
        doAnswer(invocation -> {
            RagKnowledgeDocument document = invocation.getArgument(0);
            document.setId(99L);
            return 1;
        }).when(documentMapper).insert(any(RagKnowledgeDocument.class));

        Long documentId = ragKnowledgeService.create(buildCreateRequest());

        assertThat(documentId).isEqualTo(99L);
        ArgumentCaptor<RagKnowledgeDocument> documentCaptor = ArgumentCaptor.forClass(RagKnowledgeDocument.class);
        verify(documentMapper).insert(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getCreatedBy()).isEqualTo(7L);
        assertThat(documentCaptor.getValue().getSummary()).contains("Java后端");

        ArgumentCaptor<RagKnowledgeChunk> chunkCaptor = ArgumentCaptor.forClass(RagKnowledgeChunk.class);
        verify(chunkMapper).insert(chunkCaptor.capture());
        RagKnowledgeChunk chunk = chunkCaptor.getValue();
        assertThat(chunk.getDocumentId()).isEqualTo(99L);
        assertThat(chunk.getEmbedding()).contains("0.8");
        assertThat(chunk.getEmbeddingModel()).isEqualTo("mock-embedding");
        assertThat(chunk.getEnabled()).isEqualTo(1);
    }

    @Test
    void createShouldRejectInvalidKnowledgeType() {
        RagKnowledgeCreateRequest request = buildCreateRequest();
        request.setKnowledgeType("UNKNOWN");

        assertThatThrownBy(() -> ragKnowledgeService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RAG知识类型不合法");
    }

    @Test
    void searchShouldReturnTopKSortedBySimilarity() {
        when(embeddingClient.embed("Java后端")).thenReturn(List.of(1.0, 0.0));

        RagKnowledgeChunk javaChunk = buildChunk(1L, 10L, "Java后端能力", "[1.0,0.0]");
        RagKnowledgeChunk aiChunk = buildChunk(2L, 20L, "AI应用能力", "[0.0,1.0]");
        when(chunkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(aiChunk, javaChunk));

        when(documentMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long documentId = invocation.getArgument(0);
            RagKnowledgeDocument document = new RagKnowledgeDocument();
            document.setTitle(documentId.equals(10L) ? "Java后端实习岗位能力模型" : "AI应用岗位知识");
            return document;
        });

        RagSearchRequest request = new RagSearchRequest();
        request.setQuery("Java后端");
        request.setTopK(1);

        List<RagSearchResultResponse> results = ragKnowledgeService.search(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChunkId()).isEqualTo(1L);
        assertThat(results.get(0).getTitle()).isEqualTo("Java后端实习岗位能力模型");
        assertThat(results.get(0).getSimilarity()).isGreaterThan(0.99);
    }

    private RagKnowledgeCreateRequest buildCreateRequest() {
        RagKnowledgeCreateRequest request = new RagKnowledgeCreateRequest();
        request.setTitle("Java后端实习岗位能力模型");
        request.setDirection("Java后端");
        request.setKnowledgeType("SKILL_REQUIREMENT");
        request.setContent("Java后端岗位需要掌握 Java、Spring Boot、MySQL、Redis 和接口设计。");
        return request;
    }

    private RagKnowledgeChunk buildChunk(Long id, Long documentId, String content, String embedding) {
        RagKnowledgeChunk chunk = new RagKnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setDirection("Java后端");
        chunk.setKnowledgeType("SKILL_REQUIREMENT");
        chunk.setContent(content);
        chunk.setEmbedding(embedding);
        chunk.setEnabled(1);
        return chunk;
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "tester", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }
}
