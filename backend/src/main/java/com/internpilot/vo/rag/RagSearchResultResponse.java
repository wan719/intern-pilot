package com.internpilot.vo.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 搜索结果响应类
 * 用于封装RAG检索后的结果信息
 */
@Data
@Schema(description = "RAG检索结果响应")
public class RagSearchResultResponse {
  // 使用Lombok的@Data注解，自动生成getter、setter、toString等方法
    @Schema(description = "文本块ID")
    private Long chunkId;     // 文本块ID，用于标识检索结果中的具体文本片段
    @Schema(description = "文档ID")
    private Long documentId;  // 文档ID，用于标识文本所属的文档
    
    @Schema(description = "标题")
    private String title;

    @Schema(description = "方向")
    private String direction; // 方向信息，可能用于表示文本的方向或关联性

    @Schema(description = "知识类型")
    private String knowledgeType; // 知识类型，表示文本所属的知识分类

    @Schema(description = "内容")
    private String content;   // 文本内容，检索到的具体文本信息

    @Schema(description = "相似度")
    private Double similarity; // 相似度分数，表示检索结果与查询的匹配程度
}