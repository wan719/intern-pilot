package com.internpilot.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "RAG检索请求")
public class RagSearchRequest {

    @Schema(description = "检索文本")
    @NotBlank(message = "检索文本不能为空")
    private String query;

    @Schema(description = "岗位方向")
    private String direction;

    @Schema(description = "知识类型")
    private String knowledgeType;

    @Schema(description = "返回数量")
    private Integer topK = 5;
}