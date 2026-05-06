package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("application_record")
public class ApplicationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long jobId;

    private Long resumeId;

    private Long reportId;

    private String status;

    private LocalDate applyDate;

    private LocalDateTime interviewDate;

    private String note;

    private String review;

    private String priority;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
