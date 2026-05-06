# InternPilot 工程化增强文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的工程化增强方案，包括配置安全、测试接口清理、README 更新、Docker 部署、接口测试说明、日志规范、异常处理检查、代码质量检查、GitHub 展示优化和面试亮点整理。

当用户认证、简历上传、岗位 JD 管理、AI 分析、投递记录模块基本完成后，项目已经具备 MVP 业务闭环。下一阶段的重点不再是继续堆功能，而是让项目更像一个正式的软件工程项目。

---

## 2. 工程化增强目标

工程化增强阶段的目标是：

1. 清理开发阶段临时接口；
2. 处理敏感配置；
3. 完善 README；
4. 补充 Dockerfile；
5. 补充 docker-compose.yml；
6. 完善接口测试说明；
7. 补充核心模块测试；
8. 规范日志输出；
9. 规范异常响应；
10. 优化 GitHub 项目展示；
11. 准备简历和面试讲解材料。

---

## 3. 当前项目状态判断

目前 InternPilot 已经具备以下核心能力：

```text
用户注册登录
  ↓
JWT 鉴权
  ↓
简历上传与解析
  ↓
岗位 JD 管理
  ↓
AI 匹配分析
  ↓
分析报告保存
  ↓
投递记录管理
```

这说明项目已经从“想法阶段”进入了“可运行 MVP 阶段”。

下一步重点是：

1. 让项目更安全；
2. 让项目更稳定；
3. 让项目更容易启动；
4. 让项目更容易展示；
5. 让项目更容易写进简历；
6. 让项目更容易在面试中讲清楚。

## 4. 工程化增强任务总览

### 4.1 优先级划分

| 优先级 | 任务 | 说明 |
| --- | --- | --- |
| P0 | 清理敏感配置 | 防止密码和 API Key 泄露 |
| P0 | 清理测试接口 | 避免正式项目暴露测试端点 |
| P0 | 更新 README | GitHub 展示最重要 |
| P0 | 补充接口测试说明 | 方便面试官或自己复现 |
| P1 | Dockerfile | 支持后端容器化 |
| P1 | docker-compose.yml | 一键启动 MySQL、Redis、后端 |
| P1 | 单元测试 / 集成测试 | 提升项目可信度 |
| P1 | 日志规范 | 方便排查问题 |
| P2 | CI/CD | 后续扩展 |
| P2 | 前端部署 | 后续扩展 |

## 5. 敏感配置清理

### 5.1 问题说明

项目中常见敏感配置包括：

1. MySQL 用户名；
2. MySQL 密码；
3. Redis 密码；
4. JWT Secret；
5. AI API Key；
6. 真实服务器地址；
7. 个人简历文件；
8. 上传文件目录内容。

这些内容不应该直接提交到 GitHub。

### 5.2 application-dev.yml 修改建议

原始写法可能是：

```yaml
spring:
  datasource:
    username: root
    password: 123456

ai:
  api-key: sk-xxxx
```

推荐改为：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:intern_pilot}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}
      password: ${REDIS_PASSWORD:}

jwt:
  secret: ${JWT_SECRET:intern-pilot-dev-secret-key-change-this-to-a-long-random-string}
  expiration: ${JWT_EXPIRATION:86400000}

