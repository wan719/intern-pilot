package com.internpilot.service;

import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;

import java.util.List;

public interface AdminPermissionService {

    List<RoleResponse> listRoles();

    List<PermissionResponse> listPermissions();

    Boolean updateUserRoles(Long userId, List<Long> roleIds);

    Boolean updateRolePermissions(Long roleId, List<Long> permissionIds);
}