# InternPilot Spring Boot 工程能力增强设计文档

## 1. 文档背景

InternPilot 当前已经完成主要业务功能、AI 能力增强、线上部署和 Release 发布。

当前系统已经具备：

```text
1. 邮箱验证码注册登录
2. JWT 鉴权
3. RBAC 权限系统
4. 用户中心
5. 简历管理
6. 岗位管理
7. AI 简历岗位匹配分析
8. WebSocket AI 进度推送
9. 全局 AI 任务中心
10. 岗位推荐
11. AI 面试题生成
12. RAG 岗位知识库问答
13. DeepSeek API 接入
14. AI 模型路由与 Prompt 版本管理
15. 用户反馈功能
16. 管理员后台
17. Docker Compose 服务器部署
```

当前项目已经不是“功能能不能跑”的阶段，而是进入：

```text
工程质量增强
  ↓
后端可观测性增强
  ↓
接口稳定性增强
  ↓
生产环境可维护性增强
  ↓
面试项目含金量提升
```

因此，本阶段不再新增大型业务功能，而是围绕 Spring Boot 工程能力做增强。

---

## 2. 本阶段目标

本阶段目标是让 InternPilot 后端更接近真实企业项目的工程实践。

核心目标：

```text
1. 增加 Spring Boot Actuator 健康检查
2. 增强统一参数校验
3. 增强全局异常处理
4. 增加 AOP 接口耗时统计
5. 增强操作日志记录
6. 规范 Redis 缓存使用
7. 增加定时任务清理机制
8. 增强生产环境配置隔离
9. 增加 Docker 健康检查
10. 完善测试和 README 运维说明
```

---

## 3. 本阶段不做什么

为了控制复杂度，本阶段不做：

```text
1. 不拆分微服务
2. 不引入 Spring Cloud
3. 不引入 WebFlux 全量响应式改造
4. 不引入 Kafka / RabbitMQ 等消息队列
5. 不引入复杂链路追踪系统
6. 不引入 Kubernetes
7. 不重构现有业务主流程
8. 不改变当前 AI 模型路由设计
9. 不改变当前 Docker Compose 部署方式
```

说明：

> 当前 InternPilot 作为一个 Java 全栈项目，单体 Spring Boot + Vue + MySQL + Redis + Docker Compose 架构是合理的。当前阶段重点是增强单体项目的工程质量，而不是为了复杂而复杂。

---

## 4. 总体设计方案

本阶段采用“小步增强”的方式，在不破坏现有业务的前提下增加工程能力。

整体增强方向：

```text
请求进入系统
  ↓
参数校验
  ↓
认证鉴权
  ↓
AOP 记录接口耗时
  ↓
Controller
  ↓
Service
  ↓
Mapper / Redis / DeepSeek
  ↓
统一异常处理
  ↓
统一响应
  ↓
操作日志 / 监控日志 / 健康检查
```

增强后的能力：

```text
1. 系统是否健康：Actuator 可查看
2. 接口参数是否规范：Validation 校验
3. 出错后返回是否统一：GlobalExceptionHandler 处理
4. 哪些接口慢：AOP 记录耗时
5. 用户做了什么：操作日志记录
6. 缓存 key 是否规范：统一缓存常量和工具
7. 过期数据是否清理：定时任务处理
8. Docker 是否能判断服务健康：healthcheck
```

---

# 5. 功能一：Spring Boot Actuator 健康检查

## 5.1 目标

增加系统运行状态检查能力。

用于判断：

```text
1. 后端服务是否启动成功
2. 数据库是否连接正常
3. Redis 是否连接正常
4. 服务是否适合被 Docker healthcheck 检测
5. 线上排查问题时是否有基础健康状态入口
```

---

## 5.2 添加依赖

修改：

```text
backend/intern-pilot-backend/build.gradle
```

增加：

