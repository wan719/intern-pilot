# InternPilot 管理员后台设计与实现文档

## 1. 文档说明

本文档用于描述 InternPilot 项目中管理员后台模块的设计与实现方案，包括模块背景、功能目标、权限设计、后端接口设计、前端页面设计、用户管理、角色管理、权限管理、操作日志管理、管理员数据看板、测试流程和面试讲解准备。

管理员后台是 InternPilot 从普通用户系统升级为完整管理平台的重要模块。

它依赖前面已经完成或计划完成的能力：

```text
JWT 用户认证
  ↓
RBAC 权限系统
  ↓
系统操作日志
  ↓
管理员后台
```

---

# 2. 为什么要做管理员后台

当前 InternPilot 已经支持普通用户完成完整求职流程：

```text
上传简历
  ↓
创建岗位 JD
  ↓
AI 匹配分析
  ↓
AI 面试题生成
  ↓
创建投递记录
  ↓
跟踪投递状态
```

但是一个完整系统通常还需要后台管理能力，例如：

```text
查看系统用户
禁用异常用户
管理用户角色
查看系统操作日志
查看 AI 使用情况
查看系统运行数据
```

管理员后台可以让项目从“个人工具型项目”升级为“平台型项目”。

---

# 3. 管理员后台目标

管理员后台第一阶段需要完成以下目标：

1. 管理员可以查看用户列表；
2. 管理员可以查看用户详情；
3. 管理员可以禁用用户；
4. 管理员可以启用用户；
5. 管理员可以查看角色列表；
6. 管理员可以查看权限列表；
7. 管理员可以给用户分配角色；
8. 管理员可以给角色分配权限；
9. 管理员可以查看系统操作日志；
10. 管理员可以查看日志详情；
11. 管理员可以查看后台数据看板；
12. 普通用户不能访问管理员接口；
13. 前端普通用户看不到管理员菜单；
14. 所有后台操作通过 RBAC 权限控制。

---

# 4. 管理员后台功能范围

## 4.1 第一阶段必须实现

第一阶段建议实现：

```text
用户管理
角色管理
权限查看
角色权限分配
用户角色分配
系统操作日志查看
管理员数据看板
```

---

## 4.2 第二阶段可扩展

后续可以扩展：

```text
AI 调用日志管理
Prompt 模板管理
系统配置管理
文件管理
数据导出
异常告警
用户行为分析
管理员操作审批
```

---

# 5. 管理员后台菜单设计

前端菜单建议：

```text
管理员后台
  ├── 后台看板
  ├── 用户管理
  ├── 角色管理
  ├── 权限管理
  └── 操作日志
```

对应路由：

```text
/admin/dashboard
/admin/users
/admin/roles
/admin/permissions
/admin/operation-logs
```

---

# 6. 权限设计

管理员后台必须依赖 RBAC 权限。

## 6.1 后台权限清单

```text
dashboard:admin:read

admin:user:read
admin:user:write
admin:user:disable

admin:role:read
admin:role:write

admin:permission:read

system:log:read
system:log:delete
```

---

## 6.2 权限说明

| 权限                    | 说明             |
| --------------------- | -------------- |
| dashboard:admin:read  | 查看管理员后台看板      |
| admin:user:read       | 查看用户列表和详情      |
| admin:user:write      | 修改用户基础信息或角色    |
| admin:user:disable    | 启用 / 禁用用户      |
| admin:role:read       | 查看角色           |
| admin:role:write      | 新增 / 修改角色，分配权限 |
| admin:permission:read | 查看权限列表         |
| system:log:read       | 查看系统操作日志       |
| system:log:delete     | 删除系统操作日志       |

---

## 6.3 初始化 SQL

如果前面 RBAC 已经初始化过部分权限，只需要补充缺失项。

```sql
INSERT INTO permission (permission_code, permission_name, resource_type, description, enabled)
VALUES
('dashboard:admin:read', '查看管理员后台看板', 'ADMIN_DASHBOARD', '查看管理员后台统计数据', 1),
('admin:user:read', '查看用户管理', 'ADMIN_USER', '查看用户列表和详情', 1),
('admin:user:write', '编辑用户管理', 'ADMIN_USER', '修改用户角色等信息', 1),
('admin:user:disable', '启用禁用用户', 'ADMIN_USER', '禁用或启用用户账号', 1),
('admin:role:read', '查看角色管理', 'ADMIN_ROLE', '查看角色列表和详情', 1),
('admin:role:write', '编辑角色管理', 'ADMIN_ROLE', '修改角色和角色权限', 1),
('admin:permission:read', '查看权限管理', 'ADMIN_PERMISSION', '查看权限列表', 1),
('system:log:read', '查看系统日志', 'SYSTEM_LOG', '查看系统操作日志', 1),
('system:log:delete', '删除系统日志', 'SYSTEM_LOG', '删除系统操作日志', 1);
```

