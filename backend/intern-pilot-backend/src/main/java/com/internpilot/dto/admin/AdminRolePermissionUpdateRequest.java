package com.internpilot.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminRolePermissionUpdateRequest {

    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}