```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

---

## 5.3 配置 Actuator

修改：

```text
backend/intern-pilot-backend/src/main/resources/application.yml
backend/intern-pilot-backend/src/main/resources/application-prod.yml
```

建议配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

生产环境建议：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

说明：

```text
开发环境可以暴露 metrics。
生产环境建议只暴露 health 和 info，避免暴露过多系统细节。
```

---

## 5.4 健康检查接口

Actuator 默认接口：

```text
GET /actuator/health
GET /actuator/info
```

期望结果：

```json
{
  "status": "UP"
}
```

---

## 5.5 安全策略

当前项目已有 Spring Security，需要配置 Actuator 访问规则。

建议：

```text
1. /actuator/health 允许匿名访问，便于 Docker healthcheck
2. /actuator/info 允许匿名或登录访问
3. /actuator/metrics 仅管理员访问，或者生产环境不暴露
```

修改：

```text
SecurityConfig.java
```

示例：

```java
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/actuator/info").permitAll()
.requestMatchers("/actuator/**").hasAuthority("system:monitor")
```

如果当前权限体系中没有 `system:monitor`，可以先不开放 metrics。

---

# 6. 功能二：统一参数校验增强

## 6.1 目标

当前部分接口可能只在 Service 中手动判断参数。为了提升代码规范性，需要使用 Spring Validation。

目标：

```text
1. DTO 使用注解校验
2. Controller 使用 @Valid / @Validated
3. 参数错误统一返回友好提示
4. 减少业务代码中的重复 if 判断
```

---

## 6.2 添加依赖

如果项目还没有 validation 依赖，添加：

```gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

---

## 6.3 DTO 校验示例

注册请求：

```java
@Data
public class EmailRegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 位之间")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须是 6 位")
    private String code;
}
```

用户反馈请求：

```java
@Data
public class FeedbackCreateRequest {

    @NotBlank(message = "反馈类型不能为空")
    private String type;

    @NotBlank(message = "反馈标题不能为空")
    @Size(max = 120, message = "反馈标题不能超过 120 个字符")
    private String title;

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容不能超过 2000 个字符")
    private String content;

    @Size(max = 100, message = "联系方式不能超过 100 个字符")
    private String contact;
}
```

Controller：

```java
@PostMapping("/register/email")
public Result<Void> registerByEmail(@Valid @RequestBody EmailRegisterRequest request) {
    authService.registerByEmail(request);
    return Result.success();
}
```

---

## 6.4 需要优先补强的 DTO

优先检查这些模块：

```text
1. 登录注册 DTO
2. 邮箱验证码 DTO
3. 用户中心修改资料 DTO
4. 修改密码 DTO
5. 简历创建 / 修改 DTO
6. 岗位创建 / 修改 DTO
7. AI 分析请求 DTO
8. 面试题生成 DTO
9. RAG 知识库 DTO
10. 用户反馈 DTO
11. 管理员用户 / 角色 / 权限 DTO
```

---

# 7. 功能三：全局异常处理增强

## 7.1 目标

当前项目应已有 `GlobalExceptionHandler`。本阶段需要增强它，让错误响应更统一、更适合前端展示和后端排查。

目标：

```text
1. 参数校验异常统一处理
2. 权限异常统一处理
3. 业务异常统一处理
4. AI 调用异常统一处理
5. 数据库异常统一处理
6. 未知异常兜底处理
7. 生产环境不暴露堆栈细节
```

---

## 7.2 建议统一错误结构

继续使用现有 `Result` 格式。如果没有统一错误码，可以逐步增加：

```json
{
  "code": 400,
  "message": "邮箱格式不正确",
  "data": null
}
```

更增强的结构可选：

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": {
    "field": "email",
    "reason": "邮箱格式不正确"
  }
}
```

---

## 7.3 建议处理的异常类型

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public Result<?> handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .findFirst()
        .map(FieldError::getDefaultMessage)
        .orElse("参数校验失败");
    return Result.fail(400, message);
}

@ExceptionHandler(ConstraintViolationException.class)
public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
    return Result.fail(400, "参数校验失败");
}

@ExceptionHandler(AccessDeniedException.class)
public Result<?> handleAccessDeniedException(AccessDeniedException e) {
    return Result.fail(403, "无权限访问");
}

@ExceptionHandler(BusinessException.class)
public Result<?> handleBusinessException(BusinessException e) {
    return Result.fail(e.getCode(), e.getMessage());
}

@ExceptionHandler(AiServiceException.class)
public Result<?> handleAiServiceException(AiServiceException e) {
    return Result.fail(500, "AI 服务暂时不可用，请稍后重试");
}

@ExceptionHandler(Exception.class)
public Result<?> handleException(Exception e) {
    log.error("系统未知异常", e);
    return Result.fail(500, "系统繁忙，请稍后重试");
}
```

