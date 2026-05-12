package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_document")
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