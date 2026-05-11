package com.internpilot.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminRoleResponse {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer enabled;

    private List<String> permissions;
}