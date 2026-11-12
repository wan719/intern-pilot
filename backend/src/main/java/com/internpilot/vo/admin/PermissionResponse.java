package com.internpilot.vo.admin;

import lombok.Data;

@Data
public class PermissionResponse {

    private Long permissionId;

    private String permissionCode;

    private String permissionName;

    private String resourceType;

    private String description;

    private Boolean enabled;
}