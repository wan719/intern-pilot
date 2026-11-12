package com.internpilot.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建RAG知识文档请求")
public class RagKnowledgeCreateRequest {

    @Schema(description = "标题", example = "Java后端实习岗位能力模型")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    @Schema(description = "岗位方向", example = "Java后端")
    @NotBlank(message = "岗位方向不能为空")
    @Size(max = 100, message = "岗位方向长度不能超过 100 个字符")
    private String direction;

    @Schema(description = "知识类型", example = "SKILL_REQUIREMENT")
    @NotBlank(message = "知识类型不能为空")
    @Size(max = 50, message = "知识类型长度不能超过 50 个字符")
    private String knowledgeType;

    @Schema(description = "知识内容")
    @NotBlank(message = "知识内容不能为空")
    @Size(max = 20000, message = "知识内容长度不能超过 20000 个字符")
    private String content;

    @Schema(description = "摘要")
    @Size(max = 1000, message = "摘要长度不能超过 1000 个字符")
    private String summary;
}
