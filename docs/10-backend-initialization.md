# InternPilot 后端工程初始化设计文档

## 1. 文档说明

本文档用于指导 InternPilot 后端工程的初始化，包括技术栈确认、项目创建方式、构建工具选择、依赖设计、配置文件设计、包结构设计、基础公共类设计、Swagger 配置、MySQL 配置、Redis 配置和第一次启动验收标准。

InternPilot 后端采用 Spring Boot 3 构建，主要用于实现用户认证、简历管理、岗位 JD 管理、AI 分析、投递记录管理等功能。

---

## 2. 后端工程目标

后端工程第一阶段目标是搭建一个稳定、规范、可扩展的 Spring Boot 项目基础结构。

初始化完成后，应具备以下能力：

1. Spring Boot 项目可以正常启动；
2. 项目结构清晰；
3. 能连接 MySQL；
4. 能连接 Redis；
5. 能访问 Swagger / Knife4j 接口文档；
6. 具备统一响应结构；
7. 具备统一异常处理；
8. 具备参数校验能力；
9. 具备基础日志能力；
10. 为后续 JWT 鉴权、文件上传、AI 分析模块开发做好准备。

---

## 3. 后端技术选型

### 3.1 核心技术栈

| 技术 | 建议版本 | 用途 |
|---|---|---|
| Java | 17 | 后端开发语言 |
| Spring Boot | 3.2.x / 3.3.x | 后端主框架 |
| Spring Web | 随 Spring Boot | REST API |
| Spring Security | 随 Spring Boot | 认证授权 |
| MyBatis Plus | 3.5.x | 数据持久层 |
| MySQL | 8.x | 业务数据库 |
| Redis | 7.x | 缓存 |
| JWT | jjwt 0.11.x / 0.12.x | Token 认证 |
| Lombok | 最新稳定版 | 简化 Java 代码 |
| Validation | Jakarta Validation | 参数校验 |
| Knife4j / Swagger | 最新稳定版 | 接口文档 |
| Apache PDFBox | 2.x / 3.x | PDF 解析 |
| Apache POI | 5.x | DOCX 解析 |

### 3.2 Java 版本选择

建议使用：

```text
Java 17
```

原因：

1. Spring Boot 3 最低要求 Java 17；
2. Java 17 是长期支持版本；
3. 企业后端项目中使用较多；
4. 适合写进简历；
5. 兼容性比 Java 21 更稳一些。

如果本地已经稳定使用 Java 21，也可以使用 Java 21，但为了减少环境问题，第一版建议 Java 17。

---

## 4. 构建工具选择

### 4.1 推荐选择 Gradle

建议本项目使用 Gradle。

原因：

1. 之前校园搭子匹配系统 2.0 已经使用过 Gradle；
2. Gradle 配置简洁；
3. Spring Boot 官方支持良好；
4. 适合继续积累 Gradle 项目经验；
5. 后续可以方便接入 Docker 和 CI/CD。

项目结构建议：

```text
intern-pilot
├── backend
│   └── intern-pilot-backend
├── frontend
├── docs
├── deploy
└── README.md
```

后端工程放在：

```text
backend/intern-pilot-backend
```

### 4.2 Gradle 项目基本信息

| 项目 | 内容 |
|---|---|
| Group | com.internpilot |
| Artifact | intern-pilot-backend |
| Name | intern-pilot-backend |
| Package | com.internpilot |
| Java | 17 |
| Type | Gradle Project |
| Packaging | Jar |

---

## 5. Spring Boot 项目创建方式

### 5.1 推荐方式一：Spring Initializr

访问 Spring Initializr 创建项目。

项目配置：

```text
Project: Gradle - Groovy
Language: Java
Spring Boot: 3.2.x 或 3.3.x
Group: com.internpilot
Artifact: intern-pilot-backend
Name: intern-pilot-backend
Package name: com.internpilot
Packaging: Jar
Java: 17
```

初始依赖选择：

1. Spring Web；
2. Spring Security；
3. Validation；
4. MySQL Driver；
5. Lombok；
6. Spring Data Redis。

后续手动添加：

1. MyBatis Plus；
2. Knife4j；
3. JWT；
4. PDFBox；
5. Apache POI。

### 5.2 推荐方式二：IDEA 创建

