# InternPilot 系统操作日志模块设计与实现文档

## 1. 文档说明

本文档用于描述 InternPilot 项目中系统操作日志模块的设计与实现方案，包括模块背景、功能目标、数据库设计、注解设计、AOP 切面设计、日志记录内容、接口设计、前端页面设计、权限控制、测试流程、异常处理和面试讲解准备。

系统操作日志模块主要用于记录用户在系统中的关键操作，例如：

```text
用户登录
上传简历
创建岗位 JD
发起 AI 分析
生成 AI 面试题
创建投递记录
修改投递状态
管理员禁用用户
管理员修改角色权限
```

该模块可以提升项目的工程化程度，为后续管理员后台、系统审计、问题排查和安全分析提供基础能力。

## 2. 为什么要做系统操作日志

当前 InternPilot 已经有：

用户认证
RBAC 权限系统
简历上传
岗位管理
AI 分析
AI 面试题生成
投递记录
WebSocket AI 进度

这些功能已经形成完整业务闭环，但还缺少一个重要工程能力：

系统发生了什么，谁操作了什么，什么时候操作的，是否成功，失败原因是什么。

例如：

谁上传了一份简历？
谁删除了某个岗位？
谁发起了一次 AI 分析？
谁修改了投递状态？
谁修改了用户角色？
某次接口报错时请求路径是什么？

这些问题都可以通过操作日志模块解决。

## 3. 模块目标

系统操作日志模块需要完成以下目标：

支持记录用户关键操作；
支持记录操作人 ID；
支持记录操作人用户名；
支持记录操作类型；
支持记录操作名称；
支持记录请求 URI；
支持记录请求方法；
支持记录请求 IP；
支持记录 User-Agent；
支持记录请求参数摘要；
支持记录操作是否成功；
支持记录错误信息；
支持记录操作耗时；
支持后台分页查询日志；
支持按操作类型筛选；
支持按用户名搜索；
支持按成功 / 失败筛选；
支持管理员查看日志；
支持后续扩展为审计日志和安全告警。

## 4. 日志记录范围

### 4.1 建议记录的操作

第一阶段建议记录这些操作：

认证模块
用户登录
用户注册
用户退出登录

简历模块
上传简历
删除简历
设置默认简历

岗位模块
创建岗位
修改岗位
删除岗位

AI 模块
发起 AI 匹配分析
创建 AI 分析任务
生成 AI 面试题
删除 AI 面试题报告

投递模块
创建投递记录
修改投递状态
修改投递备注
删除投递记录

管理员模块
禁用用户
启用用户
修改用户角色
修改角色权限
查看系统日志

### 4.2 不建议记录完整内容

不要记录完整敏感内容：

完整密码
完整 JWT Token
完整简历文本
完整岗位 JD
完整 AI API Key
完整 AI 原始响应
完整文件内容

日志中只保留摘要或 ID 即可。

## 5. 日志实现方案选择

### 5.1 方案一：在每个 Service 手动写日志

示例：

operationLogService.save("上传简历", userId);

优点：

简单直接；
每个地方都能自定义。

缺点：

侵入业务代码；
重复代码多；
容易漏记；
不够优雅。

### 5.2 方案二：注解 + AOP 自动记录

示例：

```java
@OperationLog(module = "简历管理", operation = "上传简历", type = "CREATE")
@PostMapping("/upload")
public Result<ResumeUploadResponse> upload(...) {
    ...
}
```

优点：

业务代码干净；
统一记录；
可复用；
面试好讲；
适合企业项目。

缺点：

需要理解 AOP；
对异常处理和参数脱敏要注意。

### 5.3 推荐方案

本项目推荐：

注解 + AOP + 数据库落库

理由：

工程化程度高
和 Spring Boot 生态匹配
适合管理员后台查看
适合面试展示 AOP 能力

## 6. 数据库设计

### 6.1 system_operation_log 表

