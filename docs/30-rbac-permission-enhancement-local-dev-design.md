# InternPilot 第二阶段 RBAC 权限系统增强设计文档（本地开发版）

## 一、文档目的

本文档用于设计 InternPilot 项目第二阶段的 RBAC 权限系统增强方案。

当前项目已经完成用户认证、JWT 鉴权、基础角色区分、管理员后台、系统操作日志等能力。第二阶段的目标是继续完善 RBAC 权限系统，使项目从“简单角色判断”升级为“可配置、可管理、可审计、可扩展”的企业级权限系统。

本阶段采用本地开发方式进行，不依赖 Docker 运行环境。

---

## 二、开发环境说明

### 2.1 当前开发方式

当前项目采用本地开发模式：

| 模块 | 运行方式 |
|---|---|
| 后端 | IDEA 启动 Spring Boot 或 `./gradlew bootRun` |
| 前端 | `npm run dev` |
| MySQL | 本机 MySQL |
| Redis | 本机 Redis |
| Docker | 已停止，不参与日常开发 |

### 2.2 本地开发环境结构

```text
Windows 本机
├── IDEA
│   └── 启动 Spring Boot 后端
├── VSCode / WebStorm
│   └── 启动 Vue 前端
├── 本机 MySQL
│   └── intern_pilot 数据库
├── 本机 Redis
│   └── 缓存 / WebSocket / AI 分析进度
└── Git
    └── main / dev / feature/rbac-permission
````

### 2.3 Docker 的定位

Docker 当前不是日常开发环境，而是后续用于：

1. 项目演示
    
2. 一键部署
    
3. 服务器部署
    
4. 面试展示
    
5. CI/CD 验证
    

本阶段 RBAC 开发时，优先保证本地环境稳定运行。

---

## 三、当前权限系统现状

### 3.1 已有能力

当前项目已经具备以下能力：

1. 用户注册
    
2. 用户登录
    
3. JWT Token 生成与校验
    
4. Spring Security 认证过滤
    
5. 管理员和普通用户角色区分
    
6. 后端部分接口使用 `@PreAuthorize` 做权限判断
    
7. 前端基于用户信息控制部分页面访问
    
8. 管理员后台已经具备初步管理能力
    
9. 系统操作日志模块已经存在
    

### 3.2 当前不足

当前权限系统仍然存在以下问题：

|问题|说明|
|---|---|
|权限粒度不够细|部分接口仍然依赖角色判断，缺少统一权限码控制|
|用户角色管理不够完善|需要支持管理员动态修改用户角色|
|角色权限管理不够完善|需要支持管理员动态调整角色拥有的权限|
|前端权限控制需要加强|路由、菜单、按钮需要统一基于权限控制|
|权限边界保护不足|需要防止删除最后一个管理员、移除自身管理员角色|
|权限变更审计不足|权限调整需要记录操作日志|
|本地开发数据需要整理|需要准备本地演示账号和权限初始化数据|

---

## 四、第二阶段建设目标

### 4.1 总体目标

第二阶段 RBAC 权限系统的核心目标是：

> 构建一个支持用户、角色、权限动态管理的权限系统，实现后端接口权限控制、前端路由权限控制、按钮权限控制和操作日志审计。

### 4.2 具体目标

1. 完善用户、角色、权限三层模型
    
2. 支持用户绑定多个角色
    
3. 支持角色绑定多个权限
    
4. 支持管理员维护用户角色
    
5. 支持管理员维护角色权限
    
6. 后端接口统一使用权限码控制
    
7. 前端路由基于权限码控制访问
    
8. 前端按钮基于权限码控制显示
    
9. 权限变更写入操作日志
    
10. 增加权限系统安全边界保护
    
11. 增加权限相关测试
    
12. 整理本地开发数据库初始化数据
    

---

## 五、RBAC 模型设计

### 5.1 RBAC 基本概念

RBAC 是 Role-Based Access Control，即基于角色的访问控制。

核心思想是：

```text
用户不直接拥有权限
用户拥有角色
角色拥有权限
用户通过角色间接获得权限
```

### 5.2 本项目 RBAC 关系

```text
User 用户
  ↓
UserRole 用户角色关系
  ↓
