package com.internpilot.service.admin.impl;

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
import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPermissionServiceImplTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private AdminPermissionServiceImpl service;

    @Test
    void listRolesAndPermissionsShouldMapResponses() {
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role(1L, "ADMIN")));
        when(permissionMapper.selectPermissionCodesByRoleId(1L)).thenReturn(List.of("user:read", "user:update"));
        when(permissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(permission(10L, "user:read")));

        List<RoleResponse> roles = service.listRoles();
        List<PermissionResponse> permissions = service.listPermissions("USER");

        assertEquals(1, roles.size());
        assertEquals(List.of("user:read", "user:update"), roles.get(0).getPermissions());
        assertEquals("user:read", permissions.get(0).getPermissionCode());
        assertTrue(permissions.get(0).getEnabled());
    }

    @Test
    void updateUserRolesShouldValidateAndUpsertRoles() {
        when(userMapper.selectById(7L)).thenReturn(user());
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        assertTrue(service.updateUserRoles(7L, List.of(1L, 2L)));

        verify(userRoleMapper).disableByUserId(7L);
        verify(userRoleMapper).upsertActive(7L, 1L);
        verify(userRoleMapper).upsertActive(7L, 2L);
    }

    @Test
    void updateUserRolesShouldRejectMissingUserOrDisabledRole() {
        when(userMapper.selectById(7L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.updateUserRoles(7L, List.of(1L)));

        User deleted = user();
        deleted.setDeleted(1);
        when(userMapper.selectById(8L)).thenReturn(deleted);
        assertThrows(BusinessException.class, () -> service.updateUserRoles(8L, List.of(1L)));

        when(userMapper.selectById(9L)).thenReturn(user());
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertThrows(BusinessException.class, () -> service.updateUserRoles(9L, List.of(1L)));
    }

    @Test
    void updateRolePermissionsShouldValidateAndUpsertPermissions() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "ADMIN"));
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        assertTrue(service.updateRolePermissions(1L, List.of(10L, 11L)));

        verify(rolePermissionMapper).disableByRoleId(1L);
        verify(rolePermissionMapper).upsertActive(1L, 10L);
        verify(rolePermissionMapper).upsertActive(1L, 11L);
    }

    @Test
    void updateRolePermissionsShouldRejectMissingRoleOrInvalidPermission() {
        when(roleMapper.selectById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.updateRolePermissions(1L, List.of(10L)));

        Role deleted = role(2L, "USER");
        deleted.setDeleted(1);
        when(roleMapper.selectById(2L)).thenReturn(deleted);
        assertThrows(BusinessException.class, () -> service.updateRolePermissions(2L, List.of(10L)));

        when(roleMapper.selectById(3L)).thenReturn(role(3L, "USER"));
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertThrows(BusinessException.class, () -> service.updateRolePermissions(3L, List.of(10L)));
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("student");
        user.setDeleted(0);
        return user;
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(code + "角色");
        role.setDescription("desc");
        role.setEnabled(1);
        role.setDeleted(0);
        return role;
    }

    private Permission permission(Long id, String code) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setPermissionName("用户读取");
        permission.setResourceType("USER");
        permission.setDescription("desc");
        permission.setEnabled(1);
        permission.setDeleted(0);
        return permission;
    }
}