```sql
CREATE TABLE IF NOT EXISTS system_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_username VARCHAR(100) DEFAULT NULL COMMENT '操作人用户名',
    module VARCHAR(100) NOT NULL COMMENT '操作模块',
    operation VARCHAR(100) NOT NULL COMMENT '操作名称',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
    request_method VARCHAR(20) DEFAULT NULL COMMENT '请求方法',
    request_params TEXT DEFAULT NULL COMMENT '请求参数摘要',
    ip_address VARCHAR(100) DEFAULT NULL COMMENT '请求IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：0失败，1成功',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    cost_time BIGINT DEFAULT NULL COMMENT '耗时，单位毫秒',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_sol_operator_id (operator_id),
    KEY idx_sol_operator_username (operator_username),
    KEY idx_sol_module (module),
    KEY idx_sol_operation_type (operation_type),
    KEY idx_sol_success (success),
    KEY idx_sol_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';
```

### 6.2 字段说明

字段	说明
id	日志 ID
operator_id	操作用户 ID
operator_username	操作用户名
module	操作模块
operation	操作名称
operation_type	操作类型
request_uri	请求路径
request_method	请求方法
request_params	请求参数摘要
ip_address	请求 IP
user_agent	浏览器 / 客户端信息
success	是否成功
error_message	失败原因
cost_time	接口耗时
created_at	创建时间
deleted	逻辑删除

## 7. 操作类型枚举设计

### 7.1 OperationTypeEnum

路径：