---

## 6.4 给 ADMIN 分配全部后台权限

```sql
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN (
      'dashboard:admin:read',
      'admin:user:read',
      'admin:user:write',
      'admin:user:disable',
      'admin:role:read',
      'admin:role:write',
      'admin:permission:read',
      'system:log:read',
      'system:log:delete'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
        AND rp.deleted = 0
  );
```

---

# 7. 数据库依赖

管理员后台主要复用已有表：

```text
user
role
permission
user_role
role_permission
system_operation_log
resume
job_description
analysis_report
application_record
interview_question_report
```

第一阶段不需要新增后台专用表。

---

# 8. 用户管理模块设计

## 8.1 功能目标

用户管理模块支持：

1. 查看用户列表；
2. 根据用户名搜索；
3. 根据角色筛选；
4. 根据启用状态筛选；
5. 查看用户详情；
6. 禁用用户；
7. 启用用户；
8. 分配用户角色；
9. 查看用户业务数据统计。

---

## 8.2 用户列表接口

### 基本信息

| 项目     | 内容               |
| ------ | ---------------- |
| URL    | /api/admin/users |
| Method | GET              |
| 权限     | admin:user:read  |

### 查询参数

| 参数       | 类型      | 说明          |
| -------- | ------- | ----------- |
| keyword  | String  | 用户名 / 邮箱关键词 |
| roleCode | String  | 角色编码        |
| enabled  | Integer | 是否启用        |
| pageNum  | Integer | 页码          |
| pageSize | Integer | 每页数量        |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "userId": 1,
        "username": "wan",
        "email": "wan@example.com",
        "school": "西南大学",
        "major": "软件工程",
        "grade": "大二",
        "enabled": 1,
        "roles": ["USER"],
        "createdAt": "2026-05-07T20:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

---

## 8.3 用户详情接口

```text
GET /api/admin/users/{id}
```

权限：

```java
@PreAuthorize("hasAuthority('admin:user:read')")
```

详情建议返回：

```text
用户基础信息
角色列表
权限列表
简历数量
岗位数量
AI 分析报告数量
投递记录数量
面试题报告数量
最近登录 / 最近操作
```

---

## 8.4 禁用用户接口

```text
PUT /api/admin/users/{id}/disable
```

权限：

```java
@PreAuthorize("hasAuthority('admin:user:disable')")
```

业务规则：

1. 只能管理员操作；
2. 不允许禁用自己；
3. 不允许禁用最后一个管理员；
4. 禁用后用户无法登录；
5. 已登录 Token 后续是否立即失效，第一阶段可以不做，后续可结合 Redis Token 黑名单。

---

## 8.5 启用用户接口

```text
PUT /api/admin/users/{id}/enable
```

权限：

```java
@PreAuthorize("hasAuthority('admin:user:disable')")
```

---

## 8.6 修改用户角色接口

```text
PUT /api/admin/users/{id}/roles
```

权限：

```java
@PreAuthorize("hasAuthority('admin:user:write')")
```

请求：

```json
{
  "roleIds": [1, 2]
}
```

业务规则：

1. userId 必须存在；
2. roleIds 必须存在；
3. 先逻辑删除旧 user_role；
4. 再插入新 user_role；
5. 修改后用户重新登录才能看到新权限；
6. 后续如果做权限缓存，需要清除用户权限缓存。

---

# 9. 用户管理 DTO / VO 设计

## 9.1 AdminUserListResponse

路径：

```text
src/main/java/com/internpilot/vo/admin/AdminUserListResponse.java
```

```java
package com.internpilot.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserListResponse {

    private Long userId;

    private String username;

    private String email;

    private String school;

    private String major;

    private String grade;

    private Integer enabled;

    private List<String> roles;

    private LocalDateTime createdAt;
}
```

---

## 9.2 AdminUserDetailResponse

路径：

```text
src/main/java/com/internpilot/vo/admin/AdminUserDetailResponse.java
```

