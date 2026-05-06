package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_report")
public class AnalysisReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

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
