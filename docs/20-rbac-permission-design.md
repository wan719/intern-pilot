# InternPilot RBAC 权限系统设计与实现文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的 RBAC 权限系统设计与实现方案，包括权限模型、数据库设计、接口设计、Spring Security 集成、用户角色权限加载、管理员权限控制、数据初始化、测试流程和后续扩展方案。

当前 InternPilot 已经实现了基础 JWT 登录鉴权，但角色权限仍然较简单。为了让项目更接近企业级后端系统，下一步将引入 RBAC 权限模型。

RBAC 全称是：

```text
Role-Based Access Control
基于角色的访问控制

其核心思想是：

用户不直接绑定权限
用户绑定角色
角色绑定权限
用户通过角色间接获得权限
```

2. 当前权限系统存在的问题

当前项目第一阶段权限设计较简单：

user 表中有 role 字段
role = USER / ADMIN
Spring Security 根据 role 判断权限

这种方式可以满足 MVP 阶段，但存在几个问题：

一个用户只能有一个角色；
角色和权限耦合在 user 表里；
无法灵活配置权限；
不方便做管理员后台；
不方便扩展多个角色；
不能做到细粒度接口权限控制；
不能支持动态权限管理。

例如当前可能是：

.requestMatchers("/api/admin/**").hasRole("ADMIN")

但企业项目中更常见的是：

@PreAuthorize("hasAuthority('user:read')")
@PreAuthorize("hasAuthority('resume:write')")
@PreAuthorize("hasAuthority('system:log:read')")
3. RBAC 改造目标

RBAC 权限系统需要完成以下目标：

支持用户绑定多个角色；
支持角色绑定多个权限；
支持根据用户 ID 查询角色和权限；
登录后 JWT 中仍然保存基础用户信息；
每次请求通过 UserDetails 加载权限；
Spring Security 能识别角色和权限；
支持 hasRole；
支持 hasAuthority；
支持管理员接口权限控制；
为后续管理员后台做准备。
4. RBAC 模型设计
4.1 核心模型

RBAC 模型包含五张核心表：

user
role
permission
user_role
role_permission

关系如下：

user
  ↓ 多对多
role
  ↓ 多对多
permission

完整关系：

一个用户可以拥有多个角色
一个角色可以分配给多个用户
一个角色可以拥有多个权限
一个权限可以分配给多个角色
4.2 示例

用户：

wan

拥有角色：

USER

USER 角色拥有权限：

resume:read
resume:write
job:read
job:write
analysis:read
analysis:write
application:read
application:write

管理员：

admin

拥有角色：

ADMIN

ADMIN 角色拥有权限：

user:read
user:write
role:read
role:write
permission:read
permission:write
system:log:read
5. 权限命名规范
5.1 权限格式

推荐权限格式：

资源:操作

例如：

resume:read
resume:write
job:read
job:write
analysis:read
analysis:write
application:read
application:write
admin:user:read
admin:user:write
system:log:read
5.2 操作类型
| 操作 | 含义 |
|------|------|
| read | 查询 |
| write | 新增 / 修改 |
| delete | 删除 |
| manage | 管理 |
| export | 导出 |
5.3 推荐权限清单
用户普通权限
resume:read
resume:write
resume:delete

job:read
job:write
job:delete

analysis:read
analysis:write
analysis:delete

application:read
application:write
application:delete
管理员权限
admin:user:read
admin:user:write
admin:user:disable

admin:role:read
admin:role:write

admin:permission:read
admin:permission:write

system:log:read
system:log:delete

dashboard:admin:read
6. 数据库设计
6.1 role 表
CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码，如 USER / ADMIN',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_role_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
6.2 permission 表
CREATE TABLE IF NOT EXISTS permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码，如 resume:read',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(50) DEFAULT NULL COMMENT '资源类型',
    description VARCHAR(255) DEFAULT NULL COMMENT '权限描述',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_permission_resource_type (resource_type),
    KEY idx_permission_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
6.3 user_role 表
CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_user_id (user_id),
    KEY idx_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';
6.4 role_permission 表
CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_permission_role_id (role_id),
    KEY idx_role_permission_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
6.5 user 表是否保留 role 字段

当前 user 表中可能已有：

role VARCHAR(30)

RBAC 改造后有两种方案。

