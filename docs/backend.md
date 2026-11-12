# 后端技术栈与开发约定

`backend/` 是 InternPilot 的单模块 Java 后端，提供认证、简历、岗位、AI 分析、面试准备、推荐、投递和管理接口。本文描述当前仓库的实现及开发约定；涉及部署和数据迁移时，仍需核对目标环境。技术选型和行为以源码、配置及实际验证结果为准，不能直接套用其他项目的版本、接口前缀或数据库规则。

## 定位与边界

- 后端负责业务规则、鉴权、数据持久化、文件上传和异步任务；前端通过 `/api/...` 调用它，并通过现有 WebSocket 路径接收 AI 任务进度。
- MySQL 保存业务数据，Redis 参与缓存和任务状态；上传文件位于运行目录的 `uploads/`。修改简历、头像等流程时，数据库记录和文件状态需要一起考虑。
- AI、邮件和短信依赖外部服务及实际环境配置。测试中的 Mock 行为不代表真实服务可用。
- 本仓库没有自动执行 `sql/migration/` 脚本的迁移框架。新建环境的初始化与已有数据库的升级是两种操作，不得把初始化脚本当作生产升级脚本。

## 技术栈

| 项目 | 当前实现 | 依据与注意点 |
| --- | --- | --- |
| Java 与构建 | CI 和 Docker 使用 JDK 17；Gradle Wrapper 指向 8.10.2 | `build.gradle` 尚未声明 Java toolchain；本地运行应与 CI 的 JDK 版本对齐。 |
| Web 框架 | Spring Boot 3.3.5、Spring Web、Validation、Actuator | 版本和依赖见 [build.gradle](../backend/build.gradle)。 |
| 数据访问 | MyBatis-Plus 3.5.9、JSqlParser、MySQL；Spring 默认连接池 | 分页拦截器针对 MySQL，单次分页上限为 100。 |
| 缓存与实时通信 | Redis、Spring WebSocket | 变更缓存键、任务状态或推送时检查超时、重试和刷新恢复。 |
| 认证与授权 | Spring Security、`jjwt`、BCrypt | 实际权限由过滤链、`@PreAuthorize` 和业务资源归属检查共同决定。 |
| 接口文档 | Knife4j 4.5.0、OpenAPI | 本地入口为 `/doc.html`；文档展示不能替代接口验证。 |
| AI 与文件处理 | DeepSeek 客户端及 Mock 实现；PDFBox、Apache POI | 外部调用与文件解析需要分别验证失败和超时路径。 |
| 测试 | JUnit 5、Mockito、MockMvc、H2、Testcontainers MySQL、JaCoCo | MySQL 专有约束和并发行为使用需要 Docker 的隔离测试。 |

## 工程结构

```text
backend/
├── build.gradle                 # 依赖、测试、bootRun 环境变量读取
├── settings.gradle              # 仓库与工程名配置
├── gradlew / gradlew.bat        # Gradle Wrapper
├── gradle/wrapper/              # Wrapper 配置和 JAR
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/internpilot/
    │   │   ├── application/     # 投递模块：按业务聚合的试点
    │   │   │   ├── controller/
    │   │   │   ├── service/
    │   │   │   │   └── impl/
    │   │   │   ├── mapper/
    │   │   │   ├── entity/
    │   │   │   ├── dto/
    │   │   │   ├── vo/
    │   │   │   └── enums/
    │   │   ├── controller/、service/  # 其他业务的接口与服务
    │   │   ├── mapper/、entity/       # 其他业务的 Mapper 与实体
    │   │   ├── dto/、vo/、enums/       # 其他业务的请求、响应与枚举
    │   │   ├── common/、exception/   # 共享响应、错误码及异常处理
    │   │   ├── security/        # JWT 过滤器、401/403 处理、用户身份
    │   │   ├── config/          # 安全、分页、异步、Redis、WebSocket 等配置
    │   │   └── ai/、task/ 等    # AI 客户端、任务及补偿逻辑
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── sql/            # 初始化与人工审查的迁移脚本
    └── test/
        ├── java/com/internpilot/  # 投递专属测试位于 application/ 下
        └── resources/          # test profile 与隔离测试 SQL
```

投递是目前唯一按业务聚合的模块，负责投递记录的创建与查询、状态和优先级更新、备注、删除，以及从分析报告生成投递计划或记录。对应接口仍位于 `/api/applications`。其他业务暂沿用原有的按层分包；健康检查、全局异常处理和基础设施配置仍在原处。[启动类](../backend/src/main/java/com/internpilot/InternPilotApplication.java) 扫描原有 Mapper 包和投递 Mapper 包；[MyBatis 配置](../backend/src/main/resources/application.yml) 同时注册两处实体包的类型别名。

