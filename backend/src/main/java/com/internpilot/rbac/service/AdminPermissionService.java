package com.internpilot.rbac.service;

import com.internpilot.rbac.vo.PermissionResponse;
import com.internpilot.rbac.vo.RoleResponse;

import java.util.List;

public interface AdminPermissionService {

    List<RoleResponse> listRoles();

    List<PermissionResponse> listPermissions(String resourceType);

    Boolean updateUserRoles(Long userId, List<Long> roleIds);

    Boolean updateRolePermissions(Long roleId, List<Long> permissionIds);
}
