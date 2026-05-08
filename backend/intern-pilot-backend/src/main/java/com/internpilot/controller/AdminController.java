package com.internpilot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.common.Result;
import com.internpilot.dto.admin.RolePermissionUpdateRequest;
import com.internpilot.dto.admin.UserRoleUpdateRequest;
import com.internpilot.entity.User;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.service.AdminPermissionService;
import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;
import com.internpilot.vo.auth.AuthUserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员 操作接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminPermissionService adminPermissionService;
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    @Operation(summary = "",description = "")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("admin ok");
    }

    @PreAuthorize("hasAuthority('admin:user:read')")
    @GetMapping("/users")
    public Result<List<AuthUserResponse>> listUsers() {
        List<AuthUserResponse> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .orderByDesc(User::getCreatedAt))
                .stream()
                .map(this::toAuthUserResponse)
                .toList();
        return Result.success(users);
    }

    @PreAuthorize("hasAuthority('admin:role:read')")
    @GetMapping("/roles")
    public Result<List<RoleResponse>> listRoles() {
        return Result.success(adminPermissionService.listRoles());
    }

    @PreAuthorize("hasAuthority('admin:permission:read')")
    @GetMapping("/permissions")
    public Result<List<PermissionResponse>> listPermissions() {
        return Result.success(adminPermissionService.listPermissions());
    }

    @PreAuthorize("hasAuthority('admin:user:write')")
    @PutMapping("/users/{id}/roles")
    public Result<Boolean> updateUserRoles(
            @PathVariable Long id,
            @RequestBody @Valid UserRoleUpdateRequest request) {
        return Result.success(adminPermissionService.updateUserRoles(id, request.getRoleIds()));
    }

    @PreAuthorize("hasAuthority('admin:role:write')")
    @PutMapping("/roles/{id}/permissions")
    public Result<Boolean> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody @Valid RolePermissionUpdateRequest request) {
        return Result.success(adminPermissionService.updateRolePermissions(id, request.getPermissionIds()));
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setSchool(user.getSchool());
        response.setMajor(user.getMajor());
        response.setGrade(user.getGrade());
        response.setRole(user.getRole());
        response.setRoles(permissionMapper.selectRoleCodesByUserId(user.getId()));
        response.setPermissions(permissionMapper.selectPermissionCodesByUserId(user.getId()));
        return response;
    }
}