---

## 7.4 日志安全要求

异常日志中不要输出：

```text
1. JWT token
2. DeepSeek API Key
3. QQ 邮箱授权码
4. 完整简历内容
5. 完整 Prompt
6. 用户密码
```

可以输出：

```text
1. userId
2. requestId
3. URI
4. method
5. errorCode
6. errorMessage
7. stacktrace，生产环境可保留在日志文件中，但不要返回给前端
```

---

# 8. 功能四：AOP 接口耗时统计

## 8.1 目标

增加接口耗时统计，方便定位慢接口。

目标：

```text
1. 记录每个接口耗时
2. 慢接口输出 warning 日志
3. 记录当前用户 ID
4. 记录请求路径和请求方法
5. 不记录敏感请求体
```

---

## 8.2 新增注解

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/common/annotation/LogExecutionTime.java
```

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {

    String value() default "";

    long slowThresholdMs() default 1000;
}
```

---

## 8.3 新增 AOP

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/common/aspect/ExecutionTimeAspect.java
```

核心逻辑：

```java
@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Around("@annotation(logExecutionTime)")
    public Object around(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = false;

        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            long cost = System.currentTimeMillis() - start;
            String methodName = joinPoint.getSignature().toShortString();

            if (cost >= logExecutionTime.slowThresholdMs()) {
                log.warn("Slow method detected: method={}, cost={}ms, success={}",
                        methodName, cost, success);
            } else {
                log.info("Method executed: method={}, cost={}ms, success={}",
                        methodName, cost, success);
            }
        }
    }
}
```

---

## 8.4 推荐加注解的位置

优先加在：

```text
1. AI 分析接口
2. 岗位推荐接口
3. 面试题生成接口
4. RAG 问答接口
5. 简历上传 / 解析接口
6. 管理员列表查询接口
```

示例：

```java
@LogExecutionTime(value = "AI简历分析", slowThresholdMs = 3000)
@PostMapping("/analyze")
public Result<?> analyze(@Valid @RequestBody AnalysisRequest request) {
    return Result.success(analysisService.analyze(request));
}
```

---

# 9. 功能五：操作日志增强

## 9.1 当前情况

项目中已有系统操作日志模块。当前阶段不建议推翻重做，而是增强它的工程实用性。

---

## 9.2 增强目标

```text
1. 统一操作日志注解
2. 自动记录操作者 userId
3. 自动记录操作模块
4. 自动记录操作类型
5. 自动记录请求 URI
6. 自动记录 IP
7. 自动记录耗时
8. 自动记录成功 / 失败状态
9. 避免记录敏感字段
```

---

## 9.3 操作日志注解

如果已有注解，则增强现有注解。若没有，可新增：

```text
common/annotation/OperationLog.java
```

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    String module();

    String action();

    String description() default "";
}
```

示例：

```java
@OperationLog(module = "用户反馈", action = "回复反馈", description = "管理员回复用户反馈")
@PutMapping("/admin/feedback/{id}/reply")
public Result<Void> replyFeedback(@PathVariable Long id,
                                  @Valid @RequestBody FeedbackReplyRequest request) {
    feedbackService.reply(id, request);
    return Result.success();
}
```

---

## 9.4 敏感字段脱敏

操作日志不要记录完整：

```text
1. password
2. oldPassword
3. newPassword
4. token
5. authorization
6. apiKey
7. mailAuthCode
8. resumeContent
9. prompt
```

可以新增工具类：

```text
common/util/SensitiveDataMasker.java
```

规则：

```text
password -> ****
token -> ****
authorization -> ****
email -> 342****714@qq.com
phone -> 138****0000
```

---

# 10. 功能六：Redis 缓存规范化

## 10.1 目标

当前项目已使用 Redis 存储验证码、AI 任务状态、缓存等。需要规范 key 命名、TTL、常量管理。

---

## 10.2 Redis Key 命名规范

建议统一：

```text
internpilot:{domain}:{type}:{id}
```

示例：

