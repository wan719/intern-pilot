package com.internpilot.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.RagKnowledgeChunk;
import com.internpilot.entity.RagKnowledgeDocument;
import com.internpilot.mapper.RagKnowledgeChunkMapper;
import com.internpilot.mapper.RagKnowledgeDocumentMapper;
import com.internpilot.service.EmbeddingClient;
import com.internpilot.util.TextChunkUtils;
import com.internpilot.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RagKnowledgeBootstrapRunner implements ApplicationRunner {

    private final RagKnowledgeDocumentMapper documentMapper;
    private final RagKnowledgeChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;

    @Override
    public void run(ApplicationArguments args) {
        List<RagKnowledgeDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<RagKnowledgeDocument>()
                .eq(RagKnowledgeDocument::getEnabled, 1)
                .eq(RagKnowledgeDocument::getDeleted, 0));
        for (RagKnowledgeDocument document : documents) {
            if (countChunks(document.getId()) == 0) {
                buildChunks(document);
            }
        }
    }

    private void buildChunks(RagKnowledgeDocument document) {
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

    private long countChunks(Long documentId) {
        Long count = chunkMapper.selectCount(new LambdaQueryWrapper<RagKnowledgeChunk>()
                .eq(RagKnowledgeChunk::getDocumentId, documentId)
                .eq(RagKnowledgeChunk::getDeleted, 0));
        return count == null ? 0 : count;
    }
}