```java
package com.internpilot.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserDetailResponse {

    private Long userId;

    private String username;

    private String email;

    private String school;

    private String major;

    private String grade;

    private Integer enabled;

    private List<String> roles;

    private List<String> permissions;

    private Integer resumeCount;

    private Integer jobCount;

    private Integer analysisReportCount;

    private Integer applicationCount;

    private Integer interviewQuestionReportCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

---

## 9.3 AdminUserRoleUpdateRequest

路径：

```text
src/main/java/com/internpilot/dto/admin/AdminUserRoleUpdateRequest.java
```

```java
package com.internpilot.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminUserRoleUpdateRequest {

    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
```

---

# 10. 角色管理模块设计

## 10.1 功能目标

角色管理模块支持：

1. 查看角色列表；
2. 查看角色详情；
3. 新增角色；
4. 修改角色；
5. 禁用角色；
6. 启用角色；
7. 给角色分配权限。

第一阶段可以先实现：

```text
角色列表
角色详情
角色权限分配
```

新增和删除角色可以后续再做。

---

## 10.2 角色列表接口

```text
GET /api/admin/roles
```

权限：

```java
@PreAuthorize("hasAuthority('admin:role:read')")
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "roleId": 1,
      "roleCode": "USER",
      "roleName": "普通用户",
      "description": "普通注册用户",
      "enabled": 1,
      "permissions": [
        "resume:read",
        "resume:write"
      ]
    }
  ]
}
```

---

## 10.3 角色详情接口

```text
GET /api/admin/roles/{id}
```

返回：

```text
角色基础信息
角色拥有的权限
拥有该角色的用户数量
```

---

## 10.4 修改角色权限接口

```text
PUT /api/admin/roles/{id}/permissions
```

权限：

```java
@PreAuthorize("hasAuthority('admin:role:write')")
```

请求：

```json
{
  "permissionIds": [1, 2, 3, 4]
}
```

业务规则：

1. roleId 必须存在；
2. permissionIds 必须存在；
3. 不建议移除 ADMIN 的关键权限；
4. 先逻辑删除旧 role_permission；
5. 再插入新 role_permission；
6. 后续若有权限缓存，需要清除相关用户缓存。

---

# 11. 角色 DTO / VO 设计

## 11.1 AdminRoleResponse

路径：

```text
src/main/java/com/internpilot/vo/admin/AdminRoleResponse.java
```

```java
package com.internpilot.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminRoleResponse {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer enabled;

    private List<String> permissions;
}
```

---

## 11.2 AdminRolePermissionUpdateRequest

路径：

```text
src/main/java/com/internpilot/dto/admin/AdminRolePermissionUpdateRequest.java
```

```java
package com.internpilot.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminRolePermissionUpdateRequest {

    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}
```

---

# 12. 权限管理模块设计

## 12.1 功能目标

权限管理模块第一阶段只做查看，不做新增删除。

原因：

```text
权限通常由开发者在代码中定义
不建议管理员随意创建不存在的权限
否则前端和后端注解无法对应
```

---

## 12.2 权限列表接口

```text
GET /api/admin/permissions
```

权限：

```java
@PreAuthorize("hasAuthority('admin:permission:read')")
```

支持按 `resourceType` 分组展示。

---

## 12.3 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "permissionId": 1,
      "permissionCode": "resume:read",
      "permissionName": "查看简历",
      "resourceType": "RESUME",
      "description": "查看自己的简历",
      "enabled": 1
    }
  ]
}
```

---

# 13. 后台看板模块设计

## 13.1 功能目标

管理员后台看板用于展示系统整体数据。

建议展示：

```text
用户总数
今日新增用户
简历总数
岗位总数
AI 分析次数
AI 面试题报告数
投递记录数
今日操作日志数
失败操作数
```

---

## 13.2 接口设计

```text
GET /api/admin/dashboard/summary
```

权限：

```java
@PreAuthorize("hasAuthority('dashboard:admin:read')")
```

---

## 13.3 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userCount": 120,
    "todayNewUserCount": 5,
    "resumeCount": 300,
    "jobCount": 260,
    "analysisReportCount": 180,
    "interviewQuestionReportCount": 70,
    "applicationCount": 210,
    "todayOperationLogCount": 500,
    "failedOperationCount": 12
  }
}
```

---

## 13.4 AdminDashboardSummaryResponse

路径：

```text
src/main/java/com/internpilot/vo/admin/AdminDashboardSummaryResponse.java
```

```java
package com.internpilot.vo.admin;

import lombok.Data;

@Data
public class AdminDashboardSummaryResponse {

    private Long userCount;

    private Long todayNewUserCount;

    private Long resumeCount;

    private Long jobCount;

    private Long analysisReportCount;

    private Long interviewQuestionReportCount;

    private Long applicationCount;

    private Long todayOperationLogCount;

    private Long failedOperationCount;
}
```

---

# 14. AdminUserService 设计

## 14.1 Service 接口

路径：

```text
src/main/java/com/internpilot/service/AdminUserService.java
```

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.admin.AdminUserRoleUpdateRequest;
import com.internpilot.vo.admin.AdminUserDetailResponse;
import com.internpilot.vo.admin.AdminUserListResponse;

public interface AdminUserService {

    PageResult<AdminUserListResponse> list(
            String keyword,
            String roleCode,
            Integer enabled,
            Integer pageNum,
            Integer pageSize
    );

    AdminUserDetailResponse getDetail(Long userId);

    Boolean disable(Long userId);

    Boolean enable(Long userId);

    Boolean updateRoles(Long userId, AdminUserRoleUpdateRequest request);
}
```

