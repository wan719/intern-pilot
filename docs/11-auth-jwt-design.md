# InternPilot 用户认证与 JWT 鉴权设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的用户认证与 JWT 鉴权设计，包括认证模块目标、用户注册流程、用户登录流程、JWT Token 生成与解析、Spring Security 配置、过滤器设计、用户上下文设计、401/403 异常处理、接口设计和测试流程。

InternPilot 是一个前后端分离项目，后端采用 Spring Boot 3 + Spring Security + JWT 实现无状态认证。

---

## 2. 认证模块目标

用户认证与鉴权模块需要完成以下目标：

1. 支持用户注册；
2. 支持用户登录；
3. 用户密码加密存储；
4. 登录成功后签发 JWT Token；
5. 前端携带 Token 访问后端接口；
6. 后端解析 Token 并识别当前用户；
7. 未登录访问业务接口返回 401；
8. 权限不足访问接口返回 403；
9. 用户只能访问自己的业务数据；
10. 为后续管理员权限扩展做好准备。

---

## 3. 认证方式选择

### 3.1 传统 Session 认证

传统 Web 项目常使用 Session 认证：

```text
用户登录
  ↓
服务端创建 Session
  ↓
浏览器保存 Cookie
  ↓
后续请求自动携带 Cookie
  ↓
服务端根据 Session 判断登录状态
```

缺点：

1. 服务端需要保存 Session；
2. 前后端分离时跨域 Cookie 配置较麻烦；
3. 不利于后续移动端或多端接入；
4. 分布式部署时需要共享 Session。

### 3.2 JWT 无状态认证

InternPilot 采用 JWT 认证：

```text
用户登录
  ↓
服务端生成 JWT Token
  ↓
前端保存 Token
  ↓
请求接口时携带 Authorization Header
  ↓
后端解析 Token
  ↓
识别当前用户身份
```

优点：

1. 服务端不需要保存登录状态；
2. 适合前后端分离；
3. 适合 RESTful API；
4. 易于扩展到移动端；
5. 和 Spring Security 集成方便。

---

## 4. 总体认证流程

### 4.1 注册流程

```text
用户提交注册信息
  ↓
AuthController 接收请求
  ↓
参数校验
  ↓
AuthService 判断用户名是否重复
  ↓
校验两次密码是否一致
  ↓
BCrypt 加密密码
  ↓
保存用户信息
  ↓
返回注册成功
```

### 4.2 登录流程

```text
用户提交用户名和密码
  ↓
AuthController 接收请求
  ↓
AuthService 查询用户
  ↓
判断用户是否存在
  ↓
判断用户是否启用
  ↓
BCrypt 校验密码
  ↓
生成 JWT Token
  ↓
返回 Token 和用户基础信息
```

### 4.3 鉴权流程

```text
用户访问业务接口
  ↓
请求头携带 Authorization: Bearer Token
  ↓
JwtAuthenticationFilter 拦截请求
  ↓
从请求头中提取 Token
  ↓
JwtTokenProvider 校验 Token
  ↓
解析 userId、username、role
  ↓
构造 CustomUserDetails
  ↓
构造 Authentication
  ↓
写入 SecurityContext
  ↓
请求进入 Controller
```

---

## 5. 数据库设计依赖

认证模块主要依赖 `user` 表。

### 5.1 user 表核心字段