ai:
  provider: ${AI_PROVIDER:deepseek}
  api-key: ${AI_API_KEY:}
  base-url: ${AI_BASE_URL:https://api.deepseek.com}
  model: ${AI_MODEL:deepseek-chat}
  timeout: ${AI_TIMEOUT:60000}
```

### 5.3 application-example.yml

建议保留一个示例配置文件，提交到 GitHub：

路径：

`backend/intern-pilot-backend/src/main/resources/application-example.yml`

内容：

```yaml
server:
  port: 8080

spring:
  application:
    name: intern-pilot-backend

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
      password:

jwt:
  secret: your_jwt_secret_at_least_32_chars
  expiration: 86400000

file:
  upload-dir: uploads/resumes
  max-size: 10485760
  allowed-types:
    - pdf
    - docx

ai:
  provider: deepseek
  api-key: ${AI_API_KEY}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  timeout: 60000
```

### 5.4 .gitignore 增强

后端 `.gitignore` 建议包含：

```gitignore
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

.idea/
*.iws
*.iml
*.ipr
out/

.vscode/

logs/
*.log

.DS_Store
Thumbs.db

uploads/
*.pdf
*.docx

.env
.env.*
application-local.yml
application-prod.yml

*.key
*.pem
```

如果你的 `application-dev.yml` 里没有真实密码，可以保留提交。

如果里面包含真实密码，建议也加入：

```gitignore
application-dev.yml
```

并只提交：

```text
application-example.yml
```

## 6. 清理测试接口

### 6.1 需要检查的临时接口

开发阶段可能存在：

1. `TestController.java`
2. `RedisTestController.java`
3. `/api/test/db`
4. `/api/test/redis`

这些接口用于开发期间验证 MySQL 和 Redis 是否连接成功，但不适合长期暴露在正式项目中。

### 6.2 方案一：直接删除

如果项目已经稳定，推荐直接删除：

```text
src/main/java/com/internpilot/controller/TestController.java
src/main/java/com/internpilot/controller/RedisTestController.java
```

优点：

1. 项目更干净；
2. 不暴露测试端点；
3. 更适合 GitHub 展示；
4. 减少安全风险。

### 6.3 方案二：仅 dev 环境启用

如果你还想保留测试接口，可以加：

```java
@Profile("dev")
```

示例：

```java
@Profile("dev")
@RestController
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/api/test/redis")
    public Result<Object> testRedis() {
        redisTemplate.opsForValue().set("internpilot:test", "redis ok");
        return Result.success(redisTemplate.opsForValue().get("internpilot:test"));
    }
}
```

### 6.4 方案三：改成健康检查接口

可以保留一个正式健康检查接口：

```http
GET /api/health
```

只返回：

```json
{
  "code": 200,
  "message": "success",
  "data": "InternPilot backend is running"
}
```

不要暴露数据库数量、Redis Key 等内部信息。

## 7. README 更新方案

### 7.1 README 的作用

README 是 GitHub 项目的门面。

面试官或招聘方打开项目后，最先看的通常就是：

```text
README.md
```

所以 README 需要清楚说明：

1. 项目是什么；
2. 解决什么问题；
3. 技术栈是什么；
4. 已完成功能有哪些；
5. 如何启动；
6. 如何测试；
7. 项目亮点是什么；
8. 后续规划是什么。

### 7.2 README 推荐结构

```markdown
# InternPilot

面向大学生的 AI 实习投递与简历优化平台。

## 项目简介

## 核心功能

## 技术栈

## 系统架构

## 项目结构

## 快速启动

## 环境变量说明

## API 文档

## 核心业务流程

## 数据库设计

## 测试说明

## 项目亮点

## 后续规划

## 作者说明
```

### 7.3 README 项目简介示例

```markdown
# InternPilot

InternPilot 是一个面向大学生实习求职场景的 AI 实习投递与简历优化平台。

系统支持用户上传简历、保存岗位 JD、调用大语言模型分析简历与岗位匹配度，并生成简历优化建议、技能缺口分析和面试准备建议。同时，系统支持实习投递记录管理，帮助用户跟踪投递状态、面试时间和复盘内容。

该项目基于 Spring Boot 3、Spring Security、JWT、MySQL、Redis 和大语言模型 API 构建，重点展示 Java 后端开发、权限认证、文件解析、AI 应用集成、缓存优化和工程化设计能力。
```

### 7.4 README 核心功能示例

```markdown
## 核心功能

