package com.internpilot.vo.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Boolean emailVerified;

    private List<String> roles;

    private List<String> permissions;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