Role 角色
  ↓
RolePermission 角色权限关系
  ↓
Permission 权限
```

### 5.3 示例

```text
用户：admin
  ↓
角色：ADMIN
  ↓
权限：
- admin:dashboard
- user:read
- user:update
- role:read
- role:update
- permission:read
- permission:update
- operation-log:read
```

```text
用户：demo
  ↓
角色：USER
  ↓
权限：
- resume:read
- resume:create
- resume:update
- job:read
- job:create
- analysis:create
- analysis:read
- interview-question:create
- interview-question:read
```

---

## 六、角色设计

### 6.1 系统内置角色

|角色编码|角色名称|说明|
|---|---|---|
|ADMIN|系统管理员|可以访问后台管理系统|
|USER|普通用户|可以使用简历、岗位、分析、推荐、面试题等功能|

### 6.2 角色设计原则

1. 角色编码使用英文大写
    
2. 系统内置角色不允许删除
    
3. ADMIN 角色必须至少保留一个用户
    
4. 普通用户默认绑定 USER 角色
    
5. 后续可以扩展更多角色，例如 REVIEWER、OPERATOR
    

### 6.3 后续可扩展角色

|角色编码|角色名称|说明|
|---|---|---|
|OPERATOR|运营人员|管理岗位、RAG 知识库、部分日志|
|REVIEWER|审核人员|审核用户简历、岗位内容|
|GUEST|访客|只允许访问公开页面|

第二阶段暂时只重点实现：

```text
ADMIN
USER
```

---

## 七、权限设计

### 7.1 权限类型

权限建议分为三类：

|权限类型|说明|示例|
|---|---|---|
|MENU|菜单权限|是否显示用户管理菜单|
|BUTTON|按钮权限|是否显示删除按钮|
|API|接口权限|是否允许访问某个接口|

### 7.2 权限码命名规范

权限码统一采用：

```text
模块:操作
```

示例：

```text
user:read
user:create
user:update
user:delete
role:read
role:create
role:update
role:delete
permission:read
permission:create
permission:update
permission:delete
resume:read
resume:create
resume:update
resume:delete
job:read
job:create
job:update
job:delete
analysis:read
analysis:create
rag:read
rag:manage
operation-log:read
```

### 7.3 命名规则

1. 全部小写
    
2. 模块和操作之间使用英文冒号
    
3. 不使用中文权限码
    
4. 模块名尽量和业务模块一致
    
5. 操作名尽量统一使用 read、create、update、delete、manage
    

---

## 八、数据库设计增强

### 8.1 user 用户表

用户表用于保存系统用户信息。

核心字段：

|字段|说明|
|---|---|
|id|用户 ID|
|username|用户名|
|password|BCrypt 加密密码|
|email|邮箱|
|real_name|真实姓名|
|enabled|是否启用|
|deleted|是否逻辑删除|
|created_at|创建时间|
|updated_at|更新时间|

### 8.2 role 角色表

角色表用于保存系统角色。

核心字段：

|字段|说明|
|---|---|
|id|角色 ID|
|role_code|角色编码，例如 ADMIN|
|role_name|角色名称|
|description|角色描述|
|enabled|是否启用|
|deleted|是否逻辑删除|
|created_at|创建时间|
|updated_at|更新时间|

### 8.3 permission 权限表

权限表用于保存系统权限。

核心字段：

|字段|说明|
|---|---|
|id|权限 ID|
|permission_code|权限码，例如 user:read|
|permission_name|权限名称|
|permission_type|权限类型，MENU / BUTTON / API|
|parent_id|父级权限 ID|
|path|前端路径或后端接口路径|
|method|HTTP 方法|
|sort_order|排序|
|enabled|是否启用|
|deleted|是否逻辑删除|
|created_at|创建时间|
|updated_at|更新时间|

### 8.4 user_role 用户角色关系表

用于维护用户和角色的多对多关系。

|字段|说明|
|---|---|
|id|主键|
|user_id|用户 ID|
|role_id|角色 ID|

### 8.5 role_permission 角色权限关系表

用于维护角色和权限的多对多关系。

|字段|说明|
|---|---|
|id|主键|
|role_id|角色 ID|
|permission_id|权限 ID|

---

## 九、后端权限控制设计

### 9.1 后端认证授权流程

```text
用户登录
  ↓
