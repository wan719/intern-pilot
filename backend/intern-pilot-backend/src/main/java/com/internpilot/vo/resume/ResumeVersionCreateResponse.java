package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建简历版本响应")
public class ResumeVersionCreateResponse {
    @Schema(description = "版本ID")
    private Long versionId;
    @Schema(description = "简历ID")
    private Long resumeId;
    @Schema(description = "版本名称")
    private String versionName;
    @Schema(description = "版本类型")
    private String versionType;
    
    @Schema(description = "是否为当前版本")
    private Integer isCurrent;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}