---

## 14.2 AdminUserController

路径：

```text
src/main/java/com/internpilot/controller/admin/AdminUserController.java
```

```java
package com.internpilot.controller.admin;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.admin.AdminUserRoleUpdateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.AdminUserService;
import com.internpilot.vo.admin.AdminUserDetailResponse;
import com.internpilot.vo.admin.AdminUserListResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "查询用户列表")
    @PreAuthorize("hasAuthority('admin:user:read')")
    @GetMapping
    public Result<PageResult<AdminUserListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(adminUserService.list(keyword, roleCode, enabled, pageNum, pageSize));
    }

    @Operation(summary = "查询用户详情")
    @PreAuthorize("hasAuthority('admin:user:read')")
    @GetMapping("/{userId}")
    public Result<AdminUserDetailResponse> getDetail(@PathVariable Long userId) {
        return Result.success(adminUserService.getDetail(userId));
    }

    @Operation(summary = "禁用用户")
    @OperationLog(module = "用户管理", operation = "禁用用户", type = OperationTypeEnum.DISABLE)
    @PreAuthorize("hasAuthority('admin:user:disable')")
    @PutMapping("/{userId}/disable")
    public Result<Boolean> disable(@PathVariable Long userId) {
        return Result.success(adminUserService.disable(userId));
    }

    @Operation(summary = "启用用户")
    @OperationLog(module = "用户管理", operation = "启用用户", type = OperationTypeEnum.ENABLE)
    @PreAuthorize("hasAuthority('admin:user:disable')")
    @PutMapping("/{userId}/enable")
    public Result<Boolean> enable(@PathVariable Long userId) {
        return Result.success(adminUserService.enable(userId));
    }

    @Operation(summary = "修改用户角色")
    @OperationLog(module = "用户管理", operation = "修改用户角色", type = OperationTypeEnum.GRANT)
    @PreAuthorize("hasAuthority('admin:user:write')")
    @PutMapping("/{userId}/roles")
    public Result<Boolean> updateRoles(
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserRoleUpdateRequest request
    ) {
        return Result.success(adminUserService.updateRoles(userId, request));
    }
}
```

---

# 15. AdminRoleService 设计

## 15.1 Service 接口

路径：

```text
src/main/java/com/internpilot/service/AdminRoleService.java
```

```java
package com.internpilot.service;

import com.internpilot.dto.admin.AdminRolePermissionUpdateRequest;
import com.internpilot.vo.admin.AdminRoleResponse;

import java.util.List;

public interface AdminRoleService {

    List<AdminRoleResponse> listRoles();

    AdminRoleResponse getDetail(Long roleId);

    Boolean updatePermissions(Long roleId, AdminRolePermissionUpdateRequest request);
}
```

---

## 15.2 AdminRoleController

路径：

```text
src/main/java/com/internpilot/controller/admin/AdminRoleController.java
```

```java
package com.internpilot.controller.admin;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.Result;
import com.internpilot.dto.admin.AdminRolePermissionUpdateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.AdminRoleService;
import com.internpilot.vo.admin.AdminRoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "查询角色列表")
    @PreAuthorize("hasAuthority('admin:role:read')")
    @GetMapping
    public Result<List<AdminRoleResponse>> listRoles() {
        return Result.success(adminRoleService.listRoles());
    }

    @Operation(summary = "查询角色详情")
    @PreAuthorize("hasAuthority('admin:role:read')")
    @GetMapping("/{roleId}")
    public Result<AdminRoleResponse> getDetail(@PathVariable Long roleId) {
        return Result.success(adminRoleService.getDetail(roleId));
    }

    @Operation(summary = "修改角色权限")
    @OperationLog(module = "角色权限", operation = "修改角色权限", type = OperationTypeEnum.GRANT)
    @PreAuthorize("hasAuthority('admin:role:write')")
    @PutMapping("/{roleId}/permissions")
    public Result<Boolean> updatePermissions(
            @PathVariable Long roleId,
            @RequestBody @Valid AdminRolePermissionUpdateRequest request
    ) {
        return Result.success(adminRoleService.updatePermissions(roleId, request));
    }
}
```

---

# 16. AdminPermissionService 设计

## 16.1 Service 接口

路径：

```text
src/main/java/com/internpilot/service/AdminPermissionService.java
```

```java
package com.internpilot.service;

import com.internpilot.vo.admin.PermissionResponse;

import java.util.List;

public interface AdminPermissionService {

    List<PermissionResponse> listPermissions(String resourceType);
}
```