后端校验用户名和密码
  ↓
生成 JWT Token
  ↓
前端保存 Token
  ↓
请求接口时携带 Token
  ↓
JwtAuthenticationFilter 解析 Token
  ↓
根据用户 ID 查询角色和权限
  ↓
封装 GrantedAuthority
  ↓
写入 SecurityContext
  ↓
Controller 使用 @PreAuthorize 校验权限
```

### 9.2 GrantedAuthority 设计

Spring Security 最终识别的是权限字符串。

本项目中建议同时放入：

```text
ROLE_ADMIN
ROLE_USER
user:read
user:update
role:read
role:update
permission:read
permission:update
```

其中：

|类型|示例|用途|
|---|---|---|
|角色|ROLE_ADMIN|用于角色级判断|
|权限|user:read|用于细粒度权限判断|

### 9.3 Controller 权限控制示例

用户管理接口：

```java
@PreAuthorize("hasAuthority('user:read')")
@GetMapping("/api/admin/users")
public Result<List<UserVO>> listUsers() {
    return Result.success(userService.listUsers());
}
```

角色管理接口：

```java
@PreAuthorize("hasAuthority('role:update')")
@PutMapping("/api/admin/roles/{id}")
public Result<Void> updateRole(@PathVariable Long id,
                               @RequestBody RoleUpdateRequest request) {
    roleService.updateRole(id, request);
    return Result.success();
}
```

角色权限分配接口：

```java
@PreAuthorize("hasAuthority('role:update')")
@PutMapping("/api/admin/roles/{id}/permissions")
public Result<Void> updateRolePermissions(@PathVariable Long id,
                                          @RequestBody RolePermissionUpdateRequest request) {
    roleService.updateRolePermissions(id, request);
    return Result.success();
}
```

用户角色分配接口：

```java
@PreAuthorize("hasAuthority('user:update')")
@PutMapping("/api/admin/users/{id}/roles")
public Result<Void> updateUserRoles(@PathVariable Long id,
                                    @RequestBody UserRoleUpdateRequest request) {
    userService.updateUserRoles(id, request);
    return Result.success();
}
```

---

## 十、SecurityConfig 设计

### 10.1 设计原则

`SecurityConfig` 只负责粗粒度路径控制：

1. 登录注册接口放行
    
2. Swagger 接口放行
    
3. 静态资源放行
    
4. 其他接口需要登录
    
5. 具体业务权限交给 `@PreAuthorize`
    

### 10.2 推荐配置思路

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
.requestMatchers("/api/admin/**").authenticated()
.anyRequest().authenticated()
```

### 10.3 为什么不在 SecurityConfig 写死所有权限

不建议这样写：

```java
.requestMatchers("/api/admin/users/**").hasAuthority("user:read")
.requestMatchers("/api/admin/roles/**").hasAuthority("role:read")
.requestMatchers("/api/admin/permissions/**").hasAuthority("permission:read")
```

原因：

1. URL 和权限耦合太重
    
2. 接口变多后配置臃肿
    
3. Controller 上看不到权限要求
    
4. 不利于维护
    

推荐方式：

```text
SecurityConfig 控制是否登录
@PreAuthorize 控制是否有具体权限
```

---

## 十一、管理员后台接口设计

### 11.1 用户管理接口

|方法|路径|权限码|说明|
|---|---|---|---|
|GET|`/api/admin/users`|`user:read`|查询用户列表|
|GET|`/api/admin/users/{id}`|`user:read`|查询用户详情|
|PUT|`/api/admin/users/{id}`|`user:update`|修改用户信息|
|PUT|`/api/admin/users/{id}/status`|`user:update`|启用或禁用用户|
|PUT|`/api/admin/users/{id}/roles`|`user:update`|修改用户角色|
|DELETE|`/api/admin/users/{id}`|`user:delete`|删除用户|

### 11.2 角色管理接口