src/main/java/com/internpilot/enums/OperationTypeEnum.java

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum OperationTypeEnum {

    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "退出"),
    UPLOAD("UPLOAD", "上传"),
    DOWNLOAD("DOWNLOAD", "下载"),
    AI("AI", "AI操作"),
    GRANT("GRANT", "授权"),
    DISABLE("DISABLE", "禁用"),
    ENABLE("ENABLE", "启用"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    OperationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

## 8. Entity 与 Mapper 设计

### 8.1 SystemOperationLog Entity

路径：

src/main/java/com/internpilot/entity/SystemOperationLog.java

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_operation_log")
public class SystemOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operatorUsername;

    private String module;

    private String operation;

    private String operationType;

    private String requestUri;

    private String requestMethod;

    private String requestParams;

    private String ipAddress;

    private String userAgent;

    private Integer success;

    private String errorMessage;

    private Long costTime;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
```

### 8.2 SystemOperationLogMapper

路径：

src/main/java/com/internpilot/mapper/SystemOperationLogMapper.java

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.SystemOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemOperationLogMapper extends BaseMapper<SystemOperationLog> {
}
```

## 9. 注解设计

### 9.1 OperationLog 注解

路径：

src/main/java/com/internpilot/annotation/OperationLog.java

```java
package com.internpilot.annotation;

import com.internpilot.enums.OperationTypeEnum;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module();

    /**
     * 操作名称
     */
    String operation();

    /**
     * 操作类型
     */
    OperationTypeEnum type() default OperationTypeEnum.OTHER;

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;
}
```

### 9.2 使用示例

```java
@OperationLog(
        module = "简历管理",
        operation = "上传简历",
        type = OperationTypeEnum.UPLOAD,
        recordParams = false
)
@PostMapping("/upload")
public Result<ResumeUploadResponse> upload(...) {
    ...
}
```

## 10. AOP 切面设计

### 10.1 依赖检查

如果项目已经有 Spring Boot Web，通常 AOP 可能未默认引入。

建议在 build.gradle 中加入：

implementation 'org.springframework.boot:spring-boot-starter-aop'

### 10.2 OperationLogAspect

路径：

src/main/java/com/internpilot/aspect/OperationLogAspect.java

```java
package com.internpilot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.annotation.OperationLog;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 1000;
    private static final int MAX_ERROR_LENGTH = 2000;

    private final SystemOperationLogMapper systemOperationLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object recordOperationLog(
            ProceedingJoinPoint joinPoint,
            OperationLog operationLog
    ) throws Throwable {

        long startTime = System.currentTimeMillis();

        SystemOperationLog log = new SystemOperationLog();
        fillBasicInfo(log, operationLog);

        Object result;
        try {
            result = joinPoint.proceed();

            log.setSuccess(1);
            log.setCostTime(System.currentTimeMillis() - startTime);
            systemOperationLogMapper.insert(log);

            return result;

        } catch (Throwable ex) {
            log.setSuccess(0);
            log.setErrorMessage(truncate(ex.getMessage(), MAX_ERROR_LENGTH));
            log.setCostTime(System.currentTimeMillis() - startTime);
            systemOperationLogMapper.insert(log);

            throw ex;
        }
    }

    private void fillBasicInfo(SystemOperationLog log, OperationLog operationLog) {
        log.setModule(operationLog.module());
        log.setOperation(operationLog.operation());
        log.setOperationType(operationLog.type().getCode());

        fillUserInfo(log);
        fillRequestInfo(log, operationLog.recordParams());
    }

    private void fillUserInfo(SystemOperationLog log) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            log.setOperatorId(userDetails.getUserId());
            log.setOperatorUsername(userDetails.getUsername());
        } else if (principal instanceof String username) {
            log.setOperatorUsername(username);
        }
    }

    private void fillRequestInfo(SystemOperationLog log, boolean recordParams) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();

        log.setRequestUri(request.getRequestURI());
        log.setRequestMethod(request.getMethod());
        log.setIpAddress(getClientIp(request));
        log.setUserAgent(truncate(request.getHeader("User-Agent"), 500));

        if (recordParams) {
            log.setRequestParams(extractRequestParams(request));
        }
    }

    private String extractRequestParams(HttpServletRequest request) {
        try {
            String params = objectMapper.writeValueAsString(request.getParameterMap());
            return truncate(params, MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
```

## 11. 参数记录与脱敏设计

### 11.1 为什么要脱敏

日志如果记录太多参数，可能导致：

密码泄露
Token 泄露
简历隐私泄露
AI Key 泄露
日志表暴涨

所以默认只记录简单请求参数，不记录完整请求体。

### 11.2 不记录参数的场景

以下接口建议：

recordParams = false

包括：

登录接口
注册接口
简历上传接口
AI 分析接口
AI 面试题生成接口
修改密码接口

示例：

```java
@OperationLog(
        module = "用户认证",
        operation = "用户登录",
        type = OperationTypeEnum.LOGIN,
        recordParams = false
)
@PostMapping("/login")
public Result<LoginResponse> login(...) {
    ...
}
```

### 11.3 后续可做参数脱敏

后续可以增加脱敏工具：

password -> ****
token -> ****
apiKey -> ****
authorization -> ****

可以设计：

SensitiveDataUtils.mask(json)

第一阶段先用 recordParams = false 控制敏感接口。

## 12. Controller 使用示例

### 12.1 AuthController

```java
@OperationLog(
        module = "用户认证",
        operation = "用户注册",
        type = OperationTypeEnum.CREATE,
        recordParams = false
)
@PostMapping("/register")
public Result<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
    return Result.success(authService.register(request));
}

@OperationLog(
        module = "用户认证",
        operation = "用户登录",
        type = OperationTypeEnum.LOGIN,
        recordParams = false
)
@PostMapping("/login")
public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
    return Result.success(authService.login(request));
}
```

### 12.2 ResumeController

```java
@OperationLog(
        module = "简历管理",
        operation = "上传简历",
        type = OperationTypeEnum.UPLOAD,
        recordParams = false
)
@PostMapping("/upload")
@PreAuthorize("hasAuthority('resume:write')")
public Result<ResumeUploadResponse> upload(...) {
    return Result.success(resumeService.upload(...));
}

@OperationLog(
        module = "简历管理",
        operation = "删除简历",
        type = OperationTypeEnum.DELETE
)
@DeleteMapping("/{id}")
@PreAuthorize("hasAuthority('resume:delete')")
public Result<Boolean> delete(@PathVariable Long id) {
    return Result.success(resumeService.delete(id));
}
```

### 12.3 JobController

```java
@OperationLog(
        module = "岗位管理",
        operation = "创建岗位",
        type = OperationTypeEnum.CREATE
)
@PostMapping
@PreAuthorize("hasAuthority('job:write')")
public Result<JobCreateResponse> create(@RequestBody @Valid JobCreateRequest request) {
    return Result.success(jobService.create(request));
}

@OperationLog(
        module = "岗位管理",
        operation = "修改岗位",
        type = OperationTypeEnum.UPDATE
)
@PutMapping("/{id}")
@PreAuthorize("hasAuthority('job:write')")
public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid JobUpdateRequest request) {
    return Result.success(jobService.update(id, request));
}

@OperationLog(
        module = "岗位管理",
        operation = "删除岗位",
        type = OperationTypeEnum.DELETE
)
@DeleteMapping("/{id}")
@PreAuthorize("hasAuthority('job:delete')")
public Result<Boolean> delete(@PathVariable Long id) {
    return Result.success(jobService.delete(id));
}
```

### 12.4 AnalysisController

```java
@OperationLog(
        module = "AI分析",
        operation = "发起简历岗位匹配分析",
        type = OperationTypeEnum.AI,
        recordParams = false
)
@PostMapping("/match")
@PreAuthorize("hasAuthority('analysis:write')")
public Result<AnalysisResultResponse> match(@RequestBody @Valid AnalysisMatchRequest request) {
    return Result.success(analysisService.match(request));
}
```

### 12.5 InterviewQuestionController

```java
@OperationLog(
        module = "AI面试题",
        operation = "生成AI面试题",
        type = OperationTypeEnum.AI,
        recordParams = false
)
@PostMapping("/generate")
@PreAuthorize("hasAuthority('analysis:write')")
public Result<InterviewQuestionGenerateResponse> generate(
        @RequestBody @Valid InterviewQuestionGenerateRequest request
) {
    return Result.success(interviewQuestionService.generate(request));
}
```

### 12.6 ApplicationController

```java
@OperationLog(
        module = "投递记录",
        operation = "创建投递记录",
        type = OperationTypeEnum.CREATE
)
@PostMapping
@PreAuthorize("hasAuthority('application:write')")
public Result<ApplicationCreateResponse> create(@RequestBody @Valid ApplicationCreateRequest request) {
    return Result.success(applicationService.create(request));
}

@OperationLog(
        module = "投递记录",
        operation = "修改投递状态",
        type = OperationTypeEnum.UPDATE
)
@PutMapping("/{id}/status")
@PreAuthorize("hasAuthority('application:write')")
public Result<Boolean> updateStatus(...) {
    return Result.success(applicationService.updateStatus(...));
}
```

## 13. 日志查询接口设计

### 13.1 功能范围

管理员可以查询系统操作日志。

支持：

分页查询；
按模块筛选；
按操作类型筛选；
按用户名搜索；
按成功 / 失败筛选；
按时间倒序。

### 13.2 请求参数

module
operationType
username
success
pageNum
pageSize

### 13.3 接口设计

GET /api/admin/operation-logs
GET /api/admin/operation-logs/{id}
DELETE /api/admin/operation-logs/{id}

权限：

system:log:read
system:log:delete

## 14. DTO / VO 设计

### 14.1 OperationLogListResponse

路径：

src/main/java/com/internpilot/vo/admin/OperationLogListResponse.java

```java
package com.internpilot.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志列表响应")
public class OperationLogListResponse {

    private Long logId;

    private Long operatorId;

    private String operatorUsername;

    private String module;

    private String operation;

    private String operationType;

    private String requestUri;

    private String requestMethod;

    private String ipAddress;

    private Integer success;

    private Long costTime;

    private LocalDateTime createdAt;
}
```

### 14.2 OperationLogDetailResponse

路径：

src/main/java/com/internpilot/vo/admin/OperationLogDetailResponse.java

```java
package com.internpilot.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志详情响应")
public class OperationLogDetailResponse {

    private Long logId;

    private Long operatorId;

    private String operatorUsername;

    private String module;

    private String operation;

    private String operationType;

    private String requestUri;

    private String requestMethod;

    private String requestParams;

    private String ipAddress;

    private String userAgent;

    private Integer success;

    private String errorMessage;

    private Long costTime;

    private LocalDateTime createdAt;
}
```

## 15. Service 设计

### 15.1 AdminOperationLogService

路径：

src/main/java/com/internpilot/service/AdminOperationLogService.java

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;

public interface AdminOperationLogService {

    PageResult<OperationLogListResponse> list(
            String module,
            String operationType,
            String username,
            Integer success,
            Integer pageNum,
            Integer pageSize
    );

    OperationLogDetailResponse getDetail(Long id);

    Boolean delete(Long id);
}
```

### 15.2 AdminOperationLogServiceImpl

路径：

src/main/java/com/internpilot/service/impl/AdminOperationLogServiceImpl.java

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.service.AdminOperationLogService;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private final SystemOperationLogMapper systemOperationLogMapper;

    @Override
    public PageResult<OperationLogListResponse> list(
            String module,
            String operationType,
            String username,
            Integer success,
            Integer pageNum,
            Integer pageSize
    ) {
        LambdaQueryWrapper<SystemOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemOperationLog::getDeleted, 0);

        if (StringUtils.hasText(module)) {
            wrapper.eq(SystemOperationLog::getModule, module);
        }

        if (StringUtils.hasText(operationType)) {
            wrapper.eq(SystemOperationLog::getOperationType, operationType);
        }

        if (StringUtils.hasText(username)) {
            wrapper.like(SystemOperationLog::getOperatorUsername, username);
        }

        if (success != null) {
            wrapper.eq(SystemOperationLog::getSuccess, success);
        }

        wrapper.orderByDesc(SystemOperationLog::getCreatedAt);

        Page<SystemOperationLog> page = new Page<>(pageNum, pageSize);
        Page<SystemOperationLog> resultPage = systemOperationLogMapper.selectPage(page, wrapper);

        List<OperationLogListResponse> records = resultPage.getRecords()
                .stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public OperationLogDetailResponse getDetail(Long id) {
        SystemOperationLog log = systemOperationLogMapper.selectOne(
                new LambdaQueryWrapper<SystemOperationLog>()
                        .eq(SystemOperationLog::getId, id)
                        .eq(SystemOperationLog::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (log == null) {
            throw new BusinessException("操作日志不存在");
        }

        return toDetailResponse(log);
    }

    @Override
    public Boolean delete(Long id) {
        SystemOperationLog log = systemOperationLogMapper.selectById(id);

        if (log == null || log.getDeleted() != null && log.getDeleted() == 1) {
            throw new BusinessException("操作日志不存在");
        }

        log.setDeleted(1);
        systemOperationLogMapper.updateById(log);

        return true;
    }

    private OperationLogListResponse toListResponse(SystemOperationLog log) {
        OperationLogListResponse response = new OperationLogListResponse();
        response.setLogId(log.getId());
        response.setOperatorId(log.getOperatorId());
        response.setOperatorUsername(log.getOperatorUsername());
        response.setModule(log.getModule());
        response.setOperation(log.getOperation());
        response.setOperationType(log.getOperationType());
        response.setRequestUri(log.getRequestUri());
        response.setRequestMethod(log.getRequestMethod());
        response.setIpAddress(log.getIpAddress());
        response.setSuccess(log.getSuccess());
        response.setCostTime(log.getCostTime());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private OperationLogDetailResponse toDetailResponse(SystemOperationLog log) {
        OperationLogDetailResponse response = new OperationLogDetailResponse();
        response.setLogId(log.getId());
        response.setOperatorId(log.getOperatorId());
        response.setOperatorUsername(log.getOperatorUsername());
        response.setModule(log.getModule());
        response.setOperation(log.getOperation());
        response.setOperationType(log.getOperationType());
        response.setRequestUri(log.getRequestUri());
        response.setRequestMethod(log.getRequestMethod());
        response.setRequestParams(log.getRequestParams());
        response.setIpAddress(log.getIpAddress());
        response.setUserAgent(log.getUserAgent());
        response.setSuccess(log.getSuccess());
        response.setErrorMessage(log.getErrorMessage());
        response.setCostTime(log.getCostTime());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
```

## 16. Controller 设计

### 16.1 AdminOperationLogController

路径：

src/main/java/com/internpilot/controller/admin/AdminOperationLogController.java

```java
package com.internpilot.controller.admin;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.annotation.OperationLog;
import com.internpilot.service.AdminOperationLogService;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员-系统操作日志接口")
@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "查询系统操作日志列表")
    @PreAuthorize("hasAuthority('system:log:read')")
    @GetMapping
    public Result<PageResult<OperationLogListResponse>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer success,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(
                adminOperationLogService.list(
                        module,
                        operationType,
                        username,
                        success,
                        pageNum,
                        pageSize
                )
        );
    }

    @Operation(summary = "查询系统操作日志详情")
    @PreAuthorize("hasAuthority('system:log:read')")
    @GetMapping("/{id}")
    public Result<OperationLogDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(adminOperationLogService.getDetail(id));
    }

    @Operation(summary = "删除系统操作日志")
    @OperationLog(
            module = "系统日志",
            operation = "删除系统操作日志",
            type = OperationTypeEnum.DELETE
    )
    @PreAuthorize("hasAuthority('system:log:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(adminOperationLogService.delete(id));
    }
}
```

## 17. 权限设计

### 17.1 新增权限

需要在 permission 表加入：

```sql
INSERT INTO permission (permission_code, permission_name, resource_type, description, enabled)
VALUES
('system:log:read', '查看系统操作日志', 'SYSTEM_LOG', '查看系统操作日志列表和详情', 1),
('system:log:delete', '删除系统操作日志', 'SYSTEM_LOG', '逻辑删除系统操作日志', 1);
```

如果之前已经有 system:log:read，只需要补 system:log:delete。

### 17.2 给 ADMIN 分配权限

```sql
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('system:log:read', 'system:log:delete')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
        AND rp.deleted = 0
  );
```

### 17.3 普通用户权限

普通用户不应该有：

system:log:read
system:log:delete

否则普通用户就能查看其他用户的操作日志。

## 18. 前端页面设计

### 18.1 页面路径

建议新增管理员页面：

/admin/operation-logs
/admin/operation-logs/:id

### 18.2 菜单位置

管理员菜单：

系统管理
  ├── 用户管理
  ├── 角色权限
  └── 操作日志

操作日志菜单需要权限：

system:log:read

### 18.3 列表页字段

字段	说明
logId	日志 ID
operatorUsername	操作人
module	模块
operation	操作
operationType	类型
requestMethod	请求方法
requestUri	请求路径
success	是否成功
costTime	耗时
createdAt	时间
操作	详情

### 18.4 筛选项

模块
操作类型
用户名
是否成功

### 18.5 前端 API 封装

路径：

src/api/adminOperationLog.ts

```ts
import request from '@/utils/request'

export function getOperationLogListApi(params: any) {
  return request.get('/api/admin/operation-logs', { params })
}

export function getOperationLogDetailApi(id: number) {
  return request.get(`/api/admin/operation-logs/${id}`)
}

export function deleteOperationLogApi(id: number) {
  return request.delete(`/api/admin/operation-logs/${id}`)
}
```

### 18.6 OperationLogList.vue 核心示例

```vue
<template>
  <el-card>
    <template #header>系统操作日志</template>

    <el-form :inline="true" :model="query">
      <el-form-item label="模块">
        <el-input v-model="query.module" placeholder="如 简历管理" />
      </el-form-item>

      <el-form-item label="类型">
        <el-select v-model="query.operationType" clearable placeholder="操作类型" style="width: 140px">
          <el-option label="新增" value="CREATE" />
          <el-option label="修改" value="UPDATE" />
          <el-option label="删除" value="DELETE" />
          <el-option label="登录" value="LOGIN" />
          <el-option label="上传" value="UPLOAD" />
          <el-option label="AI操作" value="AI" />
        </el-select>
      </el-form-item>

      <el-form-item label="用户">
        <el-input v-model="query.username" placeholder="用户名" />
      </el-form-item>

      <el-form-item label="结果">
        <el-select v-model="query.success" clearable placeholder="成功/失败" style="width: 120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>

      <el-button type="primary" @click="loadList">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </el-form>

    <el-table :data="list" style="margin-top: 16px">
      <el-table-column prop="logId" label="ID" width="80" />
      <el-table-column prop="operatorUsername" label="操作人" width="120" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="operation" label="操作" width="160" />
      <el-table-column prop="operationType" label="类型" width="100" />
      <el-table-column prop="requestMethod" label="方法" width="90" />
      <el-table-column prop="requestUri" label="路径" />
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.success === 1 ? 'success' : 'danger'">
            {{ row.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costTime" label="耗时(ms)" width="100" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" @click="goDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import router from '@/router'
import { getOperationLogListApi } from '@/api/adminOperationLog'

const list = ref<any[]>([])

const query = reactive({
  module: '',
  operationType: '',
  username: '',
  success: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

async function loadList() {
  const res: any = await getOperationLogListApi(query)
  list.value = res.records
}

function resetQuery() {
  query.module = ''
  query.operationType = ''
  query.username = ''
  query.success = undefined
  query.pageNum = 1
  loadList()
}

function goDetail(row: any) {
  router.push(`/admin/operation-logs/${row.logId}`)
}

onMounted(loadList)
</script>
```

## 19. 测试流程

### 19.1 数据库检查

执行：

SHOW TABLES LIKE 'system_operation_log';

确认表存在。

### 19.2 登录日志测试

调用登录接口：

POST /api/auth/login

检查日志：

```sql
SELECT id, operator_username, module, operation, operation_type, success, cost_time, created_at
FROM system_operation_log
ORDER BY id DESC;
```

期望出现：

用户认证 / 用户登录 / LOGIN

### 19.3 上传简历日志测试

上传简历后检查日志：

简历管理 / 上传简历 / UPLOAD

### 19.4 AI 分析日志测试

发起 AI 分析后检查日志：

AI分析 / 发起简历岗位匹配分析 / AI

### 19.5 失败日志测试

故意传入不存在的 resumeId：

```json
{
  "resumeId": 999999,
  "jobId": 1
}
```

期望日志中：

success = 0
error_message 不为空

### 19.6 管理员查询日志测试

管理员登录后访问：

GET /api/admin/operation-logs

期望：

200 成功返回日志列表

普通用户访问：

GET /api/admin/operation-logs

期望：

403 Forbidden

## 20. PowerShell 测试示例

### 20.1 查询日志列表

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/operation-logs?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

### 20.2 按类型筛选

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/operation-logs?operationType=AI&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

### 20.3 查询详情

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/operation-logs/1" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

## 21. 常见问题

### 21.1 日志没有记录

可能原因：

没有引入 spring-boot-starter-aop；
注解没有加到方法上；
AOP 切面类没有被 Spring 扫描；
方法不是通过 Spring Bean 调用；
数据库表不存在；
Mapper 没有扫描到。

### 21.2 登录接口 operatorId 为空

登录前用户还没有进入 SecurityContext，因此登录日志可能只有用户名或没有 operatorId。

这是正常现象。

优化方式：

登录成功后在 AuthService 中手动补充日志
或在 AOP 中从请求体解析 username

第一阶段可以接受。

### 21.3 请求参数为空

当前 AOP 示例只记录 query 参数，不记录 JSON body。

原因：

JSON body 读取一次后会影响 Controller 正常接收

第一阶段建议只记录 query 参数。

后续可以使用：

ContentCachingRequestWrapper

封装请求体，但实现复杂一些。

### 21.4 文件上传参数异常

文件上传接口不要记录完整 Multipart 参数。

使用：

recordParams = false

### 21.5 日志记录失败影响业务

当前设计中日志插入失败可能影响业务。

后续优化：

日志异步写入
日志失败不影响主业务
使用消息队列记录日志

第一阶段为了简单，先同步写入。

## 22. 后续增强方向

### 22.1 异步日志

可以使用线程池异步写入日志：

业务执行完成
  ↓
提交日志写入任务
  ↓
异步落库

优点：

降低业务接口耗时
日志失败不影响主流程

### 22.2 操作日志归档

日志表会越来越大。

后续可以：

按月归档
定期清理 90 天前日志
导出 CSV

### 22.3 安全告警

可以基于日志分析：

连续登录失败
频繁访问不存在资源
频繁触发 403
异常大量 AI 调用

然后做告警。

### 22.4 管理员看板

可以统计：

今日操作数
失败操作数
AI 调用次数
登录用户数
高频操作模块

## 23. 面试讲解准备

### 23.1 面试官问：操作日志模块怎么实现的？

回答：

我使用注解加 AOP 的方式实现操作日志。首先定义了 @OperationLog 注解，在需要记录的接口方法上标明模块、操作名称和操作类型。然后通过 Spring AOP 的 @Around 切面拦截这些方法，在方法执行前记录开始时间，执行后记录是否成功、耗时、请求路径、请求方法、IP、User-Agent 和当前登录用户信息，最后写入 system_operation_log 表。

这样业务代码不需要手动写日志，日志记录逻辑统一维护，后续管理员可以在后台查看系统操作记录。

### 23.2 面试官问：为什么不用在每个 Service 手动写日志？

回答：

手动写日志会侵入业务代码，而且容易重复和漏记。使用注解加 AOP 后，只需要在接口上加 @OperationLog，就能统一记录操作日志。

这种方式更符合横切关注点的处理方式，比如日志、权限、事务、性能统计都可以通过 AOP 统一处理。

### 23.3 面试官问：操作日志里会不会泄露隐私？

回答：

会有这个风险，所以我设计了 recordParams 开关。对于登录、注册、简历上传、AI 分析这类包含敏感信息的接口，不记录请求参数。

同时日志中不记录完整密码、JWT Token、简历文本、岗位 JD 和 AI API Key。后续还可以加入统一脱敏工具，对 password、token、apiKey 等字段自动打码。

### 23.4 面试官问：操作日志失败会不会影响主业务？

回答：

当前第一阶段是同步写入日志，实现简单，但确实存在日志写入失败影响业务的可能。

后续可以优化为异步写入，比如使用线程池或消息队列，把日志写入和主业务解耦。这样即使日志写入失败，也不会影响用户上传简历或发起 AI 分析。

### 23.5 面试官问：操作日志和业务日志有什么区别？

回答：

业务日志主要给开发者排查问题，比如打印某个方法执行到哪里、某个异常栈是什么。

操作日志主要给管理员和系统审计使用，记录谁在什么时候做了什么操作，是否成功，失败原因是什么。它更偏审计和后台管理。

## 24. 简历写法

完成该模块后，简历可以增加：

- 基于自定义注解 + Spring AOP 实现系统操作日志模块，自动记录用户关键操作、请求路径、操作类型、IP、执行结果和耗时，为管理员后台审计和问题排查提供支持。

更完整写法：

- 设计 system_operation_log 操作日志表，使用 @OperationLog 注解和 AOP 切面统一记录登录、简历上传、岗位管理、AI 分析、投递状态修改等关键操作，并通过 RBAC 控制管理员日志查询权限。

## 25. 开发顺序建议

推荐按以下顺序开发：

1. 创建 system_operation_log 表；
2. 创建 OperationTypeEnum；
3. 创建 SystemOperationLog Entity；
4. 创建 SystemOperationLogMapper；
5. 引入 spring-boot-starter-aop；
6. 创建 @OperationLog 注解；
7. 创建 OperationLogAspect；
8. 在核心 Controller 方法上添加 @OperationLog；
9. 新增 system:log:read 和 system:log:delete 权限；
10. 给 ADMIN 分配日志权限；
11. 创建 OperationLogListResponse；
12. 创建 OperationLogDetailResponse；
13. 创建 AdminOperationLogService；
14. 创建 AdminOperationLogController；
15. Swagger 测试日志查询；
16. 前端新增管理员日志页面；
17. 测试成功日志；
18. 测试失败日志；
19. 测试普通用户访问日志接口返回 403。

## 26. 验收标准

### 26.1 后端验收

system_operation_log 表存在；
@OperationLog 注解可用；
AOP 切面可以拦截方法；
登录操作可以记录日志；
简历上传可以记录日志；
岗位创建可以记录日志；
AI 分析可以记录日志；
投递状态修改可以记录日志；
失败操作 success = 0；
失败操作 errorMessage 不为空；
costTime 有值；
管理员可以查询日志；
普通用户不能查询日志。

### 26.2 前端验收

管理员菜单中有操作日志；
普通用户看不到操作日志菜单；
日志列表可以加载；
可以按模块筛选；
可以按操作类型筛选；
可以按用户名筛选；
可以按成功 / 失败筛选；
可以查看日志详情。

## 27. 模块设计结论

系统操作日志模块是 InternPilot 从功能型项目升级为工程化项目的重要增强。

它通过：

@OperationLog 注解
  ↓
Spring AOP 切面
  ↓
自动采集请求和用户信息
  ↓
写入 system_operation_log 表
  ↓
管理员后台查询

实现了对关键操作的审计追踪。

完成该模块后，项目将具备：

更强的工程化能力
更好的问题排查能力
管理员后台基础
安全审计基础
更好的面试表达价值