---

## 16.2 AdminPermissionController

路径：

```text
src/main/java/com/internpilot/controller/admin/AdminPermissionController.java
```

```java
package com.internpilot.controller.admin;

import com.internpilot.common.Result;
import com.internpilot.service.AdminPermissionService;
import com.internpilot.vo.admin.PermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "查询权限列表")
    @PreAuthorize("hasAuthority('admin:permission:read')")
    @GetMapping
    public Result<List<PermissionResponse>> listPermissions(
            @RequestParam(required = false) String resourceType
    ) {
        return Result.success(adminPermissionService.listPermissions(resourceType));
    }
}
```

---

# 17. AdminDashboardService 设计

## 17.1 Service 接口

路径：

```text
src/main/java/com/internpilot/service/AdminDashboardService.java
```

```java
package com.internpilot.service;

import com.internpilot.vo.admin.AdminDashboardSummaryResponse;

public interface AdminDashboardService {

    AdminDashboardSummaryResponse getSummary();
}
```

---

## 17.2 AdminDashboardController

路径：

```text
src/main/java/com/internpilot/controller/admin/AdminDashboardController.java
```

```java
package com.internpilot.controller.admin;

import com.internpilot.common.Result;
import com.internpilot.service.AdminDashboardService;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "查询管理员后台看板统计")
    @PreAuthorize("hasAuthority('dashboard:admin:read')")
    @GetMapping("/summary")
    public Result<AdminDashboardSummaryResponse> getSummary() {
        return Result.success(adminDashboardService.getSummary());
    }
}
```

---

# 18. 前端管理员后台设计

## 18.1 前端目录结构

建议新增：

```text
src/views/admin
├── dashboard
│   └── AdminDashboard.vue
├── user
│   ├── AdminUserList.vue
│   └── AdminUserDetail.vue
├── role
│   ├── AdminRoleList.vue
│   └── AdminRoleDetail.vue
├── permission
│   └── AdminPermissionList.vue
└── log
    ├── OperationLogList.vue
    └── OperationLogDetail.vue
```

---

## 18.2 路由设计

```ts
{
  path: 'admin/dashboard',
  component: () => import('@/views/admin/dashboard/AdminDashboard.vue'),
  meta: { title: '后台看板', permission: 'dashboard:admin:read' }
},
{
  path: 'admin/users',
  component: () => import('@/views/admin/user/AdminUserList.vue'),
  meta: { title: '用户管理', permission: 'admin:user:read' }
},
{
  path: 'admin/roles',
  component: () => import('@/views/admin/role/AdminRoleList.vue'),
  meta: { title: '角色管理', permission: 'admin:role:read' }
},
{
  path: 'admin/permissions',
  component: () => import('@/views/admin/permission/AdminPermissionList.vue'),
  meta: { title: '权限管理', permission: 'admin:permission:read' }
},
{
  path: 'admin/operation-logs',
  component: () => import('@/views/admin/log/OperationLogList.vue'),
  meta: { title: '操作日志', permission: 'system:log:read' }
}
```

---

## 18.3 路由守卫增加权限判断

```ts
router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }

  const token = getToken()
  if (!token) {
    return '/login'
  }

  const permission = to.meta.permission as string | undefined
  if (permission && !hasPermission(permission)) {
    return '/403'
  }

  return true
})
```

---

## 18.4 前端菜单权限过滤

菜单配置示例：

```ts
const menus = [
  {
    title: '数据看板',
    path: '/dashboard'
  },
  {
    title: '简历管理',
    path: '/resumes',
    permission: 'resume:read'
  },
  {
    title: '管理员后台',
    permission: 'dashboard:admin:read',
    children: [
      {
        title: '后台看板',
        path: '/admin/dashboard',
        permission: 'dashboard:admin:read'
      },
      {
        title: '用户管理',
        path: '/admin/users',
        permission: 'admin:user:read'
      },
      {
        title: '角色管理',
        path: '/admin/roles',
        permission: 'admin:role:read'
      },
      {
        title: '权限管理',
        path: '/admin/permissions',
        permission: 'admin:permission:read'
      },
      {
        title: '操作日志',
        path: '/admin/operation-logs',
        permission: 'system:log:read'
      }
    ]
  }
]
```

过滤方法：

```ts
function filterMenus(menus: any[]) {
  return menus
    .filter(menu => !menu.permission || hasPermission(menu.permission))
    .map(menu => ({
      ...menu,
      children: menu.children ? filterMenus(menu.children) : undefined
    }))
}
```

---

# 19. 前端 API 封装

## 19.1 adminUser.ts