|方法|路径|权限码|说明|
|---|---|---|---|
|GET|`/api/admin/roles`|`role:read`|查询角色列表|
|GET|`/api/admin/roles/{id}`|`role:read`|查询角色详情|
|POST|`/api/admin/roles`|`role:create`|新增角色|
|PUT|`/api/admin/roles/{id}`|`role:update`|修改角色|
|DELETE|`/api/admin/roles/{id}`|`role:delete`|删除角色|
|PUT|`/api/admin/roles/{id}/permissions`|`role:update`|修改角色权限|

### 11.3 权限管理接口

|方法|路径|权限码|说明|
|---|---|---|---|
|GET|`/api/admin/permissions`|`permission:read`|查询权限列表|
|GET|`/api/admin/permissions/tree`|`permission:read`|查询权限树|
|POST|`/api/admin/permissions`|`permission:create`|新增权限|
|PUT|`/api/admin/permissions/{id}`|`permission:update`|修改权限|
|DELETE|`/api/admin/permissions/{id}`|`permission:delete`|删除权限|

### 11.4 当前用户信息接口

|方法|路径|说明|
|---|---|---|
|GET|`/api/user/me`|获取当前登录用户信息、角色列表、权限列表|

返回示例：

```json
{
  "id": 1,
  "username": "admin",
  "realName": "系统管理员",
  "roles": ["ADMIN"],
  "permissions": [
    "admin:dashboard",
    "user:read",
    "user:update",
    "role:read",
    "role:update",
    "permission:read",
    "permission:update",
    "operation-log:read"
  ]
}
```

---

## 十二、前端权限控制设计

### 12.1 前端权限控制目标

前端需要完成三类权限控制：

|类型|说明|
|---|---|
|路由权限|没有权限不能进入页面|
|菜单权限|没有权限不显示菜单|
|按钮权限|没有权限不显示按钮|

### 12.2 路由权限设计

路由中配置权限码：

```ts
{
  path: '/admin/users',
  name: 'AdminUserList',
  component: () => import('@/views/admin/AdminUserList.vue'),
  meta: {
    requiresAuth: true,
    permission: 'user:read'
  }
}
```

### 12.3 路由守卫设计

重点：刷新页面后，Pinia 中的用户信息可能为空，但 token 还在。

所以路由守卫中需要先加载当前用户信息：

```ts
router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }

  const token = getToken()
  if (!token) {
    return '/login'
  }

  const authStore = useAuthStore()

  if (!authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch (error) {
      removeToken()
      return '/login'
    }
  }

  const permission = to.meta.permission as string | undefined

  if (permission && !authStore.hasPermission(permission)) {
    return '/403'
  }

  return true
})
```

### 12.4 按钮权限设计

封装权限判断函数：

```ts
export function hasPermission(permission: string): boolean {
  const authStore = useAuthStore()
  return authStore.user?.permissions?.includes(permission) ?? false
}
```

页面中使用：

```vue
<el-button
  v-if="hasPermission('user:update')"
  type="primary"
>
  编辑用户
</el-button>
```

### 12.5 后续可封装指令

后续可以封装成：

```vue
<el-button v-permission="'user:update'">
  编辑用户
</el-button>
```

第二阶段可以先使用 `v-if="hasPermission(...)"`，实现简单、调试方便。

---

## 十三、安全边界设计

### 13.1 前端权限不是安全边界

前端隐藏按钮只能提升用户体验，不能保证安全。

真正的安全控制必须放在后端：

```text
前端控制显示
后端控制访问
```

即使按钮隐藏，用户仍然可以通过 Postman 或浏览器控制台直接请求接口。

所以所有关键接口必须加：

```java
@PreAuthorize
```

### 13.2 防止删除最后一个管理员

系统必须至少保留一个可用管理员。

以下操作需要拦截：

1. 删除最后一个 ADMIN 用户
    
2. 禁用最后一个 ADMIN 用户
    
3. 移除最后一个 ADMIN 用户的 ADMIN 角色
    
4. 删除 ADMIN 角色
    

### 13.3 防止管理员移除自己的权限

当前登录管理员不能移除自己的 ADMIN 角色。

否则会出现：

```text
管理员把自己的权限删掉
→ 无法进入后台
→ 系统无人可维护
```

### 13.4 禁止删除内置角色

以下角色属于系统内置角色：

