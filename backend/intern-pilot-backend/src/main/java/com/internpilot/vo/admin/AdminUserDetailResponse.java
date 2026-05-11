package com.internpilot.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserDetailResponse {

    private Long userId;

    private String username;

    private String email;

    private String school;

    private String major;

    private String grade;

    private Integer enabled;

    private List<String> roles;

    private List<String> permissions;

    private Integer resumeCount;

    private Integer jobCount;

    private Integer analysisReportCount;

    private Integer applicationCount;

    private Integer interviewQuestionReportCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}