```text
internpilot:auth:email-code:{email}
internpilot:ai:analysis-task:{taskNo}
internpilot:ai:analysis-cache:{cacheKey}
internpilot:user:permission:{userId}
internpilot:rag:qa-cache:{hash}
```

---

## 10.3 新增 RedisKeyConstants

新增：

```text
common/constant/RedisKeyConstants.java
```

```java
public class RedisKeyConstants {

    public static final String EMAIL_CODE_PREFIX = "internpilot:auth:email-code:";
    public static final String AI_TASK_PREFIX = "internpilot:ai:analysis-task:";
    public static final String AI_CACHE_PREFIX = "internpilot:ai:cache:";
    public static final String USER_PERMISSION_PREFIX = "internpilot:user:permission:";

    public static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(5);
    public static final Duration AI_TASK_TTL = Duration.ofHours(24);
    public static final Duration USER_PERMISSION_TTL = Duration.ofHours(2);

    private RedisKeyConstants() {
    }
}
```

---

## 10.4 缓存使用要求

```text
1. 所有 Redis key 使用常量或 builder 生成
2. 不在业务代码中散落硬编码 key
3. 所有缓存必须设置 TTL
4. 用户权限缓存更新后需要清理
5. AI 缓存 key 必须包含 scenario、model、promptVersion、promptHash
```

---

# 11. 功能七：定时任务清理机制

## 11.1 目标

增加基础定时清理任务，避免数据库中堆积过多过期数据。

可清理：

```text
1. 过期 AI 任务
2. 过期 AI 任务通知
3. 过期验证码相关记录，如果数据库有记录
4. 过期操作日志，可选
5. 过期 AI 调用日志，可选
```

Redis 自身 TTL 会自动清理验证码，不需要定时清理 Redis 验证码。

---

## 11.2 启用调度

启动类增加：

```java
@EnableScheduling
@SpringBootApplication
public class InternPilotApplication {
}
```

---

## 11.3 新增定时任务类

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/task/SystemCleanupTask.java
```

示例：

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemCleanupTask {

    private final AnalysisTaskService analysisTaskService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredAiTasks() {
        log.info("Start cleanup expired AI tasks");
        int count = analysisTaskService.cleanupExpiredTasks(30);
        log.info("Finished cleanup expired AI tasks, count={}", count);
    }
}
```

---

## 11.4 配置开关

建议配置：

```yaml
internpilot:
  cleanup:
    enabled: true
    ai-task-retention-days: 30
    operation-log-retention-days: 90
```

测试环境可关闭：

```yaml
internpilot:
  cleanup:
    enabled: false
```

---

# 12. 功能八：生产环境配置增强

## 12.1 目标

进一步区分：

```text
application.yml
application-dev.yml
application-prod.yml
application-test.yml
```

---

## 12.2 配置原则

```text
application.yml:
    放通用默认配置，不放敏感信息

application-dev.yml:
    本地开发配置，可使用本地 MySQL / Redis

application-test.yml:
    测试配置，使用 Mock AI，避免真实调用 DeepSeek

application-prod.yml:
    生产配置，通过环境变量读取敏感信息
```

---

## 12.3 敏感配置要求

以下配置必须来自环境变量：

```text
DEEPSEEK_API_KEY
JWT_SECRET
MAIL_USERNAME
MAIL_PASSWORD
MYSQL_PASSWORD
REDIS_PASSWORD
```

示例：

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}

spring:
  mail:
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}

jwt:
  secret: ${JWT_SECRET:}
```

---

## 12.4 启动时配置检查

可新增：

```text
config/StartupConfigValidator.java
```

目标：

```text
1. prod 环境下如果 AI_PROVIDER=mock，则启动警告或失败
2. prod 环境下如果 DEEPSEEK_API_KEY 为空，则启动失败
3. prod 环境下如果 JWT_SECRET 使用默认值，则启动失败
4. prod 环境下如果 MAIL_PASSWORD 为空，则邮箱验证码功能警告或失败
```

示例逻辑：

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupConfigValidator implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!prod) {
            return;
        }

        String aiProvider = environment.getProperty("ai.provider");
        String apiKey = environment.getProperty("deepseek.api-key");

        if ("mock".equalsIgnoreCase(aiProvider)) {
            throw new IllegalStateException("生产环境不允许使用 Mock AI");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("生产环境 DeepSeek API Key 不能为空");
        }
    }
}
```