```text
ADMIN
USER
```

建议规则：

|操作|是否允许|
|---|---|
|修改角色描述|允许|
|修改角色名称|谨慎允许|
|删除角色|不允许|
|修改角色编码|不允许|

### 13.5 禁止删除正在使用的权限

如果权限已经被角色绑定，不建议直接物理删除。

推荐策略：

1. 优先逻辑删除
    
2. 删除前检查绑定关系
    
3. 已绑定权限必须先解除绑定再删除
    

---

## 十四、操作日志设计

### 14.1 权限系统需要记录的操作

|操作|日志类型|
|---|---|
|新增角色|ROLE_CREATE|
|修改角色|ROLE_UPDATE|
|删除角色|ROLE_DELETE|
|修改角色权限|ROLE_PERMISSION_UPDATE|
|修改用户角色|USER_ROLE_UPDATE|
|启用用户|USER_ENABLE|
|禁用用户|USER_DISABLE|
|新增权限|PERMISSION_CREATE|
|修改权限|PERMISSION_UPDATE|
|删除权限|PERMISSION_DELETE|

### 14.2 日志内容

日志建议记录：

|字段|说明|
|---|---|
|operator_id|操作人 ID|
|operator_username|操作人用户名|
|module|操作模块|
|operation_type|操作类型|
|target_id|被操作对象 ID|
|description|操作描述|
|ip|操作 IP|
|user_agent|浏览器信息|
|created_at|操作时间|

### 14.3 日志描述示例

```text
管理员 admin 修改了用户 demo 的角色：USER → ADMIN
```

```text
管理员 admin 修改了角色 USER 的权限：新增 resume:create，移除 job:delete
```

---

## 十五、本地开发数据库初始化方案

### 15.1 本地开发数据库和 Docker 数据库区分

当前项目采用本地开发，因此主要使用本机 MySQL。

```text
本机 MySQL
  ↓
intern_pilot 数据库
  ↓
IDEA 启动的 Spring Boot 连接该数据库
```

Docker 数据库已经停止，不参与本阶段开发。

### 15.2 本地数据库初始化方式

本地开发时，可以通过以下方式初始化数据：

```text
方式一：使用 Navicat / DataGrip / MySQL Workbench 执行 SQL
方式二：使用 mysql 命令行导入 SQL
方式三：通过后端启动时自动执行初始化脚本
```

当前更推荐：

```text
使用 SQL 工具手动导入初始化脚本
```

因为这样更容易控制数据，不容易误删已有开发数据。

### 15.3 建议准备本地初始化脚本

建议新增：

```text
backend/intern-pilot-backend/src/main/resources/sql/dev-init-rbac.sql
```

用途：

```text
专门初始化本地开发环境需要的角色、权限、演示账号
```

不要直接把所有测试数据都塞进主 `init.sql`。

建议 SQL 文件职责划分：

|文件|用途|
|---|---|
|`schema.sql`|建表|
|`init.sql`|基础初始化数据|
|`dev-init-rbac.sql`|本地开发 RBAC 演示数据|
|`demo-data.sql`|演示环境业务数据|

当前项目可以先保留原有结构，新增 `dev-init-rbac.sql` 即可。

---

## 十六、本地开发启动流程

### 16.1 启动 MySQL

确认本机 MySQL 已启动，并存在数据库：

```sql
CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 16.2 启动 Redis

确认本机 Redis 已启动。

Windows 下可以使用本地 Redis 服务，或者使用已有 Redis 安装。

### 16.3 启动后端

进入后端目录：

```bash
cd backend/intern-pilot-backend
```

方式一：IDEA 启动主类。

方式二：命令行启动：

```bash
./gradlew bootRun
```

Windows PowerShell：

```powershell
.\gradlew bootRun
```

### 16.4 启动前端

进入前端目录：

```bash
cd frontend/intern-pilot-frontend
```

安装依赖：

```bash
npm install
```

启动：

```bash
npm run dev
```

### 16.5 访问地址

|服务|地址|
|---|---|
|前端|`http://localhost:5173`|
|后端|`http://localhost:8080`|
|Swagger|`http://localhost:8080/swagger-ui/index.html`|

---

## 十七、分支设计

