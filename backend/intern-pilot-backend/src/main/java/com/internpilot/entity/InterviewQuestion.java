package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_question")//面试问题表
@Schema(description = "面试问题实体类，包含面试问题的详细信息")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private Long userId;

    private String questionType;

    private String difficulty;

    private String question;

    private String answer;

    private String answerPoints;

    private String relatedSkills;

    private String followUps;

    private String keywords;

    private String source;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}