package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_chunk")//RAG知识块表，存储RAG检索到的知识块，每个知识块包含内容、
// 向量等信息
@Schema(description = "RAG知识块实体类，包含RAG知识块的详细信息和关联的文档")
public class RagKnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String direction;

    private String knowledgeType;

    private Integer chunkIndex;

    private String content;

    private String embedding;

    private String embeddingModel;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}