- 用户注册与登录
- JWT 无状态认证
- Spring Security 接口鉴权
- PDF / DOCX 简历上传与解析
- 岗位 JD 创建、查询、修改、删除
- AI 简历岗位匹配分析
- 匹配分数、优势、短板、技能缺口生成
- 简历优化建议生成
- 面试准备建议生成
- Redis 缓存 AI 分析结果
- 分析报告历史查询
- 实习投递记录管理
- 投递状态跟踪
- Swagger / Knife4j 接口文档
```

### 7.5 README 技术栈示例

```markdown
## 技术栈

### 后端

- Java 17
- Spring Boot 3
- Spring Security 6
- JWT
- MyBatis Plus
- MySQL 8
- Redis 7
- Knife4j / Swagger
- Apache PDFBox
- Apache POI
- Lombok
- Gradle

### AI

- DeepSeek API / OpenAI API / Qwen API 可扩展
- Prompt Engineering
- JSON 结构化输出
- Redis 缓存 AI 分析结果

### 工程化

- Docker
- Docker Compose
- 统一响应结构
- 统一异常处理
- 参数校验
- 分层架构
```

### 7.6 README 项目亮点示例

```markdown
## 项目亮点

1. 完整的实习求职业务闭环  
   从简历上传、岗位管理、AI 分析到投递记录管理，形成完整流程。

2. Spring Security + JWT 无状态认证  
   支持前后端分离场景下的登录认证和接口鉴权。

3. 严格的数据权限控制  
   所有简历、岗位、报告和投递记录均基于 userId 做数据隔离，防止越权访问。

4. AI 应用集成  
   通过 Prompt 将简历文本与岗位 JD 输入大语言模型，生成结构化匹配分析报告。

5. Redis 缓存优化  
   对相同简历和岗位组合的 AI 分析结果进行缓存，降低重复调用成本。

6. 文件上传与解析  
   支持 PDF 和 DOCX 简历上传，使用 PDFBox 和 POI 提取文本内容。

7. 工程化设计  
   使用统一响应、统一异常处理、参数校验、Swagger 文档和模块化包结构。
```

## 8. Dockerfile 设计

### 8.1 后端 Dockerfile 路径

建议放在：

```text
backend/intern-pilot-backend/Dockerfile
```

### 8.2 Dockerfile 内容

```dockerfile
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 8.3 构建镜像

在后端目录执行：

```powershell
docker build -t intern-pilot-backend:latest .
```

### 8.4 运行容器

```powershell
docker run -p 8080:8080 `
  -e MYSQL_HOST=host.docker.internal `
  -e MYSQL_PORT=3306 `
  -e MYSQL_DATABASE=intern_pilot `
  -e MYSQL_USERNAME=root `
  -e MYSQL_PASSWORD=your_password `
  -e REDIS_HOST=host.docker.internal `
  -e AI_API_KEY=your_api_key `
  intern-pilot-backend:latest
```

## 9. Docker Compose 设计

### 9.1 docker-compose.yml 路径

建议放在：

```text
deploy/docker-compose.yml
```

### 9.2 docker-compose.yml 内容

```yaml
version: "3.9"

services:
  mysql:
    image: mysql:8.0
    container_name: internpilot-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: intern_pilot
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - internpilot-mysql-data:/var/lib/mysql
      - ../backend/intern-pilot-backend/src/main/resources/sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    command:
      --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7.2
    container_name: internpilot-redis
    restart: always
    ports:
      - "6379:6379"
    volumes:
      - internpilot-redis-data:/data

  backend:
    build:
      context: ../backend/intern-pilot-backend
      dockerfile: Dockerfile
    container_name: internpilot-backend
    restart: always
    depends_on:
      - mysql
      - redis
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: intern_pilot
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: root
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: intern-pilot-docker-secret-key-change-this-to-a-long-random-string
      AI_API_KEY: ${AI_API_KEY}
      AI_PROVIDER: deepseek
      AI_BASE_URL: https://api.deepseek.com
      AI_MODEL: deepseek-chat
    volumes:
      - internpilot-uploads:/app/uploads

volumes:
  internpilot-mysql-data:
  internpilot-redis-data:
  internpilot-uploads:
```

