package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_report")//ai分析报告表
@Schema(description = "AI分析报告实体类，包含AI对简历和职位的分析结果")
public class AnalysisReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long resumeVersionId;

    private Long jobId;

    private Integer matchScore;

    private String matchLevel;

    private String strengths;

    private String weaknesses;

    private String missingSkills;

    private String suggestions;

    private String interviewTips;

    private String rawAiResponse;

    private String aiProvider;

    private String aiModel;

    private Integer cacheHit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