在 IntelliJ IDEA 中：

```text
New Project
  ↓
Spring Boot
  ↓
选择 Java 17
  ↓
选择 Gradle
  ↓
填写 Group 和 Artifact
  ↓
选择依赖
  ↓
创建项目
```

---

## 6. 后端目录结构设计

后端工程初始化后，建议调整为以下结构：

```text
intern-pilot-backend
├── build.gradle
├── settings.gradle
├── README.md
├── .gitignore
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── internpilot
│   │   │           ├── InternPilotApplication.java
│   │   │           ├── common
│   │   │           ├── config
│   │   │           ├── security
│   │   │           ├── controller
│   │   │           ├── service
│   │   │           │   └── impl
│   │   │           ├── mapper
│   │   │           ├── entity
│   │   │           ├── dto
│   │   │           ├── vo
│   │   │           ├── enums
│   │   │           ├── exception
│   │   │           └── util
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-example.yml
│   │       ├── mapper
│   │       └── sql
│   │           └── init.sql
│   └── test
│       └── java
│           └── com
│               └── internpilot
│                   └── InternPilotApplicationTests.java
```

---

## 7. 包结构说明

### 7.1 common 包

```text
common
├── Result.java
├── ResultCode.java
└── PageResult.java
```

作用：

1. 统一响应结构；
2. 统一错误码；
3. 统一分页返回结构。

### 7.2 config 包

```text
config
├── SwaggerConfig.java
├── RedisConfig.java
├── WebConfig.java
├── FileStorageConfig.java
└── MyBatisPlusConfig.java
```

作用：

1. Swagger / Knife4j 配置；
2. Redis 序列化配置；
3. Web MVC 配置；
4. 文件上传配置；
5. MyBatis Plus 分页插件配置。

### 7.3 security 包

```text
security
├── JwtAuthenticationFilter.java
├── JwtTokenProvider.java
├── CustomUserDetails.java
├── CustomUserDetailsService.java
├── JwtAuthenticationEntryPoint.java
└── JwtAccessDeniedHandler.java
```

作用：

1. JWT 生成和解析；
2. Spring Security 登录鉴权；
3. 401 / 403 统一处理；
4. 当前用户身份上下文管理。

初始化阶段可以先创建空包，第二阶段再实现。

### 7.4 controller 包

```text
controller
├── HealthController.java
├── AuthController.java
├── UserController.java
├── ResumeController.java
├── JobController.java
├── AnalysisController.java
└── ApplicationController.java
```

初始化阶段先实现 `HealthController`，用于测试项目是否启动成功。

### 7.5 service 包

```text
service
├── AuthService.java
├── UserService.java
├── ResumeService.java
├── JobService.java
├── AnalysisService.java
├── ApplicationService.java
├── AiService.java
├── FileStorageService.java
└── ResumeParseService.java
```

### 7.6 service.impl 包

```text
service.impl
├── AuthServiceImpl.java
├── UserServiceImpl.java
├── ResumeServiceImpl.java
├── JobServiceImpl.java
├── AnalysisServiceImpl.java
├── ApplicationServiceImpl.java
├── AiServiceImpl.java
├── LocalFileStorageServiceImpl.java
└── ResumeParseServiceImpl.java
```

### 7.7 mapper 包

```text
mapper
├── UserMapper.java
├── ResumeMapper.java
├── JobDescriptionMapper.java
├── AnalysisReportMapper.java
└── ApplicationRecordMapper.java
```

### 7.8 entity 包

```text
entity
├── User.java
├── Resume.java
├── JobDescription.java
├── AnalysisReport.java
└── ApplicationRecord.java
```

### 7.9 dto 包

建议按模块分包：

```text
dto
├── auth
├── user
├── resume
├── job
├── analysis
└── application
```

### 7.10 vo 包

建议按模块分包：

```text
vo
├── auth
├── user
├── resume
├── job
├── analysis
└── application
```

### 7.11 enums 包

```text
enums
├── UserRoleEnum.java
├── ApplicationStatusEnum.java
├── MatchLevelEnum.java
├── FileTypeEnum.java
└── ParseStatusEnum.java
```

### 7.12 exception 包

```text
exception
├── GlobalExceptionHandler.java
├── BusinessException.java
├── UnauthorizedException.java
├── ForbiddenException.java
├── ResourceNotFoundException.java
├── FileParseException.java
└── AiServiceException.java
```