```sql
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    school VARCHAR(100) DEFAULT NULL COMMENT '学校',
    major VARCHAR(100) DEFAULT NULL COMMENT '专业',
    grade VARCHAR(30) DEFAULT NULL COMMENT '年级',
    role VARCHAR(30) NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_email (email),
    KEY idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### 5.2 角色设计

第一阶段使用简单角色字段：

| 角色 | 说明 |
|---|---|
| USER | 普通用户 |
| ADMIN | 系统管理员 |

第一阶段不引入完整 RBAC，避免项目复杂度过高。

后续如果需要扩展权限系统，可以增加：

```text
role
permission
user_role
role_permission
```

---

## 6. 接口设计

### 6.1 用户注册接口

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/auth/register |
| Method | POST |
| 权限 | 无需登录 |
| Content-Type | application/json |

请求示例：

```json
{
  "username": "wan",
  "password": "123456",
  "confirmPassword": "123456",
  "email": "wan@example.com",
  "school": "西南大学",
  "major": "软件工程",
  "grade": "大二"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "wan",
    "email": "wan@example.com",
    "school": "西南大学",
    "major": "软件工程",
    "grade": "大二",
    "role": "USER"
  }
}
```

### 6.2 用户登录接口

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/auth/login |
| Method | POST |
| 权限 | 无需登录 |
| Content-Type | application/json |

请求示例：

```json
{
  "username": "wan",
  "password": "123456"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx.xxx",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": 1,
      "username": "wan",
      "role": "USER"
    }
  }
}
```

### 6.3 当前用户接口

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/user/me |
| Method | GET |
| 权限 | USER |
| Header | Authorization: Bearer Token |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "wan",
    "email": "wan@example.com",
    "school": "西南大学",
    "major": "软件工程",
    "grade": "大二",
    "role": "USER"
  }
}
```

---

## 7. DTO 与 VO 设计

### 7.1 RegisterRequest

路径：

```text
src/main/java/com/internpilot/dto/auth/RegisterRequest.java
```

```java
package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "用户名", example = "wan")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 位之间")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度必须在 6 到 30 位之间")
    private String password;

    @Schema(description = "确认密码", example = "123456")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "邮箱", example = "wan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "学校", example = "西南大学")
    private String school;

    @Schema(description = "专业", example = "软件工程")
    private String major;

    @Schema(description = "年级", example = "大二")
    private String grade;
}
```

### 7.2 LoginRequest

路径：

```text
src/main/java/com/internpilot/dto/auth/LoginRequest.java
```

```java
package com.internpilot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "用户名", example = "wan")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

### 7.3 AuthUserResponse

路径：

```text
src/main/java/com/internpilot/vo/auth/AuthUserResponse.java
```

```java
package com.internpilot.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "认证用户信息")
public class AuthUserResponse {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "wan")
    private String username;

    @Schema(description = "邮箱", example = "wan@example.com")
    private String email;

    @Schema(description = "学校", example = "西南大学")
    private String school;

    @Schema(description = "专业", example = "软件工程")
    private String major;

    @Schema(description = "年级", example = "大二")
    private String grade;

    @Schema(description = "角色", example = "USER")
    private String role;
}
```

### 7.4 LoginResponse

路径：

```text
src/main/java/com/internpilot/vo/auth/LoginResponse.java
```

```java
package com.internpilot.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "过期时间，单位秒", example = "86400")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private AuthUserResponse user;
}
```

---

## 8. Entity 与 Mapper 设计

### 8.1 User Entity

路径：

```text
src/main/java/com/internpilot/entity/User.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String email;

    private String phone;

    private String realName;

    private String school;

    private String major;

    private String grade;

    private String role;

    private Integer enabled;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

### 8.2 UserMapper

路径：

```text
src/main/java/com/internpilot/mapper/UserMapper.java
```

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

---

## 9. 枚举设计

### 9.1 UserRoleEnum

路径：

```text
src/main/java/com/internpilot/enums/UserRoleEnum.java
```

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("USER", "普通用户"),
    ADMIN("ADMIN", "系统管理员");

    private final String code;
    private final String description;

    UserRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

---

## 10. JWT 配置设计

### 10.1 application-dev.yml 配置

```yaml
jwt:
  secret: intern-pilot-dev-secret-key-change-this-to-a-long-random-string
  expiration: 86400000
```

字段说明：

| 字段 | 说明 |
|---|---|
| secret | JWT 签名密钥 |
| expiration | 过期时间，单位毫秒 |

### 10.2 JwtProperties

路径：

```text
src/main/java/com/internpilot/config/JwtProperties.java
```

