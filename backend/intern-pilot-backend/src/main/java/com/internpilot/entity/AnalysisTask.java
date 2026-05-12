package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_task")
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
