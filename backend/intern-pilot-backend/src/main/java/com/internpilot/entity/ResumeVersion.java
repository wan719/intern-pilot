package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_version")//简历版本表，用户上传简历后会生成一个版本，
// 用户可以基于某个版本进行优化，每个版本包含优化后的内容、摘要、关联的目标职位等信息
@Schema(description = "简历版本实体类，包含简历版本的详细信息和关联的用户、简历、目标职位等")
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