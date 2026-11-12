package com.internpilot.rbac.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionUpdateRequest {

    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}