```java
package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private Long expiration;
}
```

---

## 11. JWT 工具类设计

### 11.1 JwtTokenProvider

路径：

```text
src/main/java/com/internpilot/security/JwtTokenProvider.java
```

```java
package com.internpilot.security;

import com.internpilot.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        if (userId instanceof Long) {
            return (Long) userId;
        }
        return Long.valueOf(userId.toString());
    }

    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        return claims.get("username", String.class);
    }

    public String getRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    public Long getExpirationSeconds() {
        return jwtProperties.getExpiration() / 1000;
    }
}
```

### 11.2 密钥长度注意事项

如果使用 HS256，`secret` 长度不能太短。

建议至少 32 个字符以上，否则可能出现：

```text
The signing key's size is too small
```

---

## 12. CustomUserDetails 设计

### 12.1 设计目的

Spring Security 需要通过 UserDetails 表示当前登录用户。

自定义 `CustomUserDetails`，用于保存：

1. `userId`；
2. `username`；
3. `password`；
4. `role`；
5. `enabled`。

### 12.2 CustomUserDetails

路径：

```text
src/main/java/com/internpilot/security/CustomUserDetails.java
```

```java
package com.internpilot.security;

import com.internpilot.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;

    private final String username;

    private final String password;

    private final String role;

    private final Boolean enabled;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.enabled = user.getEnabled() != null && user.getEnabled() == 1;
    }

    public CustomUserDetails(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.password = null;
        this.role = role;
        this.enabled = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
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
```

### 12.3 角色前缀说明

Spring Security 中：

```java
hasRole("ADMIN")
```

实际会检查：

```text
ROLE_ADMIN
```

所以数据库中建议保存：

```text
USER
ADMIN
```

而不是：

```text
ROLE_USER
ROLE_ADMIN
```

这样可以避免出现：

```text
ROLE_ROLE_ADMIN
```

---

## 13. CustomUserDetailsService 设计

### 13.1 设计目的

`CustomUserDetailsService` 用于根据用户名从数据库加载用户。

### 13.2 CustomUserDetailsService

路径：

```text
src/main/java/com/internpilot/security/CustomUserDetailsService.java
```

```java
package com.internpilot.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.User;
import com.internpilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

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

        return new CustomUserDetails(user);
    }
}
```

---

## 14. JWT 过滤器设计

### 14.1 过滤器职责

`JwtAuthenticationFilter` 负责：

1. 从请求头读取 Authorization；
2. 判断是否是 Bearer Token；
3. 提取 Token；
4. 校验 Token；
5. 解析用户信息；
6. 构造 Authentication；
7. 写入 SecurityContext。

### 14.2 JwtAuthenticationFilter

路径：

```text
src/main/java/com/internpilot/security/JwtAuthenticationFilter.java
```

```java
package com.internpilot.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);

            Long userId = getLongClaim(claims, "userId");
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            CustomUserDetails userDetails = new CustomUserDetails(userId, username, role);

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

    private Long getLongClaim(Claims claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        return Long.valueOf(value.toString());
    }
}
```

### 14.3 为什么继承 OncePerRequestFilter

`OncePerRequestFilter` 可以保证一次请求只执行一次过滤器逻辑，适合 JWT 认证过滤器。

---

## 15. 401 与 403 处理设计

### 15.1 401 和 403 区别

| 状态码 | 含义 | 场景 |
|---|---|---|
| 401 | 未认证 | 没登录、Token 无效、Token 过期 |
| 403 | 无权限 | 已登录，但访问了没有权限的接口 |

### 15.2 JwtAuthenticationEntryPoint

用于处理 401。

路径：

```text
src/main/java/com/internpilot/security/JwtAuthenticationEntryPoint.java
```

```java
package com.internpilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.common.Result;
import com.internpilot.common.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED, "请先登录或 Token 已失效");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
```

### 15.3 JwtAccessDeniedHandler

用于处理 403。

路径：

```text
src/main/java/com/internpilot/security/JwtAccessDeniedHandler.java
```