```ts
import request from '@/utils/request'

export function getAdminUserListApi(params: any) {
  return request.get('/api/admin/users', { params })
}

export function getAdminUserDetailApi(userId: number) {
  return request.get(`/api/admin/users/${userId}`)
}

export function disableUserApi(userId: number) {
  return request.put(`/api/admin/users/${userId}/disable`)
}

export function enableUserApi(userId: number) {
  return request.put(`/api/admin/users/${userId}/enable`)
}

export function updateUserRolesApi(userId: number, data: any) {
  return request.put(`/api/admin/users/${userId}/roles`, data)
}
```

---

## 19.2 adminRole.ts

```ts
import request from '@/utils/request'

export function getAdminRoleListApi() {
  return request.get('/api/admin/roles')
}

export function getAdminRoleDetailApi(roleId: number) {
  return request.get(`/api/admin/roles/${roleId}`)
}

export function updateRolePermissionsApi(roleId: number, data: any) {
  return request.put(`/api/admin/roles/${roleId}/permissions`, data)
}
```

---

## 19.3 adminPermission.ts

```ts
import request from '@/utils/request'

export function getAdminPermissionListApi(params?: any) {
  return request.get('/api/admin/permissions', { params })
}
```

---

## 19.4 adminDashboard.ts

```ts
import request from '@/utils/request'

export function getAdminDashboardSummaryApi() {
  return request.get('/api/admin/dashboard/summary')
}
```

---

# 20. 前端页面设计细节

## 20.1 AdminDashboard.vue

展示统计卡片：

```text
用户总数
今日新增用户
简历总数
岗位总数
AI 分析次数
AI 面试题报告数
投递记录数
失败操作数
```

使用组件：

```text
el-row
el-col
el-card
StatCard
ECharts
```

---

## 20.2 AdminUserList.vue

页面包含：

```text
搜索框：用户名 / 邮箱
角色筛选
启用状态筛选
用户表格
禁用 / 启用按钮
分配角色按钮
详情按钮
```

表格字段：

```text
用户ID
用户名
邮箱
学校
专业
年级
角色
状态
注册时间
操作
```

---

## 20.3 AdminRoleList.vue

页面包含：

```text
角色表格
角色详情
权限分配弹窗
```

权限分配建议使用：

```text
el-tree
el-checkbox-group
```

按 resourceType 分组展示权限。

---

## 20.4 AdminPermissionList.vue

页面包含：

```text
权限列表
资源类型筛选
权限编码
权限名称
描述
```

第一阶段只读，不允许新增和删除。

---

## 20.5 OperationLogList.vue

前面 `docs/23-system-operation-log-design.md` 已经设计过，可直接复用。

---

# 21. 关键业务规则

## 21.1 不允许禁用自己

管理员禁用用户时，需要判断：

```text
targetUserId != currentUserId
```

否则返回：

```text
不能禁用当前登录用户
```

---

## 21.2 不允许移除自己最后的 ADMIN 权限

如果当前管理员修改自己的角色，必须保证自己仍然有管理员权限。

否则可能出现：

```text
系统没有管理员
管理员把自己踢出后台
```

---

## 21.3 不允许删除系统内置角色

第一阶段建议内置角色：

```text
USER
ADMIN
```

不允许删除。

如果要禁用角色，也要防止禁用 ADMIN。

---

## 21.4 权限修改后如何生效

第一阶段：

```text
用户重新登录后生效
```

如果你的 RBAC 是每次请求从数据库加载权限，则可以立即生效。

如果后续加 Redis 权限缓存，需要在角色或权限变更后清理缓存：

```text
internpilot:auth:permissions:{userId}
```

---

# 22. 后端实现注意事项

## 22.1 查询用户角色

可以在 PermissionMapper 或 RoleMapper 中增加：

```java
List<String> selectRoleCodesByUserId(Long userId);
```

---

## 22.2 查询用户权限

```java
List<String> selectPermissionCodesByUserId(Long userId);
```

---

## 22.3 修改用户角色

推荐流程：

```text
校验目标用户存在
  ↓
校验角色 ID 都存在
  ↓
逻辑删除旧 user_role
  ↓
插入新 user_role
  ↓
记录操作日志
```

---

## 22.4 修改角色权限

推荐流程：

```text
校验角色存在
  ↓
校验权限 ID 都存在
  ↓
逻辑删除旧 role_permission
  ↓
插入新 role_permission
  ↓
记录操作日志
```

---

# 23. 测试流程

## 23.1 准备管理员账号

确保存在 ADMIN 用户。

查询：

```sql
SELECT u.id, u.username, r.role_code
FROM user u
JOIN user_role ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
WHERE r.role_code = 'ADMIN';
```

如果没有，需要手动分配：

