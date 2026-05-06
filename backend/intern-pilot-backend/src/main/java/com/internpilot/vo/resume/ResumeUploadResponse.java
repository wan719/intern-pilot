package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历上传响应")
public class ResumeUploadResponse {

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "解析文本预览")
    private String parsedTextPreview;

    @Schema(description = "是否默认简历")
    private Boolean isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