```java
package com.internpilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.common.Result;
import com.internpilot.common.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Result<Void> result = Result.fail(ResultCode.FORBIDDEN, "无权限访问该资源");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
```

---

## 16. Spring Security 配置设计

### 16.1 SecurityConfig 职责

`SecurityConfig` 负责：

1. 配置接口放行规则；
2. 禁用 CSRF；
3. 设置无状态 Session；
4. 注册 JWT 过滤器；
5. 配置 401/403 处理器；
6. 提供 PasswordEncoder；
7. 开启方法级权限控制。

### 16.2 SecurityConfig

路径：

```text
src/main/java/com/internpilot/config/SecurityConfig.java
```

```java
package com.internpilot.config;

import com.internpilot.security.JwtAccessDeniedHandler;
import com.internpilot.security.JwtAuthenticationEntryPoint;
import com.internpilot.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String[] WHITE_LIST = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/health",

            "/doc.html",
            "/webjars/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离项目不使用 CSRF
                .csrf(csrf -> csrf.disable())

                // 不使用 Session，JWT 无状态认证
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置异常处理
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // 配置接口权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 如果后续使用 AuthenticationManager，可保留该 Bean
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
```

### 16.3 为什么禁用 CSRF

CSRF 主要防护基于 Cookie 的浏览器 Session 攻击。

InternPilot 是前后端分离项目，使用 JWT Header 认证，不依赖 Cookie 保存登录状态，因此可以禁用 CSRF。

### 16.4 为什么使用 STATELESS

JWT 是无状态认证，服务端不保存 Session。

所以配置：

```java
SessionCreationPolicy.STATELESS
```

表示 Spring Security 不创建、不使用 Session。

---

## 17. 当前用户工具类设计

### 17.1 SecurityUtils

路径：

```text
src/main/java/com/internpilot/util/SecurityUtils.java
```

```java
package com.internpilot.util;

import com.internpilot.exception.BusinessException;
import com.internpilot.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessException("当前用户未登录");
        }

        return (CustomUserDetails) authentication.getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    public static String getCurrentUserRole() {
        return getCurrentUser().getRole();
    }
}
```

### 17.2 使用示例

在业务层中获取当前用户 ID：

```java
Long currentUserId = SecurityUtils.getCurrentUserId();
```

查询简历时必须带上：

```java
.eq(Resume::getUserId, currentUserId)
```

这样可以防止越权访问。

---

## 18. AuthService 设计

### 18.1 AuthService 接口

路径：

```text
src/main/java/com/internpilot/service/AuthService.java
```

```java
package com.internpilot.service;

import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;

public interface AuthService {

    AuthUserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
```

### 18.2 AuthServiceImpl

路径：

```text
src/main/java/com/internpilot/service/impl/AuthServiceImpl.java
```

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.entity.User;
import com.internpilot.enums.UserRoleEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.UserMapper;
import com.internpilot.security.JwtTokenProvider;
import com.internpilot.service.AuthService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthUserResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );

        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setSchool(request.getSchool());
        user.setMajor(request.getMajor());
        user.setGrade(request.getGrade());
        user.setRole(UserRoleEnum.USER.getCode());
        user.setEnabled(1);

        userMapper.insert(user);

        return toAuthUserResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .eq(User::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BusinessException("当前用户已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenProvider.getExpirationSeconds());
        response.setUser(toAuthUserResponse(user));

        return response;
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
        return response;
    }
}
```

---

## 19. AuthController 设计

### 19.1 AuthController

路径：

```text
src/main/java/com/internpilot/controller/AuthController.java
```

```java
package com.internpilot.controller;