### 7.13 util 包

```text
util
├── SecurityUtils.java
├── JsonUtils.java
├── FileUtils.java
├── PromptUtils.java
└── DateTimeUtils.java
```

---

## 8. Gradle 依赖设计

### 8.1 build.gradle 示例

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.internpilot'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
    targetCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Spring Security
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // Validation
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // MySQL Driver
    runtimeOnly 'com.mysql:mysql-connector-j'

    // MyBatis Plus
    implementation 'com.baomidou:mybatis-plus-spring-boot3-starter:3.5.9'

    // Knife4j / Swagger
    implementation 'com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:4.5.0'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'

    // PDFBox
    implementation 'org.apache.pdfbox:pdfbox:2.0.31'

    // Apache POI for DOCX
    implementation 'org.apache.poi:poi-ooxml:5.2.5'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 8.2 依赖说明

| 依赖 | 作用 |
|---|---|
| spring-boot-starter-web | 提供 Controller、REST API、Tomcat |
| spring-boot-starter-security | 认证授权 |
| spring-boot-starter-validation | 参数校验 |
| spring-boot-starter-data-redis | Redis 操作 |
| mysql-connector-j | MySQL 驱动 |
| mybatis-plus-spring-boot3-starter | MyBatis Plus |
| knife4j-openapi3-jakarta | Swagger / Knife4j 文档 |
| jjwt | JWT 生成和解析 |
| pdfbox | PDF 简历解析 |
| poi-ooxml | DOCX 简历解析 |
| lombok | 简化实体类代码 |
| spring-boot-starter-test | 单元测试 |
| spring-security-test | Security 测试 |

---

## 9. settings.gradle 设计

```groovy
pluginManagement {
    repositories {
        maven { url = 'https://maven.aliyun.com/repository/gradle-plugin' }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = 'https://maven.aliyun.com/repository/public' }
        mavenCentral()
    }
}

rootProject.name = 'intern-pilot-backend'
```

如果不想使用阿里云镜像，也可以简化为：

```groovy
rootProject.name = 'intern-pilot-backend'
```

---

## 10. application.yml 配置设计

### 10.1 主配置文件

路径：

```text
src/main/resources/application.yml
```

内容：

```yaml
server:
  port: 8080

spring:
  application:
    name: intern-pilot-backend

  profiles:
    active: dev

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mybatis-plus:
  mapper-locations: classpath:/mapper/*.xml
  type-aliases-package: com.internpilot.entity
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

knife4j:
  enable: true
```

### 10.2 开发环境配置文件

路径：

```text
src/main/resources/application-dev.yml
```

内容：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/intern_pilot?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms

jwt:
  secret: intern-pilot-dev-secret-key-change-this-to-a-long-random-string
  expiration: 86400000

file:
  upload-dir: uploads/resumes
  max-size: 10485760
  allowed-types:
    - pdf
    - docx