### 9.3 启动命令

在项目根目录或 `deploy` 目录执行：

```powershell
cd deploy
docker compose up -d
```

查看日志：

```powershell
docker compose logs -f backend
```

停止：

```powershell
docker compose down
```

停止并删除数据卷：

```powershell
docker compose down -v
```

### 9.4 Docker Compose 验收

启动后访问：

```text
http://localhost:8080/api/health
```

访问接口文档：

```text
http://localhost:8080/doc.html
```

如果两个地址都能打开，说明 Docker Compose 基础部署成功。

## 10. SQL 初始化文件整理

### 10.1 init.sql 路径

建议放在：

```text
backend/intern-pilot-backend/src/main/resources/sql/init.sql
```

### 10.2 init.sql 内容要求

至少包含以下表：

1. `user`
2. `resume`
3. `job_description`
4. `analysis_report`
5. `application_record`

可选包含：

1. `ai_call_log`
2. `prompt_template`
3. `application_status_log`

### 10.3 注意事项

`init.sql` 里不要插入真实用户密码。

如果要插入管理员用户，密码必须是 BCrypt 后的密文。

示例：

```sql
INSERT INTO user (
    username,
    password,
    email,
    role,
    enabled
) VALUES (
    'admin',
    '$2a$10$replace_with_bcrypt_password',
    'admin@example.com',
    'ADMIN',
    1
);
```

## 11. 接口测试文档

### 11.1 新增文档

建议新增：

```text
docs/17-api-test-guide.md
```

### 11.2 测试流程总览

接口测试顺序应该是：

1. 健康检查
2. 用户注册
3. 用户登录
4. 查询当前用户
5. 上传简历
6. 查询简历列表
7. 创建岗位 JD
8. 查询岗位列表
9. 发起 AI 分析
10. 查询分析报告
11. 创建投递记录
12. 修改投递状态
13. 查询投递详情

### 11.3 PowerShell 测试脚本建议

可以在文档中放完整 PowerShell 测试流程。

例如登录：

```powershell
$loginBody = @{
  username = "wan"
  password = "123456"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $loginBody

$token = $loginResponse.data.token
```

后续统一使用：

```powershell
$headers = @{ Authorization = "Bearer $token" }
```

## 12. 测试增强建议

### 12.1 当前测试重点

建议优先补充以下测试：

| 模块 | 测试重点 |
| --- | --- |
| AuthControllerTest | 注册、登录、错误密码 |
| ResumeServiceTest | 文件类型校验、用户权限 |
| JobServiceTest | 创建、查询、修改、删除 |
| AnalysisServiceTest | Mock AI 分析、缓存命中 |
| ApplicationServiceTest | 创建投递、修改状态、非法状态 |
| SecurityIntegrationTest | 401、403、Token 鉴权 |

### 12.2 测试分层建议

第一阶段不用追求 100% 覆盖率。

建议分三类：

1. Controller 层测试：验证接口请求响应
2. Service 层测试：验证业务逻辑
3. Security 集成测试：验证 401 / 403 / Token 鉴权

### 12.3 Mock AI 测试

AI 分析模块测试时，不建议调用真实大模型。

应该使用：

```text
MockAiClient
```

原因：

1. 测试稳定；
2. 不消耗 API 额度；
3. 不受网络影响；
4. 返回结果可控。

### 12.4 Gradle 测试命令

运行全部测试：

```powershell
./gradlew test
```

运行某个测试类：

```powershell
./gradlew test --tests AuthControllerTest
```

Windows PowerShell：

```powershell
.\gradlew.bat test
```

## 13. 日志规范

### 13.1 建议记录的日志

可以记录：