import com.internpilot.common.Result;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.service.AuthService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册", description = "用户通过用户名、密码、邮箱等信息注册账号")
    @PostMapping("/register")
    public Result<AuthUserResponse> register(@RequestBody @Valid RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统，登录成功后返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
```

---

## 20. UserService 与 UserController 设计

### 20.1 UserService

路径：

```text
src/main/java/com/internpilot/service/UserService.java
```

```java
package com.internpilot.service;

import com.internpilot.vo.auth.AuthUserResponse;

public interface UserService {

    AuthUserResponse getCurrentUserInfo();
}
```

### 20.2 UserServiceImpl

路径：

```text
src/main/java/com/internpilot/service/impl/UserServiceImpl.java
```

```java
package com.internpilot.service.impl;

import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.UserMapper;
import com.internpilot.service.UserService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.auth.AuthUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public AuthUserResponse getCurrentUserInfo() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        User user = userMapper.selectById(currentUserId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }

        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setSchool(user.getSchool());
        response.setMajor(user.getMajor());
        response.setGrade(user.getGrade());
        response.setRole(user.getRole());

        return response;
    }
}
```

### 20.3 UserController

路径：

```text
src/main/java/com/internpilot/controller/UserController.java
```

```java
package com.internpilot.controller;

import com.internpilot.common.Result;
import com.internpilot.service.UserService;
import com.internpilot.vo.auth.AuthUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户信息接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "查询当前用户信息", description = "根据 JWT Token 查询当前登录用户信息")
    @GetMapping("/me")
    public Result<AuthUserResponse> getCurrentUserInfo() {
        return Result.success(userService.getCurrentUserInfo());
    }
}
```

---

## 21. 测试流程设计

### 21.1 测试顺序

认证模块开发完成后，按以下顺序测试：

1. 访问健康检查接口；
2. 注册用户；
3. 重复注册同一用户名；
4. 使用错误密码登录；
5. 使用正确密码登录；
6. 保存返回的 Token；
7. 不带 Token 访问 `/api/user/me`；
8. 携带错误 Token 访问 `/api/user/me`；
9. 携带正确 Token 访问 `/api/user/me`；
10. 访问管理员接口验证 403。

### 21.2 PowerShell 注册测试

```powershell
$body = @{
  username = "wan"
  password = "123456"
  confirmPassword = "123456"
  email = "wan@example.com"
  school = "西南大学"
  major = "软件工程"
  grade = "大二"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/auth/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

### 21.3 PowerShell 登录测试

```powershell
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
$token
```

### 21.4 查询当前用户

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/user/me" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 21.5 不带 Token 测试

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/user/me" `
  -Method Get
```

期望返回：

```json
{
  "code": 401,
  "message": "请先登录或 Token 已失效",
  "data": null
}
```

---

## 22. curl.exe 测试示例

### 22.1 登录

```powershell
curl.exe -X POST "http://localhost:8080/api/auth/login" `
  -H "Content-Type: application/json" `
  -d "{`"username`":`"wan`",`"password`":`"123456`"}"
```

### 22.2 携带 Token 查询用户

```powershell
curl.exe -X GET "http://localhost:8080/api/user/me" `
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 23. Swagger 测试流程

访问：

```text
http://localhost:8080/doc.html
```

测试步骤：

1. 打开用户认证接口；
2. 调用注册接口；
3. 调用登录接口；
4. 复制返回 Token；
5. 点击 Authorize；
6. 输入 Bearer Token；
7. 调用 `/api/user/me`；
8. 验证是否返回当前用户信息。

如果 Knife4j 的 Authorize 不需要手动写 Bearer，就只填 Token；如果无效，就填完整：

```text
Bearer eyJhbGciOiJIUzI1NiJ9.xxx.xxx
```

---

## 24. 常见问题与解决方案

### 24.1 登录成功后访问接口仍然 401

可能原因：

1. 请求头没有加 Authorization；
2. Token 前面没有 Bearer；
3. Bearer 后面少了空格；
4. Token 已过期；
5. `JwtAuthenticationFilter` 没有注册；
6. `SecurityConfig` 中过滤器顺序错误。

正确格式：

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx.xxx
```

### 24.2 访问管理员接口返回 403

这是正常情况。

如果接口配置：

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

而当前用户角色是：

```text
USER
```

就会返回 403。

### 24.3 出现 ROLE_ROLE_ADMIN

原因：

数据库中保存了：

```text
ROLE_ADMIN
```

但代码中又拼接了：

```java
"ROLE_" + role
```

最终变成：

```text
ROLE_ROLE_ADMIN
```

解决：

数据库中只保存：

```text
ADMIN
USER
```

代码负责加 `ROLE_` 前缀。

### 24.4 JWT 密钥太短

错误类似：

```text
The signing key's size is 低于安全要求
```

解决：把配置中的 `secret` 改长。

```yaml
jwt:
  secret: intern-pilot-dev-secret-key-change-this-to-a-long-random-string
```

### 24.5 JJWT API 报错

如果使用 jjwt 0.11.5，应该使用：

```java
Jwts.builder()
    .setSubject(username)
    .setIssuedAt(now)
    .setExpiration(expirationDate)
    .signWith(key)
    .compact();
```

解析使用：

```java
Jwts.parserBuilder()
    .setSigningKey(key)
    .build()
    .parseClaimsJws(token)
    .getBody();
```

不要混用不同版本的 API。

### 24.6 注册接口被 401 拦截

检查 `SecurityConfig` 是否放行：

```text
"/api/auth/register",
"/api/auth/login"
```

### 24.7 Swagger 页面被拦截

检查是否放行：

```text
"/doc.html",
"/webjars/**",
"/swagger-ui/**",
"/swagger-ui.html",
"/v3/api-docs/**"
```

---

## 25. 安全注意事项

### 25.1 密码安全

必须满足：

1. 不存明文密码；
2. 使用 BCrypt；
3. 登录失败不区分用户名不存在还是密码错误；
4. 返回用户信息时不返回 password。

### 25.2 Token 安全

必须满足：

1. Token 设置过期时间；
2. JWT secret 不提交真实生产密钥；
3. 日志中不打印完整 Token；
4. 前端建议存在 localStorage 或 memory 中；
5. 后续可加 Refresh Token。

### 25.3 数据权限安全

认证只解决“你是谁”，不解决“你能访问哪条数据”。

业务层必须校验：

```text
资源.user_id == 当前登录用户ID
```

例如：

```java
Long currentUserId = SecurityUtils.getCurrentUserId();

Resume resume = resumeMapper.selectOne(
    new LambdaQueryWrapper<Resume>()
        .eq(Resume::getId, resumeId)
        .eq(Resume::getUserId, currentUserId)
        .eq(Resume::getDeleted, 0)
);
```

这样才能防止用户访问他人的简历、岗位和报告。

---

## 26. 面试讲解准备

这个模块之后很适合面试讲。

### 26.1 面试官可能问：你的登录流程怎么实现？

回答思路：

用户登录时提交用户名和密码，后端先根据用户名查询用户，然后使用 BCrypt 校验密码。校验成功后，通过 `JwtTokenProvider` 生成 JWT，Token 中保存 `userId`、`username` 和 `role`。前端后续请求接口时在 Authorization 请求头中携带 Bearer Token。后端通过 `JwtAuthenticationFilter` 拦截请求，解析 Token，构造 Authentication，并放入 SecurityContext。这样 Controller 和 Service 就能获取当前登录用户。

### 26.2 面试官可能问：为什么用 JWT，不用 Session？

回答思路：

因为项目是前后端分离架构，JWT 更适合 RESTful API。JWT 是无状态的，服务端不需要保存 Session，后续扩展移动端或多实例部署也更方便。同时通过 Spring Security 过滤器解析 Token，可以和权限控制体系结合起来。

### 26.3 面试官可能问：401 和 403 的区别？

回答思路：

401 表示用户没有完成认证，比如没有登录、Token 无效或 Token 过期。403 表示用户已经登录，但是没有权限访问对应资源，比如普通用户访问管理员接口。项目中分别通过 AuthenticationEntryPoint 和 AccessDeniedHandler 统一返回 JSON 错误。

### 26.4 面试官可能问：如何防止用户访问别人的简历？

回答思路：

JWT 只能证明当前用户身份，不能自动保证数据权限。所以在业务查询时，所有简历、岗位、报告、投递记录都必须带上 userId 条件。例如查询简历详情时，不只根据 resumeId 查询，还会加上 userId = 当前登录用户ID。如果查不到，就返回资源不存在或无权限访问。

### 26.5 面试官可能问：Spring Security 过滤链怎么走？

回答思路：

请求进入后，先经过 Spring Security 的过滤器链。对于登录和注册接口，因为在 `SecurityConfig` 中配置了 `permitAll`，所以可以直接访问。对于业务接口，请求会经过 `JwtAuthenticationFilter`。过滤器会从 Authorization Header 中提取 Bearer Token，校验 Token 有效性，解析用户信息，然后构造 `UsernamePasswordAuthenticationToken` 放入 `SecurityContext`。后续授权过滤器会根据 `SecurityContext` 判断用户是否已认证，以及是否拥有对应角色权限。

---

## 27. 开发顺序建议

认证模块建议按以下顺序开发：

1. 创建 User 表；
2. 创建 User Entity；
3. 创建 UserMapper；
4. 创建 RegisterRequest、LoginRequest；
5. 创建 AuthUserResponse、LoginResponse；
6. 创建 Result、ResultCode、BusinessException；
7. 创建 AuthService 和 AuthServiceImpl；
8. 创建 AuthController；
9. 配置 PasswordEncoder；
10. 创建 JwtProperties；
11. 创建 JwtTokenProvider；
12. 创建 CustomUserDetails；
13. 创建 JwtAuthenticationFilter；
14. 创建 401/403 处理器；
15. 创建 SecurityConfig；
16. 创建 SecurityUtils；
17. 创建 UserController；
18. 测试注册；
19. 测试登录；
20. 测试携带 Token 访问 `/api/user/me`。

---

## 28. 验收标准

认证与 JWT 鉴权模块完成后，应满足以下标准：

### 28.1 注册验收

- [ ] 用户可以注册
- [ ] 用户名不能为空
- [ ] 密码不能为空
- [ ] 两次密码必须一致
- [ ] 用户名不能重复
- [ ] 密码加密存储
- [ ] 返回结果不包含密码

### 28.2 登录验收

- [ ] 正确用户名密码可以登录
- [ ] 错误密码不能登录
- [ ] 不存在用户不能登录
- [ ] 被禁用用户不能登录
- [ ] 登录成功返回 JWT
- [ ] 返回用户基础信息

### 28.3 鉴权验收

- [ ] 不带 Token 访问业务接口返回 401
- [ ] 错误 Token 访问业务接口返回 401
- [ ] 过期 Token 访问业务接口返回 401
- [ ] 正确 Token 可以访问业务接口
- [ ] 普通用户访问管理员接口返回 403
- [ ] Swagger 页面可以正常访问
- [ ] 注册和登录接口可以匿名访问

### 28.4 数据权限验收

- [ ] 能通过 `SecurityUtils` 获取当前用户 ID
- [ ] 后续业务接口可以基于 userId 做数据隔离
- [ ] 返回用户信息时不包含 password
- [ ] 日志中不打印密码和完整 Token

---

## 29. 模块设计结论

InternPilot 用户认证与 JWT 鉴权模块采用 Spring Security + JWT 实现前后端分离场景下的无状态认证。

核心流程是：

```text
注册时加密密码
  ↓
登录时校验密码
  ↓
生成 JWT
  ↓
前端携带 Token
  ↓
JwtAuthenticationFilter 解析 Token
  ↓
写入 SecurityContext
  ↓
业务接口获取当前用户
```

该设计能够满足 InternPilot 第一阶段用户登录、接口保护、角色权限和数据隔离需求，同时保留了后续扩展管理员权限、Refresh Token、Token 黑名单和完整 RBAC 权限体系的空间。
