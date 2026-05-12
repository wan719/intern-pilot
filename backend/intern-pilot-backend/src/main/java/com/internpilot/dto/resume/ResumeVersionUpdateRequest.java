package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改简历版本请求")
public class ResumeVersionUpdateRequest {

    @Schema(description = "版本名称")
    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    @Schema(description = "版本内容")
    @NotBlank(message = "版本内容不能为空")
    private String content;
}