1. 用户登录成功 / 失败；
2. 简历上传成功；
3. 文件解析失败；
4. AI 分析开始；
5. AI 分析是否命中缓存；
6. AI 调用耗时；
7. 投递状态修改；
8. 业务异常简要信息。

### 13.2 不应该记录的日志

不要记录：

1. 用户密码；
2. 完整 JWT Token；
3. AI API Key；
4. 完整简历文本；
5. 完整岗位 JD；
6. 用户身份证号、手机号等敏感信息；
7. 生产环境完整异常堆栈对外返回。

### 13.3 日志示例

```java
log.info("AI analysis started, userId={}, resumeId={}, jobId={}", userId, resumeId, jobId);
log.info("AI analysis cache hit, userId={}, resumeId={}, jobId={}", userId, resumeId, jobId);
log.warn("Resume parse failed, userId={}, fileName={}", userId, originalFileName);
```

不要这样写：

```java
log.info("resumeText={}", resume.getParsedText());
log.info("apiKey={}", aiProperties.getApiKey());
```

## 14. 异常处理检查

### 14.1 统一响应格式

所有异常应该统一返回：

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

不要出现默认 Spring Boot 错误页：

```text
Whitelabel Error Page
```

### 14.2 常见错误码检查

| 场景 | 推荐 code |
| --- | --- |
| 参数错误 | 400 |
| 未登录 | 401 |
| 无权限 | 403 |
| 资源不存在 | 404 |
| 数据冲突 | 409 |
| 系统异常 | 500 |
| AI 服务异常 | 600 |
| 文件处理异常 | 700 |

### 14.3 检查点

需要确认：

1. 未登录访问业务接口返回 401
2. 普通用户访问管理员接口返回 403
3. 上传非法文件返回 700
4. AI 调用失败返回 600
5. 资源不存在返回清晰错误
6. 参数校验失败返回字段提示

## 15. 安全检查清单

### 15.1 配置安全

- GitHub 中不包含真实数据库密码；
- GitHub 中不包含 AI API Key；
- GitHub 中不包含生产 JWT Secret；
- 使用环境变量读取敏感配置；
- 提供 `application-example.yml`。

### 15.2 接口安全

- 注册、登录接口放行；
- Swagger 接口放行；
- 业务接口需要登录；
- 管理员接口需要 `ADMIN`；
- 不存在未鉴权业务接口；
- 文件上传接口需要登录。

### 15.3 数据权限

- 简历查询带 `userId`；
- 岗位查询带 `userId`；
- AI 报告查询带 `userId`；
- 投递记录查询带 `userId`；
- 创建投递记录时校验 `jobId`、`resumeId`、`reportId` 属于当前用户；
- 不能通过改 URL 访问他人数据。

### 15.4 文件安全

- 限制文件大小；
- 限制文件类型；
- 不使用用户原始文件名作为存储文件名；
- `uploads` 不提交到 GitHub；
- 日志不打印完整简历文本。

## 16. GitHub 展示优化

### 16.1 推荐目录结构

```text
InternPilot
├── backend
│   └── intern-pilot-backend
├── frontend
├── docs
│   ├── 01-project-idea.md
│   ├── 02-feasibility-analysis.md
│   ├── 03-project-plan.md
│   ├── 04-technology-selection.md
│   ├── 05-system-analysis.md
│   ├── 06-requirements-analysis.md
│   ├── 07-outline-design.md
│   ├── 08-database-design.md
│   ├── 09-api-design.md
│   ├── 10-backend-initialization.md
│   ├── 11-auth-jwt-design.md
│   ├── 12-resume-upload-parse-design.md
│   ├── 13-job-description-design.md
│   ├── 14-ai-analysis-design.md
│   ├── 15-application-record-design.md
│   └── 16-engineering-enhancement.md
├── deploy
│   └── docker-compose.yml
├── README.md
└── LICENSE
```