ai:
  provider: deepseek
  api-key: ${AI_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  timeout: 60000
```

### 10.3 示例配置文件

路径：

```text
src/main/resources/application-example.yml
```

用途：

1. 给别人看配置格式；
2. 可以提交到 GitHub；
3. 不包含真实密码和真实 API Key。

内容：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/intern_pilot?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_mysql_username
    password: your_mysql_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      database: 0

jwt:
  secret: your_jwt_secret
  expiration: 86400000

file:
  upload-dir: uploads/resumes

ai:
  provider: deepseek
  api-key: ${AI_API_KEY}
  base-url: https://api.deepseek.com
  model: deepseek-chat
```

---

## 11. .gitignore 设计

后端 `.gitignore` 建议包含：

```gitignore
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

### IntelliJ IDEA ###
.idea/
*.iws
*.iml
*.ipr
out/

### VS Code ###
.vscode/

### Logs ###
logs/
*.log

### OS ###
.DS_Store
Thumbs.db

### Upload files ###
uploads/

### Env files ###
.env
application-local.yml
application-prod.yml

### Secret files ###
*.key
*.pem
```

注意：

1. `application-dev.yml` 如果包含真实数据库密码，不建议提交；
2. 更规范做法是提交 `application-example.yml`，本地自己创建 `application-dev.yml`，并在 `.gitignore` 排除真实配置；
3. 早期个人项目为了方便，也可以暂时提交 dev 配置，只是不要放真实 AI Key。

---

## 12. 主启动类设计

路径：

```text
src/main/java/com/internpilot/InternPilotApplication.java
```

代码：

```java
package com.internpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.internpilot.mapper")
@SpringBootApplication
public class InternPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternPilotApplication.class, args);
    }
}
```

说明：

1. `@SpringBootApplication` 启动 Spring Boot；
2. `@MapperScan` 扫描 MyBatis Mapper；
3. 包名统一使用 `com.internpilot`。

---

## 13. 统一响应结构设计

### 13.1 ResultCode

路径：

```text
src/main/java/com/internpilot/common/ResultCode.java
```

代码：

```java
package com.internpilot.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),

    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),

    INTERNAL_ERROR(500, "系统内部错误"),

    AI_SERVICE_ERROR(600, "AI 服务异常"),
    FILE_PROCESS_ERROR(700, "文件处理异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

### 13.2 Result

路径：

```text
src/main/java/com/internpilot/common/Result.java
```

代码：

```java
package com.internpilot.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }
}
```

### 13.3 PageResult

路径：

```text
src/main/java/com/internpilot/common/PageResult.java
```

代码：

```java
package com.internpilot.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;

    private Long total;

    private Long pageNum;

    private Long pageSize;

    private Long pages;
}
```

---

## 14. 统一异常处理设计

### 14.1 BusinessException

路径：

```text
src/main/java/com/internpilot/exception/BusinessException.java
```

代码：

```java
package com.internpilot.exception;

import com.internpilot.common.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
```

### 14.2 FileParseException

路径：

```text
src/main/java/com/internpilot/exception/FileParseException.java
```

代码：

```java
package com.internpilot.exception;

import com.internpilot.common.ResultCode;

public class FileParseException extends BusinessException {

    public FileParseException(String message) {
        super(ResultCode.FILE_PROCESS_ERROR, message);
    }
}
```

### 14.3 AiServiceException

路径：

```text
src/main/java/com/internpilot/exception/AiServiceException.java
```

代码：

```java
package com.internpilot.exception;

import com.internpilot.common.ResultCode;

public class AiServiceException extends BusinessException {

    public AiServiceException(String message) {
        super(ResultCode.AI_SERVICE_ERROR, message);
    }
}
```

### 14.4 GlobalExceptionHandler

路径：

```text
src/main/java/com/internpilot/exception/GlobalExceptionHandler.java
```

代码：

```java
package com.internpilot.exception;