### 17.1 当前分支结构

当前仓库已有：

```text
main
dev
```

RBAC 权限增强建议新建功能分支：

```text
feature/rbac-permission
```

不要使用：

```text
feature/rabc
```

原因：

```text
正确术语是 RBAC，不是 RABC
```

### 17.2 分支职责

|分支|职责|说明|
|---|---|---|
|main|稳定分支|只保存可运行、可演示、可发布版本|
|dev|开发集成分支|多个 feature 分支完成后合并到这里统一测试|
|feature/rbac-permission|RBAC 功能分支|第二阶段 RBAC 权限增强开发分支|

### 17.3 推荐开发流程

```text
main
  ↑
dev
  ↑
feature/rbac-permission
```

具体流程：

```text
1. 从 dev 创建 feature/rbac-permission
2. 在 feature/rbac-permission 上开发 RBAC 功能
3. 本地测试通过后合并到 dev
4. dev 集成测试通过后合并到 main
5. main 打版本 tag
```

---

## 十八、Git 操作流程

### 18.1 创建 RBAC 功能分支

```bash
git checkout dev
git pull origin dev
git checkout -b feature/rbac-permission
```

### 18.2 开发过程中提交

```bash
git status
git add .
git commit -m "完善第二阶段 RBAC 权限系统设计与基础实现"
```

### 18.3 推送功能分支

```bash
git push -u origin feature/rbac-permission
```

### 18.4 合并到 dev

```bash
git checkout dev
git pull origin dev
git merge feature/rbac-permission
git push origin dev
```

### 18.5 dev 稳定后合并到 main

```bash
git checkout main
git pull origin main
git merge dev
git push origin main
```

### 18.6 打版本标签

```bash
git tag v0.2.0-rbac
git push origin v0.2.0-rbac
```

---

## 十九、开发任务拆分

### 19.1 后端任务

|编号|任务|
|---|---|
|RBAC-BE-01|检查 User、Role、Permission、UserRole、RolePermission 实体|
|RBAC-BE-02|完善用户角色查询 Mapper|
|RBAC-BE-03|完善用户权限查询 Mapper|
|RBAC-BE-04|修改 CustomUserDetails，加入权限集合|
|RBAC-BE-05|完善 `/api/user/me` 返回角色和权限|
|RBAC-BE-06|完善用户管理接口|
|RBAC-BE-07|完善角色管理接口|
|RBAC-BE-08|完善权限管理接口|
|RBAC-BE-09|完善用户角色分配接口|
|RBAC-BE-10|完善角色权限分配接口|
|RBAC-BE-11|增加管理员危险操作保护|
|RBAC-BE-12|权限变更写入操作日志|
|RBAC-BE-13|增加 RBAC 相关测试|

### 19.2 前端任务

|编号|任务|
|---|---|
|RBAC-FE-01|完善 AuthStore，保存用户角色和权限|
|RBAC-FE-02|修改路由守卫，刷新后自动加载用户信息|
|RBAC-FE-03|增加 403 页面|
|RBAC-FE-04|完善用户管理页面|
|RBAC-FE-05|完善角色管理页面|
|RBAC-FE-06|完善权限管理页面|
|RBAC-FE-07|增加用户角色分配组件|
|RBAC-FE-08|增加角色权限分配组件|
|RBAC-FE-09|封装 hasPermission 方法|
|RBAC-FE-10|页面按钮增加权限判断|

### 19.3 数据库任务

|编号|任务|
|---|---|
|RBAC-DB-01|检查角色表结构|
|RBAC-DB-02|检查权限表结构|
|RBAC-DB-03|初始化 ADMIN、USER 角色|
|RBAC-DB-04|初始化系统权限|
|RBAC-DB-05|初始化角色权限关系|
|RBAC-DB-06|初始化本地开发演示账号|
|RBAC-DB-07|初始化必要演示数据|

---

## 二十、本地测试方案

### 20.1 后端测试

进入后端目录：

```bash
cd backend/intern-pilot-backend
```

执行：

```bash
./gradlew clean test
```

Windows PowerShell：

```powershell
.\gradlew clean test
```

通过标准：

```text
BUILD SUCCESSFUL
```

### 20.2 前端测试

