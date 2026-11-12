package com.internpilot.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "修改投递备注请求")
public class ApplicationNoteUpdateRequest {

    @Schema(description = "备注", example = "已完成一面，主要问了 Spring Security 和 Redis。")
    private String note;

    @Schema(description = "面试或笔试复盘", example = "Spring Security 过滤链回答不够清楚，需要复习。")
    private String review;

    @Schema(description = "面试时间", example = "2026-05-10T14:00:00")
    private LocalDateTime interviewDate;
}
