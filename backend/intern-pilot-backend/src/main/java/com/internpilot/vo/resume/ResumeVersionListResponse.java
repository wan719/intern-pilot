package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历版本列表响应")
public class ResumeVersionListResponse {
    @Schema(description = "版本ID")
    private Long versionId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "版本名称")
    private String versionName;

    @Schema(description = "版本类型")
    private String versionType;

    @Schema(description = "内容摘要")
    private String contentSummary;
    
    @Schema(description = "目标岗位ID")
    private Long targetJobId;

    @Schema(description = "目标公司名称")
    private String targetCompanyName;

    @Schema(description = "目标岗位标题")
    private String targetJobTitle;

    @Schema(description = "是否为当前版本")
    private Integer isCurrent;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}