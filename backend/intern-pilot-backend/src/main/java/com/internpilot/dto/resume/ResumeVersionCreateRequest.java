package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建简历版本请求")
public class ResumeVersionCreateRequest {

    @Schema(description = "版本名称", example = "Java后端实习优化版")
    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    @Schema(description = "版本类型", example = "MANUAL")
    private String versionType;

    @Schema(description = "版本内容")
    @NotBlank(message = "版本内容不能为空")
    private String content;

    @Schema(description = "目标岗位ID")
    private Long targetJobId;

    @Schema(description = "来源版本ID")
    private Long sourceVersionId;
}