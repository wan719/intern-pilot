package com.internpilot.vo.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "RAG知识块响应")
public class RagKnowledgeChunkResponse {
    @Schema(description = "文本块ID")
    private Long chunkId;

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文本块索引")
    private Integer chunkIndex;
    @Schema(description = "内容")
    private String content;
    @Schema(description = "嵌入向量")
    private String embeddingModel;
    @Schema(description = "启用状态")
    private Integer enabled;
}
