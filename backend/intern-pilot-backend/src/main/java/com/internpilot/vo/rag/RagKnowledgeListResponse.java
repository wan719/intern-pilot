package com.internpilot.vo.rag;

import lombok.Data;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "RAG知识列表响应")
public class RagKnowledgeListResponse {
    @Schema(description = "文档ID")
    private Long documentId;
    
    @Schema(description = "标题")
    private String title;

    @Schema(description = "岗位方向")
    private String direction;

    @Schema(description = "知识类型")
    private String knowledgeType;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "块数量")
    private Integer chunkCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}