| 层或组件 | 主要职责 | 修改时核对 |
| --- | --- | --- |
| Controller 与 DTO | 接收请求、参数校验、调用服务、形成响应 | URL、请求格式、权限注解、HTTP 与业务错误 |
| Service | 业务规则、事务、资源所属关系和状态转换 | 重复提交、并发、跨用户访问、失败回滚 |
| Mapper 与 Entity | 查询、更新及表映射 | 过滤条件、软删除、分页、数据库约束 |
| `security/` 与 `config/` | 认证、授权、外部依赖和基础设施配置 | 401/403、公开路径、环境差异和秘密注入 |
| `ai/`、`task/`、`runner/` | AI 调用、异步状态及恢复 | 超时、重试、幂等、缓存键和进度通知 |

模块内按 Controller → Service → Mapper 的方向调用；按实际职责建包，无需为每个模块复制空目录。`common/` 只收纳确实跨模块共享的通用组件，`config/` 用于全局配置。新增跨模块调用优先使用所属模块对外提供的接口，避免直接依赖其 Mapper 或持久化实体。现阶段投递仍直接查询简历、岗位和分析报告，后台用户管理和岗位推荐仍使用投递 Mapper；这些调用保留原有的查询、权限和事务语义。新接口需同步核对前端调用方、参数校验、后端授权、错误响应和测试断言。

## 配置与本地运行

配置使用 `application.yml` 及 `dev`、`prod` 两个 profile；自动化测试另用 `src/test/resources/application-test.yml`。公共配置中的 `spring.profiles.active` 为 `dev`，生产运行必须显式选择并核对 `prod`。配置文件包含本地开发默认值，这些值不是生产凭据。

| Profile | 数据库与外部依赖 | 使用边界 |
| --- | --- | --- |
| `dev` | MySQL、Redis；`spring.sql.init.mode: always`；验证码默认为真实邮件模式 | 只连接可初始化的本地开发库，启动前确认目标库和邮件配置。 |
| `test` | H2 测试库与测试 SQL；验证码和 AI 使用 Mock；部分测试使用 Testcontainers MySQL | 不连接生产依赖；MySQL 集成测试需要可用的 Docker。 |
| `prod` | MySQL、Redis；`spring.sql.init.mode: never`；真实凭据由环境注入 | 不自动建表或迁移，升级流程需单独制定。 |

`bootRun` 在启动前读取 `backend/.env` 和 `deploy/.env` 中适用的变量；已有进程环境变量优先。该逻辑有专门的例外处理，具体以 [build.gradle](../backend/build.gradle) 为准。常见变量包括 `MYSQL_HOST`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`REDIS_HOST`、`JWT_SECRET`、`DEEPSEEK_API_KEY` 和邮件配置；示例见 [deploy/.env.example](../deploy/.env.example)。真实 `.env`、密钥和上传数据不得提交。

从仓库根目录在 Windows PowerShell 中运行：

```powershell
cd backend
.\gradlew.bat bootRun
.\gradlew.bat test --no-daemon --max-workers=1
.\gradlew.bat clean bootJar --no-daemon
```