### 16.2 建议补充 docs

建议后续再补：

```text
docs/17-api-test-guide.md
docs/18-interview-summary.md
docs/19-frontend-design.md
docs/20-deployment-guide.md
```

### 16.3 README 加截图

等前端完成后，README 中建议加入：

1. 登录页截图
2. 简历管理页截图
3. 岗位管理页截图
4. AI 分析报告页截图
5. 投递记录页截图
6. Swagger 接口文档截图

即使前端没完成，也可以先放：

1. Swagger 接口文档截图
2. 后端接口测试截图
3. 数据库表结构截图

## 17. Commit 规范

### 17.1 推荐 Commit 类型

| 类型 | 说明 |
| --- | --- |
| feat | 新功能 |
| fix | 修复问题 |
| docs | 文档 |
| refactor | 重构 |
| test | 测试 |
| chore | 工程配置 |
| style | 代码格式 |
| perf | 性能优化 |

### 17.2 示例

```powershell
git commit -m "feat: add application record module"
git commit -m "docs: add engineering enhancement guide"
git commit -m "chore: add docker compose deployment"
git commit -m "fix: remove sensitive database password"
git commit -m "test: add auth controller tests"
```

## 18. 分支建议

如果你一个人开发，可以使用简单分支策略：

```text
main
dev
feature/auth
feature/resume
feature/job
feature/analysis
feature/application
```

如果项目已经接近展示阶段：

```text
main：稳定可展示版本
dev：日常开发版本
```

## 19. 面试材料整理

### 19.1 新增文档

建议新增：

```text
docs/18-interview-summary.md
```

### 19.2 面试讲解结构

可以按这个顺序讲项目：

1. 项目背景
2. 解决的问题
3. 系统整体架构
4. 核心业务流程
5. 权限认证设计
6. 简历上传与解析设计
7. AI 分析模块设计
8. Redis 缓存优化
9. 数据权限控制
10. 工程化与部署
11. 遇到的问题和解决方案
12. 后续优化方向

### 19.3 简历项目描述示例

```markdown
InternPilot：面向大学生的 AI 实习投递与简历优化平台

项目描述：
基于 Spring Boot 3 + Spring Security + JWT + MySQL + Redis + 大语言模型 API 构建的实习求职辅助平台，支持简历上传解析、岗位 JD 管理、AI 匹配分析、简历优化建议生成和投递记录管理，形成从岗位分析到投递跟踪的完整闭环。

技术栈：
Java 17、Spring Boot、Spring Security、JWT、MyBatis Plus、MySQL、Redis、PDFBox、Apache POI、Knife4j、Docker Compose

核心职责：
- 设计并实现用户注册登录和 JWT 无状态认证，完成接口鉴权与 401/403 统一处理；
- 实现 PDF / DOCX 简历上传与文本解析，使用 userId 做数据隔离，防止越权访问；
- 设计岗位 JD 管理模块，支持岗位创建、搜索、修改和逻辑删除；
- 集成大语言模型 API，构造 Prompt 分析简历与岗位匹配度，生成匹配分数、技能缺口和优化建议；
- 使用 Redis 缓存相同简历和岗位组合的 AI 分析结果，降低重复调用成本；
- 实现投递记录模块，支持投递状态跟踪、面试时间记录和复盘备注；
- 使用统一响应结构、全局异常处理、参数校验和 Swagger 文档提升项目工程化程度。
```

## 20. 前端开发前置条件

在进入前端前，需要确认：

1. 后端接口基本稳定
2. Swagger 文档可访问
3. 核心接口均已测试
4. 返回字段基本确定
5. 认证流程稳定
6. 跨域配置完成

### 20.1 前端需要的接口

前端第一版依赖这些接口：

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/user/me

POST /api/resumes/upload
GET  /api/resumes
GET  /api/resumes/{id}
DELETE /api/resumes/{id}
PUT /api/resumes/{id}/default

