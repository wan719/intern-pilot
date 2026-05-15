package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_question_report")//面试问题报告表，报告包含多个面试问题
@Schema(description = "面试问题报告实体类，包含面试问题报告的详细信息和关联的面试问题")
public class InterviewQuestionReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long resumeVersionId;

    private Long jobId;

    private Long analysisReportId;

    private String title;

    private Integer questionCount;

    private String aiProvider;

    private String aiModel;

    private String rawAiResponse;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