Linux/macOS 将 `.\gradlew.bat` 换为 `./gradlew`。开发时先准备所需 MySQL、Redis 和环境变量；首次安装步骤见 [README 后端启动](../README.md#后端启动)。`bootJar` 生成可执行 JAR，但打包成功不等于目标环境已部署或验收。

## 数据库与 MyBatis-Plus 约定

- 初始化脚本是 `src/main/resources/sql/init.sql`；已有库的变更脚本在 `sql/migration/`，如 V40、V41。新增迁移应说明适用版本、已有数据检查、锁和停机影响、备份、恢复及应用回滚限制。
- 项目实体使用 `@TableName`、`@TableId`，部分实体使用 `@TableLogic`。是否软删除、主键策略和审计字段以对应实体及 SQL 为准，不能假设所有表相同。
- `application.yml` 配置了下划线字段映射和逻辑删除值；MyBatis-Plus 分页拦截器位于 [MyBatisPlusConfig.java](../backend/src/main/java/com/internpilot/config/MyBatisPlusConfig.java)。分页结果仍需明确排序和用户范围，避免重复页或越权读数。
- 简单查询可沿用现有 Mapper 与条件构造器；复杂查询应审查参数绑定、索引、分页和权限条件。当前配置声明了 `classpath:/mapper/*.xml`，但仓库目前没有对应的 XML 目录；新增 XML 时须同时核对资源位置与加载配置。
- 数据库约束是并发与数据完整性的最后防线。修改投递记录等唯一性规则时，既要验证服务层返回，也要在隔离 MySQL 上验证实际约束；H2 不能替代 MySQL 专有行为。

## 接口、响应与错误

现有接口前缀为 `/api/...`，例如 `/api/auth`、`/api/resumes`、`/api/jobs`、`/api/analysis`、`/api/applications` 和 `/api/admin`。不要从案例引入 `/api/v1`，除非同时规划并验证全部调用方的迁移。接口文档入口 `/doc.html` 和健康检查 `/api/health` 由当前安全配置放行。

[Result.java](../backend/src/main/java/com/internpilot/common/Result.java) 的响应体包含 `code`、`message`、`data`，成功业务码是 `200`，例如：

```json
{"code":200,"message":"success","data":{}}
```

`ResultCode` 定义 400、401、403、404、409、500 等业务码。当前 [GlobalExceptionHandler.java](../backend/src/main/java/com/internpilot/exception/GlobalExceptionHandler.java) 对不少业务异常返回响应体而不改变 HTTP 状态；认证入口和拒绝访问处理器则分别返回 HTTP 401、403。因此接口测试和前端调用必须同时核对 **HTTP 状态与响应体 `code`**，不能假设二者永远相同。新增错误映射应保持消息安全，不把 SQL、密钥或内部异常栈暴露给用户。

## 认证、权限与数据隔离

- 登录接口在 `/api/auth`；请求通过 `Authorization: Bearer <token>` 携带 JWT。[JwtAuthenticationFilter.java](../backend/src/main/java/com/internpilot/security/JwtAuthenticationFilter.java) 解析 token，并从当前用户信息加载权限。
- [SecurityConfig.java](../backend/src/main/java/com/internpilot/config/SecurityConfig.java) 配置无状态会话、白名单和方法级授权；密码通过 BCrypt 哈希。Controller 使用 `@PreAuthorize` 约束操作，但业务层仍须校验简历、报告、投递等记录的所属用户。
- 未认证与无权限路径分别由安全处理器返回 HTTP 401 和 403。新增端点时应为普通用户、管理员、跨用户资源和过期 token 设计验证，不能只靠前端隐藏按钮。
- 当前白名单包含 `/uploads/**` 和 `/ws/analysis/**`。修改上传或实时进度时，要重新审查文件内容、链接可见性与订阅范围；公开路径的存在不能视为资源天然可公开。
- JWT 只应承载认证必需信息，真实密钥通过环境变量注入。开发默认密钥不能用于生产，也不要把 token、验证码或密码写进日志。

## AI、异步任务与外部服务

`ai.provider` 的默认值是 `deepseek`，测试 profile 使用 Mock。场景模型、重试和超时由 `application*.yml` 的 `ai.*` 配置及客户端实现决定。调整 AI 分析、面试题、岗位推荐或 RAG 时，检查输入限制、失败降级、重试重复执行、缓存键、任务状态和 WebSocket 通知；Mock 下的通过结果不能证明真实 DeepSeek 配额、延迟或输出格式。

邮件、短信、数据库、Redis 和上传存储同样属于环境依赖。变更配置时分别说明本地、CI 和目标部署环境如何验证。生产更新应参考 [README 部署说明](../README.md#部署说明)并结合实际环境制定备份、验证和回滚步骤。

## 测试、构建与交付

后端测试在 `src/test/java/`；`src/test/resources/` 提供测试配置和 SQL。JUnit/MockMvc 用于业务、接口和权限断言；Testcontainers 用隔离 MySQL 验证数据库特性。只有放在 Gradle 测试源目录中的测试才会被当前 `test` 任务编译和执行，移动或新增测试后要检查实际测试报告。

```powershell
cd backend
.\gradlew.bat test --no-daemon --max-workers=1
.\gradlew.bat clean test jacocoTestReport --no-daemon --max-workers=1
.\gradlew.bat test --tests 'com.internpilot.util.VectorUtilsTest' --no-daemon
```

测试任务配置为结束后生成 JaCoCo 报告，HTML 位于 `backend/build/reports/jacoco/test/html/`。部分 MySQL 集成测试需要 Docker；环境不可用时应报告未覆盖范围，不把未执行测试写成通过。CI 的确切 JDK、命令和触发条件见 [工作流](../.github/workflows/ci.yml)，按改动风险选择验证见 [测试规范](test.md)。

交付时至少说明改动、运行目录与命令、测试结果和跳过项，并检查 `git diff --check`、相关接口的成功与失败路径、相对链接及敏感信息。数据库迁移、真实 AI/邮件链路和生产部署都需要独立的目标环境证据。
