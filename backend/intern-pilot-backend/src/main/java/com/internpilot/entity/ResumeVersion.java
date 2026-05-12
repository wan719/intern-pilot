package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_version")
public class ResumeVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private String versionName;

    private String versionType;

    private String content;

    private String contentSummary;

    private Long targetJobId;

    private Long sourceVersionId;

    private Long aiReportId;

    private String optimizePrompt;

    private String aiRawResponse;

    private Integer isCurrent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}