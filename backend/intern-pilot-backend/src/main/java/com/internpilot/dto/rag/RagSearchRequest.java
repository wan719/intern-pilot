package com.internpilot.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "RAG检索请求")
public class RagSearchRequest {

    @Schema(description = "检索文本")
    @NotBlank(message = "检索文本不能为空")
    @Size(max = 1000, message = "检索文本长度不能超过 1000 个字符")
    private String query;

    @Schema(description = "岗位方向")
    @Size(max = 100, message = "岗位方向长度不能超过 100 个字符")
    private String direction;

    @Schema(description = "知识类型")
    @Size(max = 50, message = "知识类型长度不能超过 50 个字符")
    private String knowledgeType;

    @Schema(description = "返回数量")
    @Min(value = 1, message = "返回数量不能少于 1")
    @Max(value = 20, message = "返回数量不能超过 20")
    private Integer topK = 5;
}
