# 后端技术栈与开发约定

## 工程与技术栈

后端位于 `backend/`，使用 Java 17、Spring Boot 3.3.5、Gradle Wrapper、MyBatis-Plus、MySQL、Redis、Spring Security/JWT、WebSocket 和 Knife4j。实际依赖版本以 [build.gradle](../backend/build.gradle) 为准。

代码根包为 `com.internpilot`。`controller/` 提供 REST 接口，`service/` 实现业务规则，`mapper/` 与 `entity/` 负责持久化；`dto/`、`vo/` 用于接口数据。跨层修改应检查认证、权限、数据约束与响应契约，Controller 不直接承担数据库业务逻辑。接口设计见 本地 API 设计资料。

## 配置与数据

- 配置在 `src/main/resources/application.yml`、`application-dev.yml`、`application-prod.yml`；测试配置在 `src/test/resources/application-test.yml`。本地启动默认使用 `dev` profile。
- 数据库和 Redis、JWT、邮件、短信及 AI 凭据通过环境变量提供；参考 [部署环境变量示例](../deploy/.env.example)。不要提交真实 `.env` 或把秘密放入日志、前端配置。
- SQL 位于 `src/main/resources/sql/`，包括初始化脚本和有序迁移脚本。现有生产库不能无条件重放初始化或迁移 SQL；变更前核对当前 schema、数据影响和恢复方案。[README 部署说明](../README.md#部署说明)只提供首次安装示例，存量升级需环境专属方案。
- AI 的 `mock` 模式用于 test/CI；生产配置应使用真实提供方。外部 AI、邮件与短信的行为须分别验证，单元测试通过不代表真实服务可用。

## 本地运行与验证

先准备 MySQL、Redis 和所需环境变量，具体步骤见 [README 启动章节](../README.md#后端启动)。在工程目录执行：

```powershell
cd backend
.\gradlew.bat bootRun
.\gradlew.bat test --no-daemon --max-workers=1
```

Linux/macOS 使用 `./gradlew`。后端测试位于 `src/test/java/`，使用 JUnit 5、Mockito、Spring Boot Test、MockMvc、H2；覆盖率报告由 JaCoCo 生成。按 [测试规范](test.md) 选择相关测试，并以本次执行结果为准。

## 目录与职责

| 路径（相对于后端工程） | 作用 | 修改时重点检查 |
| --- | --- | --- |
| `src/main/java/com/internpilot/controller/` | 认证、用户、简历、岗位、分析、面试、推荐、投递、反馈及管理接口 | 参数校验、HTTP/业务错误、权限、所属用户 |
| `service/`、`mapper/`、`entity/` | 业务流程、数据库访问及表映射 | 事务、重复提交、状态转换、软删除和查询范围 |
| `security/`、`config/`、`captcha/` | JWT、Spring Security、运行配置和验证码 | 401/403、角色权限、生产与测试配置差异 |
| `ai/`、`runner/`、`task/` | AI 客户端、任务补偿与后台任务 | 超时、重试、幂等、异步结果及失败恢复 |
| `src/main/resources/mapper/` | MyBatis XML | SQL 条件、参数绑定、排序和分页 |
| `src/main/resources/sql/` | 初始化和人工迁移脚本 | 现有库兼容性、唯一约束、恢复方案 |

接口路径实际采用 `/api/...`，如 `/api/auth`、`/api/resumes`、`/api/jobs`、`/api/analysis`、`/api/applications` 和 `/api/admin`；不要从模板引入新的 `/api/v1` 前缀而不同时迁移调用方。统一响应与错误码应对照当前 Controller、异常处理和前端请求封装。

## Profile 与本地开发

| Profile | 数据库初始化 | 用途与注意点 |
| --- | --- | --- |
| `dev` | `spring.sql.init.mode: always` | 面向本地开发；启动前确认目标库是可初始化的开发库，避免误连已有重要数据。 |
| `test` | 使用 `src/test/resources/` 中的配置和测试 SQL | 面向自动化测试，不连接生产依赖。 |
| `prod` | `spring.sql.init.mode: never` | 不自动执行初始化；需要迁移时先单独核对和执行。 |

`bootRun` 会读取后端工程的 `.env` 和 `deploy/.env` 中适用的本地变量，未设置 `SPRING_PROFILES_ACTIVE` 时使用 `dev`；变量来源和优先级以 [build.gradle](../backend/build.gradle) 为准。启动前核对 `MYSQL_HOST`、`MYSQL_DATABASE`、`REDIS_HOST`、`JWT_SECRET` 等环境变量。不要将开发默认密码和 JWT 密钥用于生产。

当前 `dev` 配置的验证码模式为 `real`，邮件提供方为 `smtp`；`test` 配置的验证码与 AI 则为 `mock`。因此本地只启动成功不代表邮件验证码链路可用，测试环境通过也不能证明真实邮件或 AI 接口正常。需要验证外部服务时，明确所用 profile、凭据来源和可观察结果。

上传的简历和头像使用 `uploads/` 下的目录；数据库之外还存在文件状态。修改上传、删除或备份流程时，需同时核对数据库记录与文件的对应关系。生产数据目录应按现有 Compose 卷管理，不用测试或开发初始化脚本处理。

## 常见改动的检查点

- 新增接口：检查 DTO/VO、参数校验、Service、Mapper、鉴权规则、异常映射和前端 API 调用；为成功、无权限与失败路径选择测试。
- 修改简历、岗位或投递：检查资源所属用户、状态转换、软删除与重复记录约束；数据库约束不能仅由前端校验替代。
- 修改 AI 或异步任务：检查超时、重试、缓存键、状态推送和刷新恢复；Mock 测试不证明真实 DeepSeek 可用。
- 修改 SQL：区分首次初始化与现有库迁移；先确认目标 schema 和重复数据，再评估事务、锁、备份及回滚。不要在生产上直接运行本地示例命令。

测试、构建及覆盖率命令见 [测试规范](test.md)；首次安装与端口说明见 [README 部署说明](../README.md#部署说明)，生产更新另需核对目标环境。

## 完成后如何确认

本地后端启动后可检查 `/api/health` 与 `/actuator/health`；前者是应用接口，后者会反映已启用的健康指标。健康响应不能证明特定业务流程、外部服务或生产版本已经通过验收。接口改动还应实际验证对应 API 的成功、参数错误、未登录和无权限行为，并查看日志是否泄露秘密。若改变数据模型，应说明所用测试库与目标数据库之间的差异。
