package com.internpilot.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserListResponse {

    private Long userId;

    private String username;

    private String email;

    private String school;

    private String major;

    private String grade;

    private Integer enabled;

    private List<String> roles;

    private LocalDateTime createdAt;
}   