package com.internpilot.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.RagKnowledgeChunk;
import com.internpilot.entity.RagKnowledgeDocument;
import com.internpilot.mapper.RagKnowledgeChunkMapper;
import com.internpilot.mapper.RagKnowledgeDocumentMapper;
import com.internpilot.service.EmbeddingClient;
import com.internpilot.util.TextChunkUtils;
import com.internpilot.util.VectorUtils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Schema(description = "RAG知识引导运行器，在应用启动时执行，负责从数据库中加载RAG知识文档，并将其内容切分成知识块，计算向量并存储到数据库中")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class RagKnowledgeBootstrapRunner implements ApplicationRunner {

    private final RagKnowledgeDocumentMapper documentMapper;
    private final RagKnowledgeChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;

    @Override
    @Schema(description = "运行RAG知识引导逻辑，在应用启动时执行")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
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

    @Schema(description = "构建知识块，将文档内容切分成多个知识块并计算向量")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
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

    @Schema(description = "统计指定文档的知识块数量")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    private long countChunks(Long documentId) {
        Long count = chunkMapper.selectCount(new LambdaQueryWrapper<RagKnowledgeChunk>()
                .eq(RagKnowledgeChunk::getDocumentId, documentId)
                .eq(RagKnowledgeChunk::getDeleted, 0));
        return count == null ? 0 : count;
    }
}