---

# 13. 功能九：Docker 健康检查增强

## 13.1 目标

让 Docker 可以判断容器是否健康。

---

## 13.2 后端 healthcheck

修改：

```text
deploy/docker-compose.yml
```

后端服务增加：

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

注意：

```text
如果后端镜像没有 curl，需要改用 wget，或者在 Dockerfile 中安装 curl。
```

可替代：

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
```

---

## 13.3 前端 healthcheck

前端 Nginx 服务可增加：

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost/ || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
```

---

## 13.4 数据库和 Redis 健康检查

MySQL：

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  interval: 30s
  timeout: 10s
  retries: 5
```

Redis：

```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 30s
  timeout: 10s
  retries: 5
```

---

# 14. 功能十：README 运维说明增强

README 增加：

```text
1. 健康检查接口
2. Docker 服务状态查看
3. 后端日志查看
4. 前端日志查看
5. MySQL 备份
6. Redis 检查
7. 环境变量说明
8. 常见部署问题
```

建议新增章节：

```text
## 运维与健康检查
```

内容示例：

````markdown
### 健康检查

后端健康检查：

```bash
curl http://localhost:8080/actuator/health
````

Docker 服务状态：

```bash
docker compose -f deploy/docker-compose.yml ps
```

查看后端日志：

```bash
docker logs -f internpilot-backend
```

````

---

# 15. 推荐新增 / 修改文件

## 15.1 后端新增文件

