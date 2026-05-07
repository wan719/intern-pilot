package com.internpilot.vo.admin;

import lombok.Data;

@Data
public class RoleResponse {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private String description;

    private Boolean enabled;
}