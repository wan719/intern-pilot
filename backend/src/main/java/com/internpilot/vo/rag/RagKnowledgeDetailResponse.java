package com.internpilot.vo.rag;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
@Data
@Schema(description = "RAG知识详情响应")
public class RagKnowledgeDetailResponse {
    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "岗位方向")
    private String direction;

    @Schema(description = "知识类型")
    private String knowledgeType;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "是否启用")
    private Integer enabled;
    
    @Schema(description = "知识块列表")
    private List<RagKnowledgeChunkResponse> chunks;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