```text
common/annotation/LogExecutionTime.java
common/aspect/ExecutionTimeAspect.java
common/annotation/OperationLog.java，若已有则增强
common/util/SensitiveDataMasker.java
common/constant/RedisKeyConstants.java
config/StartupConfigValidator.java
task/SystemCleanupTask.java
````

---

## 15.2 后端修改文件

```text
build.gradle
InternPilotApplication.java
SecurityConfig.java
GlobalExceptionHandler.java
application.yml
application-dev.yml
application-prod.yml
application-test.yml
已有 DTO 类
已有操作日志相关类
已有 Redis key 使用处
AnalysisTaskService.java
AnalysisTaskServiceImpl.java
```

---

## 15.3 部署文件修改

```text
deploy/docker-compose.yml
backend/intern-pilot-backend/Dockerfile，视 healthcheck 命令需要决定
README.md
```

---

# 16. 开发步骤建议

## 第 1 步：创建分支

```bash
git checkout dev
git pull origin dev
git checkout -b feature/spring-boot-engineering-enhancement
```

---

## 第 2 步：保存设计文档

```bash
touch docs/43-spring-boot-engineering-enhancement-design.md
```

---

## 第 3 步：添加 Actuator

```text
1. build.gradle 添加 actuator 依赖
2. application.yml 配置 health/info
3. SecurityConfig 放行 /actuator/health
4. 本地启动验证 /actuator/health
```

---

## 第 4 步：增强参数校验

```text
1. 检查核心 DTO
2. 添加 @NotBlank、@NotNull、@Size、@Email 等注解
3. Controller 添加 @Valid
4. GlobalExceptionHandler 处理校验异常
```

---

## 第 5 步：增强异常处理

```text
1. 梳理现有异常类型
2. 补充参数校验异常
3. 补充权限异常
4. 补充 AI 异常
5. 兜底未知异常
6. 确认前端能正常显示 message
```

---

## 第 6 步：添加接口耗时 AOP

```text
1. 新增 LogExecutionTime 注解
2. 新增 ExecutionTimeAspect
3. 在 AI、RAG、推荐、面试题接口加注解
4. 验证慢接口 warning 日志
```

---

## 第 7 步：增强操作日志

```text
1. 检查现有 OperationLog 实现
2. 增强 userId、URI、IP、耗时、成功失败
3. 增加敏感字段脱敏
4. 验证管理员操作能记录
```

---

## 第 8 步：规范 Redis key

```text
1. 新增 RedisKeyConstants
2. 替换散落的 Redis key 字符串
3. 确认验证码、AI 任务、缓存 TTL 不变
4. 更新相关测试
```

---

## 第 9 步：增加定时任务

```text
1. 启用 @EnableScheduling
2. 新增 SystemCleanupTask
3. 增加配置开关
4. 测试环境默认关闭
5. 验证不会影响单元测试
```

---

## 第 10 步：增强 Docker healthcheck 和 README

```text
1. docker-compose.yml 增加 healthcheck
2. 验证 docker compose ps 能看到健康状态
3. README 增加运维说明
```

---

# 17. 测试清单

## 17.1 后端测试

执行：

```bash
./gradlew test --no-daemon --max-workers=1
```

Windows：

```powershell
.\gradlew.bat test --no-daemon --max-workers=1
```

必须通过。

---

## 17.2 前端构建

虽然本阶段主要改后端，也需要确认前端不受影响：

```bash
npm run build
```

---

## 17.3 Actuator 测试

```bash
curl http://localhost:8080/actuator/health
```

期望：

```json
{"status":"UP"}
```

---

## 17.4 参数校验测试

测试：

```text
1. 注册邮箱为空
2. 邮箱格式错误
3. 验证码为空
4. 密码长度不足
5. 反馈标题为空
6. 反馈内容过长
7. 岗位标题为空
```

期望：

```text
返回 400 和明确错误提示
```

---

## 17.5 权限测试

测试：

```text
1. 未登录访问普通接口返回 401
2. 普通用户访问管理员接口返回 403
3. /actuator/health 可访问
4. /actuator/metrics 生产环境不可公开访问
```

---

## 17.6 AOP 日志测试

测试：

```text
1. AI 分析接口输出耗时日志
2. RAG 问答接口输出耗时日志
3. 慢接口超过阈值输出 warn
4. 异常接口仍然输出耗时
```

---

## 17.7 操作日志测试

测试：

```text
1. 管理员修改用户记录操作日志
2. 管理员回复反馈记录操作日志
3. 管理员管理 RAG 知识库记录操作日志
4. 密码、token、API Key 不进入日志
```

---

## 17.8 定时任务测试

测试：

```text
1. dev 环境可以启动定时任务
2. test 环境可以关闭定时任务
3. 清理任务执行时有日志
4. 清理任务异常不会导致应用退出
```

---

## 17.9 Docker 测试

```bash
docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml ps
```

查看：

```text
backend healthy
frontend healthy
mysql healthy
redis healthy
```

---

# 18. 验收标准

本阶段完成后，应满足：

```text
1. /actuator/health 可访问
2. Spring Security 不阻塞健康检查
3. DTO 参数校验生效
4. 参数错误返回统一 Result
5. 全局异常处理不泄露敏感信息
6. AI、RAG、推荐等核心接口有耗时日志
7. 慢接口会输出 warning 日志
8. 操作日志记录更完整
9. Redis key 命名更规范
10. 定时清理任务可配置开关
11. Docker healthcheck 可用
12. README 有运维说明
13. 后端测试通过
14. 前端构建通过
15. 不破坏现有登录、AI、RAG、管理员后台功能
```

---

# 19. 面试讲法

如果面试官问：

```text
你后期是怎么提升 Spring Boot 项目工程质量的？
```

可以回答：

```text
项目后期我没有继续盲目堆业务功能，而是做了一轮 Spring Boot 工程能力增强。主要包括接入 Spring Boot Actuator 做健康检查，方便部署后判断服务是否正常；使用 Validation 规范 DTO 参数校验，并通过 GlobalExceptionHandler 统一返回错误信息；使用 AOP 记录核心接口耗时，定位 AI 分析、RAG 问答等慢接口；增强操作日志，记录管理员关键操作并对敏感字段脱敏；规范 Redis key 和 TTL；使用定时任务清理过期 AI 任务和日志；最后在 Docker Compose 中增加 healthcheck，让容器状态更可观测。这些优化让项目从能运行，进一步提升到可维护、可排查、可部署的工程状态。
```

---

# 20. 推荐提交信息

```bash
git add .
git commit -m "Enhance Spring Boot engineering and observability"
git push origin feature/spring-boot-engineering-enhancement
```

合并 dev：

```bash
git checkout dev
git pull origin dev
git merge feature/spring-boot-engineering-enhancement
git push origin dev
```

---
