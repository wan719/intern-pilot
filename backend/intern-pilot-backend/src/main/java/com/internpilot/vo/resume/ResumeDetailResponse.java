package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历详情响应")
public class ResumeDetailResponse {

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "存储文件名")
    private String storedFileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "是否默认简历")
    private Boolean isDefault;

    @Schema(description = "解析后的完整文本")
    private String parsedText;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