```sql
INSERT INTO user_role (user_id, role_id)
SELECT 1, r.id
FROM role r
WHERE r.role_code = 'ADMIN';
```

---

## 23.2 管理员登录

```powershell
$body = @{
  username = "admin"
  password = "123456"
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

$adminToken = $response.data.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
```

---

## 23.3 查询用户列表

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/users?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $adminHeaders
```

期望：

```text
200 成功
```

---

## 23.4 普通用户访问后台接口

普通用户访问：

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/users?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $userHeaders
```

期望：

```text
403 Forbidden
```

---

## 23.5 禁用用户测试

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/users/2/disable" `
  -Method Put `
  -Headers $adminHeaders
```

检查数据库：

```sql
SELECT id, username, enabled
FROM user
WHERE id = 2;
```

期望：

```text
enabled = 0
```

---

## 23.6 启用用户测试

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/users/2/enable" `
  -Method Put `
  -Headers $adminHeaders
```

期望：

```text
enabled = 1
```

---

## 23.7 修改用户角色测试

```powershell
$body = @{
  roleIds = @(1, 2)
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/users/2/roles" `
  -Method Put `
  -ContentType "application/json" `
  -Headers $adminHeaders `
  -Body $body
```

检查：

```sql
SELECT *
FROM user_role
WHERE user_id = 2
  AND deleted = 0;
```

---

## 23.8 修改角色权限测试

```powershell
$body = @{
  permissionIds = @(1, 2, 3, 4)
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/roles/1/permissions" `
  -Method Put `
  -ContentType "application/json" `
  -Headers $adminHeaders `
  -Body $body
```

---

# 24. 前端验收流程

## 24.1 管理员登录

管理员登录后应该看到：

```text
管理员后台菜单
```

普通用户登录后不应该看到。

---

## 24.2 用户管理页

检查：

```text
用户列表能加载
搜索能用
角色筛选能用
状态筛选能用
禁用 / 启用按钮可用
分配角色弹窗可用
```

---

## 24.3 角色管理页

检查：

```text
角色列表能加载
角色权限能展示
权限分配能保存
```

---

## 24.4 权限管理页

检查：

```text
权限列表能加载
按资源类型分组展示
```

---

## 24.5 操作日志页

检查：

```text
日志列表能加载
日志详情能查看
普通用户不能访问
```

---

# 25. 常见问题

## 25.1 普通用户也能看到管理员菜单

原因：

```text
前端没有按 permissions 过滤菜单
```

解决：

```text
菜单配置加 permission 字段
渲染前 filterMenus
```

---

## 25.2 普通用户访问后台接口不是 403

可能原因：

```text
Controller 没加 @PreAuthorize
@EnableMethodSecurity 没开启
用户误拥有管理员权限
```

检查：

```sql
SELECT u.username, p.permission_code
FROM user u
JOIN user_role ur ON u.id = ur.user_id
JOIN role_permission rp ON ur.role_id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE u.username = '普通用户名';
```

---

## 25.3 管理员没有权限访问后台

可能原因：

```text
ADMIN 角色没有对应权限
JWT 登录响应 permissions 为空
CustomUserDetails 没加载权限
```

---

## 25.4 禁用用户后仍然能访问

原因：

```text
用户已有 Token 没有立即失效
```

第一阶段可以接受。

后续优化：

```text
JWT 黑名单
Redis Token Version
每次请求校验 user.enabled
```

推荐在 `CustomUserDetailsService` 加载用户时检查 enabled。

---

## 25.5 修改权限后没有立即生效

如果权限存在缓存，需要清缓存。

如果 JWT 中保存了 permissions，用户必须重新登录。

推荐方案：

```text
JWT 只保存 userId / username
权限每次从数据库或 Redis 加载
```

---

# 26. 面试讲解准备

## 26.1 面试官问：管理员后台做了什么？

回答：

```text
我在 InternPilot 中基于 RBAC 权限系统实现了管理员后台，主要包括用户管理、角色权限管理、权限查看、操作日志查看和后台数据看板。

管理员可以查看系统用户，禁用异常用户，给用户分配角色，也可以给角色分配权限。后台接口都使用 @PreAuthorize 做权限控制，例如 admin:user:read、admin:role:write、system:log:read，普通用户不能访问后台接口。
```

---

## 26.2 面试官问：管理员后台和 RBAC 怎么结合？

回答：

```text
RBAC 是管理员后台的权限基础。用户通过 user_role 绑定角色，角色通过 role_permission 绑定权限。

后台接口不是简单判断是否 ADMIN，而是使用细粒度权限，比如查看用户需要 admin:user:read，修改用户角色需要 admin:user:write，查看系统日志需要 system:log:read。

这样以后即使不是超级管理员，也可以配置不同的后台角色，比如只读管理员、用户管理员、日志审计员。
```