方案 A：保留 role 字段

保留 user.role，作为兼容字段。

优点：

改动小；
不影响现有代码；
老接口不容易坏；
方便快速上线。

缺点：

user.role 和 user_role 可能出现不一致；
不够纯粹。
方案 B：删除 role 字段

完全使用 user_role。

优点：

模型更规范；
支持多角色；
没有冗余字段。

缺点：

改动较大；
现有 AuthUserResponse、LoginResponse 需要调整；
可能影响测试。
推荐方案

第一阶段推荐：

保留 user.role 字段
但实际权限判断以 user_role + role_permission 为准

也就是：

user.role 用于展示默认角色
user_role 用于真正权限控制

后续稳定后再考虑删除 user.role。

7. 初始化数据设计
7.1 初始化角色
INSERT INTO role (role_code, role_name, description, enabled)
VALUES
('USER', '普通用户', '普通注册用户', 1),
('ADMIN', '系统管理员', '系统管理员，拥有后台管理权限', 1);
7.2 初始化权限
INSERT INTO permission (permission_code, permission_name, resource_type, description, enabled)
VALUES
('resume:read', '查看简历', 'RESUME', '查看自己的简历', 1),
('resume:write', '编辑简历', 'RESUME', '上传、修改自己的简历', 1),
('resume:delete', '删除简历', 'RESUME', '删除自己的简历', 1),

('job:read', '查看岗位', 'JOB', '查看自己的岗位JD', 1),
('job:write', '编辑岗位', 'JOB', '创建和修改自己的岗位JD', 1),
('job:delete', '删除岗位', 'JOB', '删除自己的岗位JD', 1),

('analysis:read', '查看分析报告', 'ANALYSIS', '查看自己的AI分析报告', 1),
('analysis:write', '创建分析报告', 'ANALYSIS', '发起AI匹配分析', 1),
('analysis:delete', '删除分析报告', 'ANALYSIS', '删除自己的分析报告', 1),

('application:read', '查看投递记录', 'APPLICATION', '查看自己的投递记录', 1),
('application:write', '编辑投递记录', 'APPLICATION', '创建和修改自己的投递记录', 1),
('application:delete', '删除投递记录', 'APPLICATION', '删除自己的投递记录', 1),

('admin:user:read', '查看用户管理', 'ADMIN_USER', '查看用户列表和详情', 1),
('admin:user:write', '编辑用户管理', 'ADMIN_USER', '修改用户信息', 1),
('admin:user:disable', '禁用用户', 'ADMIN_USER', '禁用或启用用户', 1),

('admin:role:read', '查看角色管理', 'ADMIN_ROLE', '查看角色列表和详情', 1),
('admin:role:write', '编辑角色管理', 'ADMIN_ROLE', '创建和修改角色', 1),

('admin:permission:read', '查看权限管理', 'ADMIN_PERMISSION', '查看权限列表', 1),
('admin:permission:write', '编辑权限管理', 'ADMIN_PERMISSION', '修改权限配置', 1),

('system:log:read', '查看系统日志', 'SYSTEM_LOG', '查看系统操作日志', 1),
('dashboard:admin:read', '查看管理员看板', 'DASHBOARD', '查看管理员统计数据', 1);
7.3 给 USER 分配权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p
WHERE r.role_code = 'USER'
  AND p.permission_code IN (
      'resume:read',
      'resume:write',
      'resume:delete',
      'job:read',
      'job:write',
      'job:delete',
      'analysis:read',
      'analysis:write',
      'analysis:delete',
      'application:read',
      'application:write',
      'application:delete'
  );
7.4 给 ADMIN 分配全部权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p
WHERE r.role_code = 'ADMIN';
7.5 给已有用户分配 USER 角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.role_code = 'USER'
WHERE u.role = 'USER'
  AND NOT EXISTS (
      SELECT 1
      FROM user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
        AND ur.deleted = 0
  );
7.6 给已有管理员分配 ADMIN 角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.role_code = 'ADMIN'
WHERE u.role = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
        AND ur.deleted = 0
  );
8. Entity 设计
8.1 Role Entity

路径：

src/main/java/com/internpilot/entity/Role.java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
8.2 Permission Entity

路径：

src/main/java/com/internpilot/entity/Permission.java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String permissionCode;

    private String permissionName;

    private String resourceType;

    private String description;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