进入前端目录：

```bash
cd frontend/intern-pilot-frontend
```

执行：

```bash
npm install
npm run type-check
npm run build
```

通过标准：

```text
type-check 通过
vite build 成功
```

### 20.3 手动功能测试

|测试项|预期结果|
|---|---|
|admin 登录|成功|
|demo 登录|成功|
|未登录访问后台|跳转登录页|
|普通用户访问后台|跳转 403|
|管理员访问后台|正常进入|
|管理员查看用户列表|成功|
|管理员修改用户角色|成功|
|管理员修改角色权限|成功|
|没有权限的按钮|不显示|
|无权限直接请求接口|返回 403|
|权限变更|记录操作日志|
|刷新后台页面|不误跳 403|

---

## 二十一、验收标准

第二阶段 RBAC 权限系统完成后，需要满足：

1. 用户可以绑定多个角色
    
2. 角色可以绑定多个权限
    
3. 管理员可以修改用户角色
    
4. 管理员可以修改角色权限
    
5. 后端接口使用权限码控制访问
    
6. 前端路由使用权限码控制访问
    
7. 前端按钮使用权限码控制显示
    
8. 普通用户无法访问管理员后台
    
9. 无权限访问接口返回 403
    
10. 权限变更写入操作日志
    
11. 系统至少保留一个管理员
    
12. 管理员不能移除自己的 ADMIN 权限
    
13. 内置角色 ADMIN、USER 不允许删除
    
14. 本地开发环境可以正常启动
    
15. 后端测试通过
    
16. 前端构建通过
    
17. feature 分支合并到 dev 前无明显报错
    

---

## 二十二、开发优先级

### P0：必须完成

1. `/api/user/me` 返回角色和权限
    
2. 后端接口补充 `@PreAuthorize`
    
3. 前端路由权限守卫修复
    
4. 用户角色分配
    
5. 角色权限分配
    
6. 普通用户访问后台返回 403
    
7. 管理员可以正常访问后台
    

### P1：重要增强

1. 按钮权限控制
    
2. 权限树展示
    
3. 操作日志记录
    
4. 防止删除最后一个管理员
    
5. 防止管理员移除自身 ADMIN 角色
    
6. 初始化本地演示账号
    

### P2：后续优化

1. 动态菜单
    
2. Redis 缓存权限
    
3. 权限变更后 Token 失效
    
4. 数据权限
    
5. 部门权限
    
6. 超级管理员机制
    

---

## 二十三、面试表达方式

本阶段可以在面试中这样介绍：

> 我在 InternPilot 项目中设计并实现了 RBAC 权限系统。系统采用用户、角色、权限三层模型，用户通过角色间接获得权限。后端基于 Spring Security 和 `@PreAuthorize` 实现接口级权限控制，前端基于 Vue Router 和 Pinia 实现路由和按钮权限控制。同时，我加入了用户角色分配、角色权限分配、管理员危险操作保护和操作日志审计，使权限系统更接近企业后台管理场景。

可以强调的项目亮点：

1. 使用 Spring Security 实现认证授权
    
2. 使用 JWT 实现前后端分离登录
    
3. 使用 RBAC 模型实现权限解耦
    
4. 使用 `@PreAuthorize` 做接口级权限控制
    
5. 使用 Pinia 保存用户权限状态
    
6. 使用 Vue Router 实现前端路由拦截
    
7. 使用操作日志记录权限变更
    
8. 考虑了最后一个管理员保护等安全边界
    

---

## 二十四、总结

第二阶段 RBAC 权限系统增强的核心目标，不是简单增加几个管理页面，而是让 InternPilot 从普通业务系统升级为具备企业级后台管理能力的系统。

本阶段完成后，项目将具备：

1. 更完整的认证授权体系
    
2. 更清晰的用户、角色、权限关系
    
3. 更专业的后台权限控制能力
    
4. 更完善的前端权限体验
    
5. 更安全的管理员操作保护
    
6. 更适合面试展示的企业级项目亮点
    

当前开发方式采用本地开发，不依赖 Docker。

本地开发完成并测试通过后，再合并到 `dev` 分支；`dev` 稳定后，再合并到 `main` 分支。

````



