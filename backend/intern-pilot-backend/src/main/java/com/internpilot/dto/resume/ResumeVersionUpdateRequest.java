package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改简历版本请求")
public class ResumeVersionUpdateRequest {

    @Schema(description = "版本名称")
    @NotBlank(message = "版本名称不能为空")
    @Size(max = 100, message = "版本名称长度不能超过 100 个字符")
    private String versionName;

    @Schema(description = "版本内容")
    @NotBlank(message = "版本内容不能为空")
    @Size(max = 50000, message = "版本内容长度不能超过 50000 个字符")
    private String content;
}
