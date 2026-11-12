package com.internpilot.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.ai.client.EmbeddingClient;
import com.internpilot.entity.RagKnowledgeChunk;
import com.internpilot.entity.RagKnowledgeDocument;
import com.internpilot.mapper.RagKnowledgeChunkMapper;
import com.internpilot.mapper.RagKnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagKnowledgeBootstrapRunnerTest {

    @Mock
    private RagKnowledgeDocumentMapper documentMapper;

    @Mock
    private RagKnowledgeChunkMapper chunkMapper;

    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private RagKnowledgeBootstrapRunner runner;

    @Test
    void runShouldBuildChunksForEnabledDocumentsWithoutExistingChunks() {
        RagKnowledgeDocument document = document("a".repeat(120));
        when(documentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(document));
        when(chunkMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(embeddingClient.embed(any(String.class))).thenReturn(List.of(0.1, 0.9));
        when(embeddingClient.getModel()).thenReturn("mock-embedding");

        runner.run(null);

        ArgumentCaptor<RagKnowledgeChunk> captor = ArgumentCaptor.forClass(RagKnowledgeChunk.class);
        verify(chunkMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getDocumentId());
        assertEquals(1, captor.getValue().getChunkIndex());
        assertTrue(captor.getValue().getEmbedding().contains("0.9"));
        assertEquals("mock-embedding", captor.getValue().getEmbeddingModel());
    }

    @Test
    void runShouldSkipDocumentsWithExistingChunksAndTreatNullCountAsZero() {
        RagKnowledgeDocument first = document("first ".repeat(30));
        RagKnowledgeDocument second = document("second ".repeat(30));
        second.setId(2L);
        when(documentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));
        when(chunkMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L).thenReturn(null);
        when(embeddingClient.embed(any(String.class))).thenReturn(List.of(1.0));
        when(embeddingClient.getModel()).thenReturn("mock-embedding");

        runner.run(null);

        verify(chunkMapper).insert(any(RagKnowledgeChunk.class));
    }

    @Test
    void runShouldDoNothingWhenNoDocuments() {
        when(documentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        runner.run(null);

        verify(chunkMapper, never()).insert(any(RagKnowledgeChunk.class));
    }

    private RagKnowledgeDocument document(String content) {
        RagKnowledgeDocument document = new RagKnowledgeDocument();
        document.setId(1L);
        document.setDirection("Java");
        document.setKnowledgeType("SKILL");
        document.setContent(content);
        document.setEnabled(1);
        document.setDeleted(0);
        return document;
    }
}