import com.internpilot.common.Result;
import com.internpilot.common.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数校验失败"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        log.warn("参数校验异常：{}", message);
        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数绑定失败"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        log.warn("参数绑定异常：{}", message);
        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("参数约束异常：{}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误：{}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
```

---

## 15. 健康检查接口设计

初始化阶段建议先写一个简单接口，用于确认项目启动成功。

路径：

```text
src/main/java/com/internpilot/controller/HealthController.java
```

代码：

```java
package com.internpilot.controller;

import com.internpilot.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Result<String> health() {
        return Result.success("InternPilot backend is running");
    }
}
```

访问：

```http
GET http://localhost:8080/api/health
```

期望返回：

```json
{
  "code": 200,
  "message": "success",
  "data": "InternPilot backend is running"
}
```

---

## 16. Swagger / Knife4j 配置设计

### 16.1 SwaggerConfig

路径：

```text
src/main/java/com/internpilot/config/SwaggerConfig.java
```

代码：

```java
package com.internpilot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI internPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InternPilot API 文档")
                        .description("面向大学生的 AI 实习投递与简历优化平台接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("InternPilot")
                                .email("admin@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
```

### 16.2 访问地址

项目启动后访问：

```text
http://localhost:8080/doc.html
```

或：

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 17. MyBatis Plus 配置设计

### 17.1 MyBatisPlusConfig

路径：

```text
src/main/java/com/internpilot/config/MyBatisPlusConfig.java
```

代码：

```java
package com.internpilot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(100L);

        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
```

---

## 18. Redis 配置设计

### 18.1 RedisConfig

路径：

```text
src/main/java/com/internpilot/config/RedisConfig.java
```

代码：

```java
package com.internpilot.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

---

## 19. 测试数据库连接

### 19.1 创建测试实体

初始化阶段可以先不创建全部业务实体。但为了测试 MySQL，可以先创建 User 实体和 UserMapper。

路径：

```text
src/main/java/com/internpilot/entity/User.java
```

代码：

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

### 19.2 创建 UserMapper

路径：

```text
src/main/java/com/internpilot/mapper/UserMapper.java
```

代码：

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

### 19.3 创建测试接口

只用于初始化阶段测试数据库，后续可以删除。

路径：

```text
src/main/java/com/internpilot/controller/TestController.java
```

代码：

```java
package com.internpilot.controller;

import com.internpilot.common.Result;
import com.internpilot.entity.User;
import com.internpilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final UserMapper userMapper;

    @GetMapping("/api/test/db")
    public Result<Long> testDb() {
        Long count = userMapper.selectCount(null);
        return Result.success(count);
    }
}
```

访问：

```http
GET http://localhost:8080/api/test/db
```

如果返回用户数量，说明 MySQL 连接成功。

---

## 20. 测试 Redis 连接

### 20.1 创建 Redis 测试接口

初始化阶段可以创建临时接口，后续删除。

```java
package com.internpilot.controller;

import com.internpilot.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/api/test/redis")
    public Result<Object> testRedis() {
        redisTemplate.opsForValue().set("internpilot:test", "redis ok");
        Object value = redisTemplate.opsForValue().get("internpilot:test");
        return Result.success(value);
    }
}
```

访问：

```http
GET http://localhost:8080/api/test/redis
```

期望返回：

```json
{
  "code": 200,
  "message": "success",
  "data": "redis ok"
}
```

---

## 21. 初始化 SQL 文件设计

路径：

```text
src/main/resources/sql/init.sql
```

内容可以直接放数据库设计文档中的五张核心表 SQL。

第一阶段建议至少包含：

1. `user`；
2. `resume`；
3. `job_description`；
4. `analysis_report`；
5. `application_record`。

可以先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

然后执行建表 SQL。

---

## 22. 本地启动前准备

### 22.1 环境要求

| 环境 | 版本 |
|---|---|
| JDK | 17 |
| Gradle | 使用项目自带 Wrapper |
| MySQL | 8.x |
| Redis | 7.x |
| IDEA | 2023+ 推荐 |

### 22.2 启动 MySQL

确认本地 MySQL 已启动。

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

执行：

```text
src/main/resources/sql/init.sql
```

### 22.3 启动 Redis

Windows 可以使用：

1. Redis for Windows；
2. Docker Redis；
3. WSL Redis。

Linux / macOS 可以使用：

```bash
redis-server
```

测试 Redis：

```bash
redis-cli ping
```

期望返回：

```text
PONG
```

---

## 23. 启动命令

### 23.1 Windows PowerShell

在后端项目目录下执行：

```powershell
./gradlew bootRun
```

如果 PowerShell 不识别，可以使用：

```powershell
.\gradlew.bat bootRun
```

### 23.2 Linux / macOS

```bash
./gradlew bootRun
```

### 23.3 IDEA 启动

直接运行：

```text
InternPilotApplication
```

---

## 24. 启动成功验收

### 24.1 控制台检查

看到类似日志：

```text
Tomcat started on port 8080
Started InternPilotApplication
```

表示项目启动成功。

### 24.2 健康检查接口

访问：

```text
http://localhost:8080/api/health
```

期望返回：

```json
{
  "code": 200,
  "message": "success",
  "data": "InternPilot backend is running"
}
```

### 24.3 Swagger 文档

访问：

```text
http://localhost:8080/doc.html
```

如果能打开 Knife4j 页面，说明接口文档配置成功。

### 24.4 MySQL 测试接口

访问：

```text
http://localhost:8080/api/test/db
```

如果返回数字，说明数据库连接成功。

### 24.5 Redis 测试接口

访问：

```text
http://localhost:8080/api/test/redis
```

如果返回：

```text
"redis ok"
```

说明 Redis 连接成功。

---

## 25. 常见问题与解决方案

### 25.1 Java 版本错误

问题：

```text
Spring Boot 3 requires Java 17 or later
```

解决：

1. 检查 IDEA Project SDK 是否为 Java 17；
2. 检查 `JAVA_HOME` 是否指向 JDK 17。

PowerShell 查看：

```powershell
java -version
```

### 25.2 MySQL 连接失败

常见原因：

1. MySQL 没启动；
2. 数据库名写错；
3. 用户名密码错误；
4. 端口不是 3306；
5. 没创建 `intern_pilot` 数据库。

解决：

```sql
SHOW DATABASES;
```

确认存在：

```text
intern_pilot
```

### 25.3 Redis 连接失败

常见原因：

1. Redis 没启动；
2. 端口不是 6379；
3. Docker Redis 没映射端口；
4. Redis 配置密码但 `application.yml` 没写。

测试：

```bash
redis-cli ping
```

### 25.4 Knife4j 页面打不开

检查：

1. 是否引入 Knife4j 依赖；
2. Spring Boot 版本和 Knife4j 版本是否兼容；
3. 是否被 Spring Security 拦截。

后续加 Spring Security 后，需要放行：

```text
/doc.html
/swagger-ui/**
/v3/api-docs/**
/webjars/**
```

### 25.5 Lombok 不生效

解决：

1. IDEA 安装 Lombok 插件；
2. 开启 Annotation Processing。

路径：

```text
Settings
  ↓
Build, Execution, Deployment
  ↓
Compiler
  ↓
Annotation Processors
  ↓
Enable annotation processing
```

### 25.6 PowerShell curl 问题

PowerShell 中 `curl` 可能是 `Invoke-WebRequest` 的别名。

建议使用：

```powershell
curl.exe
```

或者：

```powershell
Invoke-RestMethod
```

---

## 26. 第一次提交建议

初始化后第一次 Git 提交内容：

```text
backend/intern-pilot-backend
docs/10-backend-initialization.md
```

建议 commit 信息：

```bash
git add .
git commit -m "feat: initialize Spring Boot backend project"
```

如果只是文档：

```bash
git add docs/10-backend-initialization.md
git commit -m "docs: add backend initialization design"
```

---

## 27. 初始化阶段验收清单

### 27.1 工程结构

- [ ] 创建 `backend/intern-pilot-backend`
- [ ] 设置包名 `com.internpilot`
- [ ] 创建基础包结构
- [ ] 创建主启动类
- [ ] 创建 `.gitignore`

### 27.2 配置文件

- [ ] 创建 `application.yml`
- [ ] 创建 `application-dev.yml`
- [ ] 创建 `application-example.yml`
- [ ] 配置 MySQL
- [ ] 配置 Redis
- [ ] 配置文件上传限制
- [ ] 配置 JWT 参数
- [ ] 配置 AI 参数占位

### 27.3 基础能力

- [ ] 统一响应 `Result`
- [ ] 错误码 `ResultCode`
- [ ] 分页响应 `PageResult`
- [ ] 业务异常 `BusinessException`
- [ ] 全局异常处理 `GlobalExceptionHandler`
- [ ] Swagger / Knife4j 配置
- [ ] MyBatis Plus 分页配置
- [ ] RedisTemplate 配置

### 27.4 测试验证

- [ ] 项目能正常启动
- [ ] `/api/health` 能访问
- [ ] `/doc.html` 能访问
- [ ] MySQL 连接成功
- [ ] Redis 连接成功
- [ ] `Gradle test` 通过

---

## 28. 后端初始化结论

InternPilot 后端工程初始化阶段的核心任务不是实现复杂业务，而是先建立一个清晰、稳定、规范的 Spring Boot 后端基础。

初始化完成后，项目应该具备：

1. Spring Boot 启动能力；
2. MySQL 连接能力；
3. Redis 连接能力；
4. 统一响应能力；
5. 统一异常处理能力；
6. Swagger 文档能力；
7. 清晰包结构；
8. 基础配置管理。

这些能力完成后，后续就可以按模块逐步开发：

```text
用户注册登录
  ↓
JWT 鉴权
  ↓
简历上传解析
  ↓
岗位 JD 管理
  ↓
AI 分析
  ↓
投递记录管理
```

第一阶段建议严格按这个顺序推进，不要一开始就做前端，也不要一开始就做复杂 AI 功能。先把后端地基打稳，后面的模块开发会更顺。
