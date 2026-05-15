package com.internpilot.controller;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.admin.AdminRolePermissionUpdateRequest;
import com.internpilot.dto.admin.AdminUserRoleUpdateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.AdminPermissionService;
import com.internpilot.service.AdminUserService;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import com.internpilot.vo.admin.AdminUserDetailResponse;
import com.internpilot.vo.admin.AdminUserListResponse;
import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员操作接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "连通性检查")
    @PreAuthorize("hasAuthority('admin:dashboard')")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("admin ok");
    }

    @Operation(summary = "查询用户列表")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping("/users")
    public Result<PageResult<AdminUserListResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(adminUserService.list(keyword, roleCode, enabled, pageNum, pageSize));
    }

    @Operation(summary = "查询用户详情")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping("/users/{id}")
    public Result<AdminUserDetailResponse> getUserDetail(@PathVariable Long id) {
        return Result.success(adminUserService.getDetail(id));
    }

    @Operation(summary = "禁用用户")
    @PreAuthorize("hasAuthority('user:update')")
    @OperationLog(module = "管理员", operation = "禁用用户", type = OperationTypeEnum.DISABLE)
    @PutMapping("/users/{id}/disable")
    public Result<Boolean> disableUser(@PathVariable Long id) {
        return Result.success(adminUserService.disable(id));
    }

    @Operation(summary = "启用用户")
    @PreAuthorize("hasAuthority('user:update')")
    @OperationLog(module = "管理员", operation = "启用用户", type = OperationTypeEnum.ENABLE)
    @PutMapping("/users/{id}/enable")
    public Result<Boolean> enableUser(@PathVariable Long id) {
        return Result.success(adminUserService.enable(id));
    }

    @Operation(summary = "修改用户角色")
    @PreAuthorize("hasAuthority('user:update')")
    @OperationLog(module = "管理员", operation = "修改用户角色", type = OperationTypeEnum.GRANT)
    @PutMapping("/users/{id}/roles")
    public Result<Boolean> updateUserRoles(
            @PathVariable Long id,
            @RequestBody @Valid AdminUserRoleUpdateRequest request
    ) {
        return Result.success(adminUserService.updateRoles(id, request));
    }

    @Operation(summary = "查询角色列表")
    @PreAuthorize("hasAuthority('role:read')")
    @GetMapping("/roles")
    public Result<List<RoleResponse>> listRoles() {
        return Result.success(adminPermissionService.listRoles());
    }

    @Operation(summary = "查询权限列表")
    @PreAuthorize("hasAuthority('permission:read')")
    @GetMapping("/permissions")
    public Result<List<PermissionResponse>> listPermissions(
            @RequestParam(required = false) String resourceType
    ) {
        return Result.success(adminPermissionService.listPermissions(resourceType));
    }

    @Operation(summary = "修改角色权限")
    @PreAuthorize("hasAuthority('role:update')")
    @OperationLog(module = "管理员", operation = "修改角色权限", type = OperationTypeEnum.GRANT)
    @PutMapping("/roles/{id}/permissions")
    public Result<Boolean> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody @Valid AdminRolePermissionUpdateRequest request
    ) {
        return Result.success(adminPermissionService.updateRolePermissions(id, request.getPermissionIds()));
    }

    @Operation(summary = "管理员看板汇总")
    @PreAuthorize("hasAuthority('admin:dashboard')")
    @GetMapping("/dashboard/summary")
    public Result<AdminDashboardSummaryResponse> dashboardSummary() {
        return Result.success(adminUserService.dashboardSummary());
    }
}
