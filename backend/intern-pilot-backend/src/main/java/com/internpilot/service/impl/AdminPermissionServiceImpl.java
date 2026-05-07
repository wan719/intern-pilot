package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.Permission;
import com.internpilot.entity.Role;
import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.RoleMapper;
import com.internpilot.mapper.RolePermissionMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.mapper.UserRoleMapper;
import com.internpilot.service.AdminPermissionService;
import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl implements AdminPermissionService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<RoleResponse> listRoles() {
        return roleMapper.selectList(
                        new LambdaQueryWrapper<Role>()
                                .eq(Role::getDeleted, 0)
                                .orderByAsc(Role::getId))
                .stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        return permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .eq(Permission::getDeleted, 0)
                                .orderByAsc(Permission::getResourceType)
                                .orderByAsc(Permission::getId))
                .stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    @Override
    @Transactional
    public Boolean updateUserRoles(Long userId, List<Long> roleIds) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        validateRoles(roleIds);

        userRoleMapper.disableByUserId(userId);
        for (Long roleId : roleIds) {
            userRoleMapper.upsertActive(userId, roleId);
        }
        return true;
    }

    @Override
    @Transactional
    public Boolean updateRolePermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleMapper.selectById(roleId);
        if (role == null || Integer.valueOf(1).equals(role.getDeleted())) {
            throw new BusinessException("角色不存在");
        }
        validatePermissions(permissionIds);

        rolePermissionMapper.disableByRoleId(roleId);
        for (Long permissionId : permissionIds) {
            rolePermissionMapper.upsertActive(roleId, permissionId);
        }
        return true;
    }

    private void validateRoles(List<Long> roleIds) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>()
                        .in(Role::getId, roleIds)
                        .eq(Role::getDeleted, 0)
                        .eq(Role::getEnabled, 1));
        if (count == null || count != roleIds.size()) {
            throw new BusinessException("角色不存在或已禁用");
        }
    }

    private void validatePermissions(List<Long> permissionIds) {
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permissionIds)
                        .eq(Permission::getDeleted, 0)
                        .eq(Permission::getEnabled, 1));
        if (count == null || count != permissionIds.size()) {
            throw new BusinessException("权限不存在或已禁用");
        }
    }

    private RoleResponse toRoleResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setRoleId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setEnabled(Integer.valueOf(1).equals(role.getEnabled()));
        return response;
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setPermissionId(permission.getId());
        response.setPermissionCode(permission.getPermissionCode());
        response.setPermissionName(permission.getPermissionName());
        response.setResourceType(permission.getResourceType());
        response.setDescription(permission.getDescription());
        response.setEnabled(Integer.valueOf(1).equals(permission.getEnabled()));
        return response;
    }
}
