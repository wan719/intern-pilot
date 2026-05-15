package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_document")//RAG知识文档表，存储RAG检索到的知识文档，
// 每个文档包含标题、内容、摘要等信息
@Schema(description = "RAG知识文档实体类，包含RAG知识文档的详细信息和关联的知识块")
public class RagKnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String direction;

    private String knowledgeType;

    private String content;

    private String summary;

    private Integer enabled;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}