POST /api/jobs
GET  /api/jobs
GET  /api/jobs/{id}
PUT  /api/jobs/{id}
DELETE /api/jobs/{id}

POST /api/analysis/match
GET  /api/analysis/reports
GET  /api/analysis/reports/{id}

POST /api/applications
GET  /api/applications
GET  /api/applications/{id}
PUT  /api/applications/{id}/status
PUT  /api/applications/{id}/note
DELETE /api/applications/{id}
```

## 21. 跨域配置

### 21.1 前后端分离需要 CORS

如果前端运行在：

```text
http://localhost:5173
```

后端运行在：

```text
http://localhost:8080
```

就需要配置跨域。

### 21.2 CorsConfig 示例

路径：

`src/main/java/com/internpilot/config/CorsConfig.java`

```java
package com.internpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
```

### 21.3 SecurityConfig 中启用 CORS

在 `SecurityConfig` 中加入：

```java
.cors(cors -> {})
```

示例：

```java
http
    .cors(cors -> {})
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    );
```

## 22. 工程化验收清单

### 22.1 配置安全

- AI API Key 不在 GitHub；
- MySQL 密码不在 GitHub；
- JWT Secret 使用环境变量；
- `uploads` 目录被 `.gitignore` 忽略；
- 提供 `application-example.yml`。

### 22.2 接口清理

- 删除或限制 `/api/test/db`；
- 删除或限制 `/api/test/redis`；
- 保留 `/api/health`；
- Swagger 可正常访问。

### 22.3 README

- 项目简介清楚；
- 技术栈完整；
- 功能模块完整；
- 启动步骤清楚；
- 环境变量说明清楚；
- API 文档地址清楚；
- 项目亮点明确；
- 后续规划合理。

### 22.4 Docker

- 后端 Dockerfile 可构建；
- `docker-compose.yml` 可启动 MySQL；
- `docker-compose.yml` 可启动 Redis；
- `docker-compose.yml` 可启动后端；
- `/api/health` 可访问；
- `/doc.html` 可访问。

### 22.5 测试

- 注册登录测试通过；
- JWT 鉴权测试通过；
- 简历上传测试通过；
- 岗位管理测试通过；
- AI 分析 Mock 测试通过；
- 投递记录测试通过；
- 未登录访问返回 401；
- 越权访问被拦截。

## 23. 推荐执行顺序

工程化增强建议按以下顺序做：

1. 清理敏感配置；
2. 更新 `.gitignore`；
3. 删除或限制测试接口；
4. 更新 README；
5. 补充 `application-example.yml`；
6. 补充 `init.sql`；
7. 补充 Dockerfile；
8. 补充 `docker-compose.yml`；
9. 本地测试 Docker Compose；
10. 补充 API 测试文档；
11. 补充核心测试；
12. 整理面试总结文档；
13. 再进入前端页面开发。

## 24. 完成工程化后项目状态

工程化增强完成后，项目可以被描述为：

```text
一个具备完整后端 MVP、接口文档、缓存优化、AI 集成、Docker 部署能力和清晰工程结构的 Java 后端项目。
```

这时再进入前端开发会更稳。

前端只需要围绕已有接口做展示：

1. 登录注册页
2. 简历管理页
3. 岗位管理页
4. AI 分析页
5. 投递记录页
6. 数据看板页

## 25. 工程化增强结论

InternPilot 当前已经具备较完整的业务功能，但要成为适合 GitHub 展示和实习面试的项目，还需要完成工程化增强。

本阶段最重要的不是继续加新功能，而是解决以下问题：

1. 配置是否安全
2. 项目是否容易启动
3. 接口是否容易测试
4. README 是否专业
5. Docker 是否能一键运行
6. 测试是否能证明功能可靠
7. 代码是否没有明显临时痕迹

工程化增强完成后，InternPilot 将从“功能型项目”升级为“工程型项目”。
