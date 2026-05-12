package com.internpilot.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建RAG知识文档请求")
public class RagKnowledgeCreateRequest {

    @Schema(description = "标题", example = "Java后端实习岗位能力模型")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "岗位方向", example = "Java后端")
    @NotBlank(message = "岗位方向不能为空")
    private String direction;

    @Schema(description = "知识类型", example = "SKILL_REQUIREMENT")
    @NotBlank(message = "知识类型不能为空")
    private String knowledgeType;

    @Schema(description = "知识内容")
    @NotBlank(message = "知识内容不能为空")
    private String content;

    @Schema(description = "摘要")
    private String summary;
}
