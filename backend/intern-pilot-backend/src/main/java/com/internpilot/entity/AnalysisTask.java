package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_task")//ai分析任务表
@Schema(description = "AI分析任务实体类，包含AI分析任务的详细信息和状态")
public class AnalysisTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private Long userId;

    private Long resumeId;

    private Long resumeVersionId;

    private Long jobId;

    private Long reportId;

    private String status;

    private Integer progress;

    private String message;

    private Integer forceRefresh;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