8.3 UserRole Entity

路径：

src/main/java/com/internpilot/entity/UserRole.java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_role")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
8.4 RolePermission Entity

路径：

src/main/java/com/internpilot/entity/RolePermission.java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role_permission")
public class RolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long permissionId;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
9. Mapper 设计
9.1 RoleMapper

路径：

src/main/java/com/internpilot/mapper/RoleMapper.java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
9.2 PermissionMapper

路径：

src/main/java/com/internpilot/mapper/PermissionMapper.java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
9.3 UserRoleMapper

路径：

src/main/java/com/internpilot/mapper/UserRoleMapper.java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
9.4 RolePermissionMapper

路径：

src/main/java/com/internpilot/mapper/RolePermissionMapper.java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
10. 权限查询 Mapper 设计
10.1 为什么需要自定义查询

Spring Security 加载用户权限时，需要一次性查出：

用户拥有的角色
用户拥有的权限

使用多次 BaseMapper 查询也可以，但代码较复杂。

推荐在 PermissionMapper 中增加自定义 SQL。

10.2 PermissionMapper 增加方法
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM permission p
            JOIN role_permission rp ON p.id = rp.permission_id
            JOIN role r ON rp.role_id = r.id
            JOIN user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.deleted = 0
              AND r.deleted = 0
              AND r.enabled = 1
              AND rp.deleted = 0
              AND p.deleted = 0
              AND p.enabled = 1
            """)
    List<String> selectPermissionCodesByUserId(Long userId);

    @Select("""
            SELECT DISTINCT r.role_code
            FROM role r
            JOIN user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.deleted = 0
              AND r.deleted = 0
              AND r.enabled = 1
            """)
    List<String> selectRoleCodesByUserId(Long userId);
}
11. CustomUserDetails 改造
11.1 当前问题

当前 CustomUserDetails 可能只保存：

userId
username
password
role

并且权限可能是：

List.of(new SimpleGrantedAuthority("ROLE_" + role))

RBAC 改造后，需要同时保存：

roles
permissions
authorities
11.2 改造后的 CustomUserDetails

路径：

src/main/java/com/internpilot/security/CustomUserDetails.java
package com.internpilot.security;

import com.internpilot.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;

    private final String username;

    private final String password;

    private final Boolean enabled;

    private final List<String> roles;

    private final List<String> permissions;

    public CustomUserDetails(
            User user,
            List<String> roles,
            List<String> permissions
    ) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = user.getEnabled() != null && user.getEnabled() == 1;
        this.roles = roles == null ? List.of() : roles;
        this.permissions = permissions == null ? List.of() : permissions;
    }

    public CustomUserDetails(
            Long userId,
            String username,
            List<String> roles,
            List<String> permissions
    ) {
        this.userId = userId;
        this.username = username;
        this.password = null;
        this.enabled = true;
        this.roles = roles == null ? List.of() : roles;
        this.permissions = permissions == null ? List.of() : permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        return authorities;
    }

    public String getPrimaryRole() {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        return roles.get(0);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
12. CustomUserDetailsService 改造
12.1 改造目标

登录时需要：

根据用户名查询用户；
根据 userId 查询角色；
根据 userId 查询权限；
构造 CustomUserDetails。
12.2 改造代码

路径：

src/main/java/com/internpilot/security/CustomUserDetailsService.java
package com.internpilot.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.User;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<String> roles = permissionMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(user.getId());

        if (roles == null || roles.isEmpty()) {
            roles = List.of("USER");
        }

        return new CustomUserDetails(user, roles, permissions);
    }
}
13. JWT Token 设计调整
13.1 是否在 JWT 中保存权限

不建议在 JWT 中保存完整权限列表。

原因：

权限可能很多，Token 会变大；
权限变更后旧 Token 仍然保留旧权限；
不方便动态权限控制；
安全性较差。

推荐 JWT 中只保存：

userId
username
primaryRole

每次请求时，后端根据 userId 查询最新角色权限。

13.2 当前 MVP 可接受方案

为了减少数据库查询，也可以在 JWT 中保存 roles 和 permissions。

但本项目推荐：

JWT 保存身份信息
数据库加载最新权限

这更适合面试讲。

14. JwtAuthenticationFilter 改造
14.1 当前问题

当前过滤器可能从 Token 中解析：

userId
username
role

然后直接构造：

new CustomUserDetails(userId, username, role)

这会绕过 RBAC 数据库权限。

14.2 推荐改造方案

在过滤器中解析 username 后，调用 CustomUserDetailsService 加载完整权限。

14.3 改造代码

路径：

src/main/java/com/internpilot/security/JwtAuthenticationFilter.java

核心逻辑：

package com.internpilot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)
                && jwtTokenProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String username = jwtTokenProvider.getUsername(token);

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
15. AuthService 注册逻辑改造
15.1 注册时需要分配默认角色

用户注册成功后，需要自动插入：

user_role

也就是给用户分配 USER 角色。

15.2 AuthServiceImpl 改造思路

注册流程：

校验用户名
  ↓
保存 user
  ↓
查询 USER 角色
  ↓
插入 user_role
  ↓
返回注册结果
15.3 示例代码片段

在 AuthServiceImpl.register() 中 userMapper.insert(user) 之后加入：

Role userRole = roleMapper.selectOne(
        new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, UserRoleEnum.USER.getCode())
                .eq(Role::getDeleted, 0)
                .last("LIMIT 1")
);

if (userRole == null) {
    throw new BusinessException("系统默认角色不存在");
}

UserRole relation = new UserRole();
relation.setUserId(user.getId());
relation.setRoleId(userRole.getId());
userRoleMapper.insert(relation);
15.4 需要注入
private final RoleMapper roleMapper;

private final UserRoleMapper userRoleMapper;
16. LoginResponse 改造
16.1 当前响应

当前可能返回：

{
  "token": "...",
  "user": {
    "userId": 1,
    "username": "wan",
    "role": "USER"
  }
}

RBAC 后建议返回：

{
  "token": "...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "userId": 1,
    "username": "wan",
    "roles": ["USER"],
    "permissions": [
      "resume:read",
      "resume:write",
      "job:read"
    ]
  }
}
16.2 AuthUserResponse 改造
package com.internpilot.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "认证用户信息")
public class AuthUserResponse {

    private Long userId;

    private String username;

    private String email;

    private String school;

    private String major;

    private String grade;

    private String role;

    private List<String> roles;

    private List<String> permissions;
}
17. SecurityConfig 改造
17.1 开启方法级权限

确保有：

@EnableMethodSecurity

示例：

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
}
17.2 接口权限配置

SecurityConfig 中可以保留基础路径控制：

.authorizeHttpRequests(auth -> auth
        .requestMatchers(WHITE_LIST).permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
)

具体业务权限建议交给方法注解。

18. Controller 权限注解设计
18.1 简历模块
@PreAuthorize("hasAuthority('resume:write')")
@PostMapping("/upload")
public Result<ResumeUploadResponse> upload(...) {
    return Result.success(resumeService.upload(file, resumeName));
}

@PreAuthorize("hasAuthority('resume:read')")
@GetMapping
public Result<PageResult<ResumeListResponse>> list(...) {
    return Result.success(resumeService.list(pageNum, pageSize));
}

@PreAuthorize("hasAuthority('resume:delete')")
@DeleteMapping("/{id}")
public Result<Boolean> delete(@PathVariable Long id) {
    return Result.success(resumeService.delete(id));
}
18.2 岗位模块
@PreAuthorize("hasAuthority('job:write')")
@PostMapping
public Result<JobCreateResponse> create(@RequestBody @Valid JobCreateRequest request) {
    return Result.success(jobService.create(request));
}

@PreAuthorize("hasAuthority('job:read')")
@GetMapping
public Result<PageResult<JobListResponse>> list(...) {
    return Result.success(jobService.list(keyword, jobType, location, pageNum, pageSize));
}

@PreAuthorize("hasAuthority('job:delete')")
@DeleteMapping("/{id}")
public Result<Boolean> delete(@PathVariable Long id) {
    return Result.success(jobService.delete(id));
}
18.3 AI 分析模块
@PreAuthorize("hasAuthority('analysis:write')")
@PostMapping("/match")
public Result<AnalysisResultResponse> match(@RequestBody @Valid AnalysisMatchRequest request) {
    return Result.success(analysisService.match(request));
}

@PreAuthorize("hasAuthority('analysis:read')")
@GetMapping("/reports")
public Result<PageResult<AnalysisReportListResponse>> listReports(...) {
    return Result.success(analysisService.listReports(resumeId, jobId, minScore, pageNum, pageSize));
}
18.4 投递记录模块
@PreAuthorize("hasAuthority('application:write')")
@PostMapping
public Result<ApplicationCreateResponse> create(@RequestBody @Valid ApplicationCreateRequest request) {
    return Result.success(applicationService.create(request));
}

@PreAuthorize("hasAuthority('application:read')")
@GetMapping
public Result<PageResult<ApplicationListResponse>> list(...) {
    return Result.success(applicationService.list(status, keyword, pageNum, pageSize));
}

@PreAuthorize("hasAuthority('application:delete')")
@DeleteMapping("/{id}")
public Result<Boolean> delete(@PathVariable Long id) {
    return Result.success(applicationService.delete(id));
}
19. 管理员接口设计

RBAC 系统需要为后续管理员后台预留接口。

第一阶段可以先只设计，不急着全部实现。

19.1 用户管理接口
GET    /api/admin/users
GET    /api/admin/users/{id}
PUT    /api/admin/users/{id}/disable
PUT    /api/admin/users/{id}/enable
PUT    /api/admin/users/{id}/roles

权限：

@PreAuthorize("hasAuthority('admin:user:read')")
@PreAuthorize("hasAuthority('admin:user:write')")
@PreAuthorize("hasAuthority('admin:user:disable')")
19.2 角色管理接口
GET    /api/admin/roles
POST   /api/admin/roles
GET    /api/admin/roles/{id}
PUT    /api/admin/roles/{id}
DELETE /api/admin/roles/{id}
PUT    /api/admin/roles/{id}/permissions

权限：

@PreAuthorize("hasAuthority('admin:role:read')")
@PreAuthorize("hasAuthority('admin:role:write')")
19.3 权限管理接口
GET /api/admin/permissions

权限：

@PreAuthorize("hasAuthority('admin:permission:read')")
20. DTO / VO 设计
20.1 RoleResponse
package com.internpilot.vo.admin;

import lombok.Data;

@Data
public class RoleResponse {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private String description;

    private Boolean enabled;
}
20.2 PermissionResponse
package com.internpilot.vo.admin;

import lombok.Data;

@Data
public class PermissionResponse {

    private Long permissionId;

    private String permissionCode;

    private String permissionName;

    private String resourceType;

    private String description;

    private Boolean enabled;
}
20.3 UserRoleUpdateRequest
package com.internpilot.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleUpdateRequest {

    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
20.4 RolePermissionUpdateRequest
package com.internpilot.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionUpdateRequest {

    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}
21. AdminPermissionService 设计
21.1 Service 接口
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
21.2 实现思路
updateUserRoles
校验当前用户有 admin:user:write
  ↓
校验 userId 存在
  ↓
逻辑删除旧 user_role
  ↓
插入新 user_role
updateRolePermissions
校验当前用户有 admin:role:write
  ↓
校验 roleId 存在
  ↓
逻辑删除旧 role_permission
  ↓
插入新 role_permission
22. 权限缓存设计
22.1 是否需要缓存权限

第一阶段可以不缓存权限。

每次请求时通过 CustomUserDetailsService 查询角色权限。

优点：

权限变更实时生效；
逻辑简单；
不用考虑缓存一致性。

缺点：

每次请求都会查数据库；
请求量大时性能一般。
22.2 后续 Redis 缓存方案

后续可以缓存用户权限：

internpilot:auth:permissions:{userId}

缓存内容：

{
  "roles": ["USER"],
  "permissions": ["resume:read", "resume:write"]
}

过期时间：

30 分钟

当管理员修改用户角色或角色权限时，主动删除相关缓存。

23. 前端权限控制设计
23.1 登录后保存权限

登录成功后前端保存：

token
roles
permissions

Pinia 中：

state: () => ({
  token: '',
  user: null,
  roles: [],
  permissions: []
})
23.2 前端权限判断工具
export function hasPermission(permission: string) {
  const authStore = useAuthStore()
  return authStore.permissions.includes(permission)
}

export function hasRole(role: string) {
  const authStore = useAuthStore()
  return authStore.roles.includes(role)
}
23.3 菜单权限控制

例如管理员菜单：

{
  title: '用户管理',
  path: '/admin/users',
  permission: 'admin:user:read'
}

渲染时过滤：

menus.filter(menu => !menu.permission || hasPermission(menu.permission))
23.4 按钮权限控制

例如删除简历按钮：

<el-button
  v-if="hasPermission('resume:delete')"
  type="danger"
>
  删除
</el-button>
24. 测试流程
24.1 初始化数据测试

执行 SQL 后检查：

SELECT * FROM role;
SELECT * FROM permission;
SELECT * FROM user_role;
SELECT * FROM role_permission;

期望：

role 中有 USER / ADMIN
permission 中有权限数据
USER 角色有关联权限
ADMIN 角色拥有全部权限
24.2 注册用户测试

注册新用户：

newuser

检查：

SELECT *
FROM user_role ur
JOIN role r ON ur.role_id = r.id
WHERE ur.user_id = 新用户ID;

期望：

新用户自动拥有 USER 角色
24.3 登录测试

登录后响应应包含：

{
  "roles": ["USER"],
  "permissions": [
    "resume:read",
    "resume:write"
  ]
}
24.4 普通用户访问简历接口

普通用户访问：

GET /api/resumes

期望：

200 成功
24.5 普通用户访问管理员接口

普通用户访问：

GET /api/admin/users

期望：

403 Forbidden
24.6 管理员访问管理员接口

管理员登录后访问：

GET /api/admin/users

期望：

200 成功
24.7 移除权限测试

从 USER 角色移除：

resume:delete

然后普通用户访问：

DELETE /api/resumes/{id}

期望：

403 Forbidden
25. PowerShell 测试示例
25.1 登录
$body = @{
  username = "wan"
  password = "123456"
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

$token = $response.data.token
$headers = @{ Authorization = "Bearer $token" }

$response.data.user.roles
$response.data.user.permissions
25.2 访问普通接口
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/resumes" `
  -Method Get `
  -Headers $headers
25.3 访问管理员接口
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/users" `
  -Method Get `
  -Headers $headers

普通用户期望返回：

403
26. 常见问题
26.1 登录后权限为空

可能原因：

user_role 没有数据；
role_permission 没有数据；
role.enabled = 0；
permission.enabled = 0；
deleted 字段为 1；
SQL 查询条件错误。

检查：

SELECT u.id, u.username, r.role_code, p.permission_code
FROM user u
JOIN user_role ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permission rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE u.username = 'wan';
26.2 hasRole 不生效

检查角色是否加了 ROLE_ 前缀。

Spring Security 中：

hasRole("ADMIN")

实际检查的是：

ROLE_ADMIN

所以 GrantedAuthority 中要加入：

new SimpleGrantedAuthority("ROLE_" + roleCode)

数据库中保存：

ADMIN
USER

不要保存：

ROLE_ADMIN
ROLE_USER

否则会出现：

ROLE_ROLE_ADMIN
26.3 hasAuthority 不生效

检查 getAuthorities() 是否加入了权限：

new SimpleGrantedAuthority("resume:read")

权限字符串必须和注解完全一致：

@PreAuthorize("hasAuthority('resume:read')")
26.4 注册用户后没有默认角色

检查 AuthServiceImpl.register() 是否插入了 user_role。

26.5 修改权限后没有立即生效

如果每次请求都查数据库，应该立即生效。

如果你后续加了 Redis 权限缓存，需要删除：

internpilot:auth:permissions:{userId}
27. 开发顺序建议

推荐按以下顺序开发：

1. 创建 role 表；
2. 创建 permission 表；
3. 创建 user_role 表；
4. 创建 role_permission 表；
5. 初始化 USER / ADMIN 角色；
6. 初始化权限数据；
7. 给 USER / ADMIN 分配权限；
8. 创建 Role Entity；
9. 创建 Permission Entity；
10. 创建 UserRole Entity；
11. 创建 RolePermission Entity；
12. 创建对应 Mapper；
13. PermissionMapper 增加查询角色权限方法；
14. 改造 CustomUserDetails；
15. 改造 CustomUserDetailsService；
16. 改造 JwtAuthenticationFilter；
17. 改造注册逻辑，默认分配 USER 角色；
18. 改造 LoginResponse 返回 roles / permissions；
19. 给 Controller 增加 @PreAuthorize；
20. 测试普通用户权限；
21. 测试管理员权限；
22. 测试移除权限后的 403。
28. 验收标准
28.1 数据库验收
 role 表存在；
 permission 表存在；
 user_role 表存在；
 role_permission 表存在；
 USER 角色存在；
 ADMIN 角色存在；
 权限数据存在；
 USER 角色拥有普通业务权限；
 ADMIN 角色拥有全部权限。
28.2 登录验收
 用户登录成功；
 登录响应包含 roles；
 登录响应包含 permissions；
 新注册用户自动拥有 USER 角色；
 管理员拥有 ADMIN 角色。
28.3 接口权限验收
 有 resume:read 可以访问简历列表；
 没有 resume:delete 不能删除简历；
 普通用户不能访问 /api/admin/**；
 管理员可以访问 /api/admin/**；
 权限不足返回 403；
 未登录返回 401。
28.4 前端验收
 登录后前端保存 roles；
 登录后前端保存 permissions；
 无权限菜单不显示；
 无权限按钮不显示；
 Token 失效跳转登录；
 403 显示无权限提示。
29. 面试讲解准备
29.1 面试官问：你项目的权限系统怎么设计？

回答：

项目第一阶段只有 USER 和 ADMIN 两种简单角色，后续我把它升级成了 RBAC 权限模型。

RBAC 中用户不直接绑定权限，而是用户绑定角色，角色绑定权限。数据库中设计了 role、permission、user_role、role_permission 四张表。

登录时根据用户 ID 查询角色和权限，并封装到 CustomUserDetails 中。Spring Security 通过 GrantedAuthority 识别角色和权限，接口上使用 @PreAuthorize 做权限控制，比如 hasAuthority('resume:read')。
29.2 面试官问：hasRole 和 hasAuthority 有什么区别？

回答：

hasRole 用于判断角色，Spring Security 会自动加 ROLE_ 前缀。比如 hasRole('ADMIN') 实际判断的是 ROLE_ADMIN。

hasAuthority 判断的是完整权限字符串，不会自动加前缀。比如 hasAuthority('resume:read') 就要求当前用户的 GrantedAuthority 中存在 resume:read。

所以我在 CustomUserDetails 中同时加入了 ROLE_ADMIN 这种角色权限，以及 resume:read 这种业务权限。
29.3 面试官问：为什么不把权限直接放 JWT 里？

回答：

因为权限可能会变化。如果把权限直接放在 JWT 里，管理员修改用户权限后，旧 Token 在过期前仍然携带旧权限，权限不能立即生效。

所以我更推荐 JWT 只保存 userId 和 username，每次请求时通过 userId 加载最新角色权限。这样权限变更可以更快生效。后续如果性能有压力，可以用 Redis 缓存用户权限，并在权限变更时删除缓存。
29.4 面试官问：RBAC 和数据权限有什么区别？

回答：

RBAC 解决的是用户有没有某个操作权限，比如有没有 resume:read 权限。

数据权限解决的是用户能不能访问某一条具体数据。比如用户虽然有 resume:read 权限，但也只能查看自己的简历，不能查看别人的简历。

所以我的项目中既使用 @PreAuthorize 控制接口权限，也在业务查询中加 userId 条件防止水平越权。
30. 后续扩展方向

RBAC 完成后，可以继续扩展：

1. 管理员后台；
2. 用户管理；
3. 角色管理；
4. 权限管理；
5. 菜单权限；
6. 按钮权限；
7. 操作日志；
8. 数据权限规则；
9. Redis 权限缓存；
10. 权限变更实时失效。
31. 模块设计结论

RBAC 权限系统是 InternPilot 从 MVP 项目升级为工程化后端项目的重要一步。

它将当前简单的：

user.role = USER / ADMIN

升级为：

user
  ↓
user_role
  ↓
role
  ↓
role_permission
  ↓
permission

完成后，项目将具备：

多角色支持
细粒度权限控制
管理员后台基础
菜单权限基础
按钮权限基础
更好的面试表达价值

推荐先完成 RBAC，再继续做：

WebSocket AI 分析进度实时展示
系统操作日志
管理员后台
AI 面试题生成