---

## 26.3 面试官问：如何防止管理员误操作？

回答：

```text
我设计了一些保护规则，比如不允许管理员禁用自己，不允许移除最后一个管理员权限，不建议删除系统内置的 USER 和 ADMIN 角色。

同时所有关键后台操作都会通过操作日志模块记录下来，包括谁在什么时候做了什么操作，是否成功，失败原因是什么，方便后续审计和排查。
```

---

## 26.4 面试官问：为什么权限管理中不允许随便新增权限？

回答：

```text
因为权限编码需要和后端代码中的 @PreAuthorize 注解对应。比如接口上写的是 hasAuthority('admin:user:read')，数据库里也必须有 admin:user:read。

如果管理员随便创建一个权限编码，但后端没有使用它，这个权限就没有实际意义。所以第一阶段权限由系统初始化，后台只负责查看和给角色分配权限。
```

---

## 26.5 面试官问：禁用用户后已有 Token 怎么办？

回答：

```text
第一阶段可以做到禁用后用户无法重新登录。如果用户已经登录，旧 Token 在过期前可能仍然有效。

更完善的做法是每次请求加载用户信息时检查 enabled 状态，或者使用 Redis 维护 Token 黑名单 / tokenVersion。当用户被禁用时，使旧 Token 立即失效。
```

---

# 27. 简历写法

完成管理员后台后，简历可以增加：

```text
- 基于 RBAC 权限模型实现管理员后台，支持用户管理、角色权限分配、系统操作日志查看和后台数据看板，并通过 @PreAuthorize 完成细粒度接口权限控制。
```

更完整写法：

```text
- 设计并实现管理员后台模块，管理员可查看用户列表、启用/禁用用户、分配用户角色、配置角色权限和查看系统操作日志；结合 RBAC 权限模型实现 admin:user:read、admin:role:write、system:log:read 等细粒度权限控制。
```

---

# 28. 开发顺序建议

推荐按以下顺序开发：

```text
1. 确认 RBAC 表和权限数据完整；
2. 补充管理员后台权限；
3. 给 ADMIN 分配后台权限；
4. 创建 AdminUserService；
5. 创建 AdminUserController；
6. 实现用户列表、详情、禁用、启用；
7. 实现用户角色分配；
8. 创建 AdminRoleService；
9. 创建 AdminRoleController；
10. 实现角色列表和权限分配；
11. 创建 AdminPermissionService；
12. 创建 AdminPermissionController；
13. 创建 AdminDashboardService；
14. 创建 AdminDashboardController；
15. 前端新增管理员后台路由；
16. 前端新增管理员菜单权限控制；
17. 前端实现用户管理页；
18. 前端实现角色管理页；
19. 前端实现权限管理页；
20. 前端接入操作日志页；
21. 测试普通用户访问后台返回 403；
22. 测试管理员完整后台功能。
```

---

# 29. 验收标准

## 29.1 后端验收

* [ ] 管理员可以查询用户列表；
* [ ] 管理员可以查看用户详情；
* [ ] 管理员可以禁用用户；
* [ ] 管理员可以启用用户；
* [ ] 管理员可以修改用户角色；
* [ ] 管理员可以查询角色列表；
* [ ] 管理员可以修改角色权限；
* [ ] 管理员可以查询权限列表；
* [ ] 管理员可以查询后台看板；
* [ ] 管理员可以查询操作日志；
* [ ] 普通用户访问 `/api/admin/**` 返回 403；
* [ ] 未登录访问 `/api/admin/**` 返回 401；
* [ ] 后台关键操作被记录到 system_operation_log。

---

## 29.2 前端验收

* [ ] 管理员登录后显示管理员后台菜单；
* [ ] 普通用户登录后不显示管理员后台菜单；
* [ ] 用户管理页面可以加载；
* [ ] 用户可以禁用 / 启用；
* [ ] 可以分配用户角色；
* [ ] 角色管理页面可以加载；
* [ ] 可以分配角色权限；
* [ ] 权限管理页面可以加载；
* [ ] 操作日志页面可以加载；
* [ ] 无权限访问后台页面跳转 403 页面。

---

# 30. 模块设计结论

管理员后台模块是 InternPilot 从个人求职工具升级为平台化系统的重要一步。

它基于：

```text
RBAC 权限系统
  ↓
系统操作日志
  ↓
管理员接口
  ↓
前端后台页面
```

实现了：

```text
用户管理
角色权限管理
权限查看
日志审计
后台数据看板
```

完成该模块后，InternPilot 将具备更完整的企业级后端项目特征：

```text
认证
授权
用户数据隔离
管理员后台
操作审计
AI 能力
前后端展示
```
