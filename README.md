# InternPilot

InternPilot 是一个面向大学生实习求职场景的 AI 实习投递与简历优化平台。

系统支持用户上传简历、保存岗位 JD、调用大语言模型分析简历与岗位匹配度，并生成简历优化建议、技能缺口分析和面试准备建议。同时，系统支持实习投递记录管理，帮助用户跟踪投递状态、面试时间和复盘内容。

该项目基于 Spring Boot 3、Spring Security、JWT、MySQL、Redis 和大语言模型 API 构建，重点展示 Java 后端开发、权限认证、文件解析、AI 应用集成、缓存优化和工程化设计能力。

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

## 项目结构

```text
intern-pilot
├── backend
│   └── intern-pilot-backend
├── frontend
├── docs
├── deploy
│   └── docker-compose.yml
└── README.md
```

## 快速启动

### 本地启动后端

1. 准备 MySQL 8 和 Redis 7。
2. 创建数据库并执行初始化 SQL：

```sql
CREATE DATABASE IF NOT EXISTS intern_pilot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

初始化表结构文件位于：

```text
backend/intern-pilot-backend/src/main/resources/sql/init.sql
```

3. 设置环境变量，至少包含：

```powershell
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_DATABASE = "intern_pilot"
$env:MYSQL_USERNAME = "root"
$env:MYSQL_PASSWORD = "your_password"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:JWT_SECRET = "replace-with-a-long-random-secret"
$env:AI_API_KEY = "your_ai_api_key"
```

4. 启动后端：

```powershell
cd backend/intern-pilot-backend
.\gradlew.bat bootRun
```

### Docker Compose 启动

```powershell
cd deploy
docker compose up -d
```

查看后端日志：

```powershell
docker compose logs -f backend
```

停止服务：

```powershell
docker compose down
```

## 环境变量说明

| 变量名 | 说明 | 默认值 |
| --- | --- | --- |
| MYSQL_HOST | MySQL 主机 | localhost |
| MYSQL_PORT | MySQL 端口 | 3306 |
| MYSQL_DATABASE | 数据库名 | intern_pilot |
| MYSQL_USERNAME | MySQL 用户名 | root |
| MYSQL_PASSWORD | MySQL 密码 | root |
| REDIS_HOST | Redis 主机 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| REDIS_DATABASE | Redis 数据库编号 | 0 |
| REDIS_PASSWORD | Redis 密码 | 空 |
| JWT_SECRET | JWT 签名密钥 | 开发占位值 |
| JWT_EXPIRATION | Token 有效期毫秒数 | 86400000 |
| AI_PROVIDER | AI 服务提供商 | deepseek |
| AI_API_KEY | AI API Key | 空 |
| AI_BASE_URL | AI 服务地址 | https://api.deepseek.com |
| AI_MODEL | AI 模型名称 | deepseek-chat |
| AI_TIMEOUT | AI 调用超时时间 | 60000 |

## API 文档

后端启动后访问：

```text
http://localhost:8080/doc.html
```

健康检查：

```text
http://localhost:8080/api/health
```

## 核心业务流程

```text
用户注册登录
  ↓
上传简历
  ↓
创建岗位 JD
  ↓
AI 匹配分析
  ↓
保存分析报告
  ↓
创建投递记录
  ↓
跟踪投递状态
```

## 项目文档

- [可行性分析](docs/01-feasibility-analysis.md)
- [项目背景与目标](docs/02-project-background-and-goals.md)
- [技术选型](docs/03-technical-selection.md)
- [项目规划](docs/04-project-planning.md)
- [系统分析](docs/05-system-analysis.md)
- [需求分析](docs/06-requirements-analysis.md)
- [概要设计](docs/07-outline-design.md)
- [数据库设计](docs/08-database-design.md)
- [接口设计](docs/09-api-design.md)
- [后端初始化](docs/10-backend-initialization.md)
- [用户认证与 JWT 鉴权](docs/11-auth-jwt-design.md)
- [简历上传与解析](docs/12-resume-upload-parse-design.md)
- [岗位 JD 管理](docs/13-job-description-design.md)
- [AI 分析模块](docs/14-ai-analysis-design.md)
- [投递记录模块](docs/15-application-record-design.md)
- [工程化增强](docs/16-engineering-enhancement.md)

## 项目亮点

1. 完整的实习求职业务闭环：从简历上传、岗位管理、AI 分析到投递记录管理，形成完整流程。
2. Spring Security + JWT 无状态认证：支持前后端分离场景下的登录认证和接口鉴权。
3. 严格的数据权限控制：简历、岗位、报告和投递记录均基于 userId 做数据隔离，防止越权访问。
4. AI 应用集成：通过 Prompt 将简历文本与岗位 JD 输入大语言模型，生成结构化匹配分析报告。
5. Redis 缓存优化：对相同简历和岗位组合的 AI 分析结果进行缓存，降低重复调用成本。
6. 文件上传与解析：支持 PDF 和 DOCX 简历上传，使用 PDFBox 和 POI 提取文本内容。
7. 工程化设计：使用统一响应、统一异常处理、参数校验、Swagger 文档和 Docker Compose 部署结构。

## 后续规划

- 补充完整接口测试文档
- 增加更多 Service 层和 Security 集成测试
- 完成前端页面开发
- 增加投递状态时间线
- 增加投递数据看板
