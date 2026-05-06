# InternPilot

InternPilot 是一个面向大学生实习求职场景的 AI 实习投递与简历优化平台。系统支持用户上传 PDF / DOCX 简历、保存目标岗位 JD、调用大语言模型分析简历与岗位的匹配程度，并生成匹配分数、优势短板、技能缺口、简历优化建议和面试准备建议。系统同时支持投递记录管理，帮助用户跟踪从待投递、已投递、笔试、面试到 Offer 或被拒的完整流程。

这个项目适合作为 Java 后端实习求职项目展示，重点体现 Spring Boot 后端开发、Spring Security + JWT 鉴权、MySQL 建模、Redis 缓存、文件上传解析、AI 应用集成、用户数据权限控制和工程化交付能力。

## 一句话介绍

InternPilot 是一个面向大学生实习求职的 AI 简历岗位匹配与投递管理平台，支持简历上传解析、岗位 JD 管理、AI 匹配分析、分析报告生成和投递进度跟踪。

## 核心功能

- 用户注册、登录与 JWT 无状态认证
- Spring Security 接口鉴权和 401 / 403 统一处理
- PDF / DOCX 简历上传、校验、保存与文本解析
- 岗位 JD 创建、查询、搜索、修改和逻辑删除
- AI 简历岗位匹配分析，生成结构化 JSON 分析报告
- Redis 缓存相同用户、简历、岗位组合的 AI 分析结果
- 分析报告历史查询和详情查看
- 投递记录创建、列表查询、状态更新、备注复盘和逻辑删除
- 基于 userId 的简历、岗位、报告、投递记录数据隔离
- Knife4j / Swagger 接口文档
- Dockerfile / Docker Compose 部署配置

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Spring Security 6
- JWT
- MyBatis Plus
- MySQL 8
- Redis 7
- Apache PDFBox
- Apache POI
- Knife4j / Swagger
- Lombok
- Gradle

AI 与工程化：

- DeepSeek API，兼容扩展 OpenAI / Qwen 等模型
- Prompt Engineering
- JSON 结构化输出解析
- Redis 缓存 AI 分析结果
- 统一响应结构
- 全局异常处理
- 参数校验
- 逻辑删除
- Docker Compose

## 业务闭环

```text
用户注册登录
  -> 上传 PDF / DOCX 简历
  -> 系统解析简历文本
  -> 创建目标岗位 JD
  -> 选择简历和岗位发起 AI 分析
  -> 生成并保存匹配分析报告
  -> 创建投递记录
  -> 持续更新投递状态和复盘内容
```

## 架构概览

```text
前端 / API 调用方
  -> HTTP JSON / Multipart
  -> Spring Boot Controller
  -> Service 业务层
  -> MyBatis Plus Mapper
  -> MySQL

Spring Boot
  -> Redis 缓存 AI 分析结果
  -> uploads/resumes 文件存储
  -> 大语言模型 API
```

## 快速启动

### 本地启动后端

1. 准备 MySQL 8 和 Redis 7。

2. 创建数据库并执行初始化 SQL：

```sql
CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

初始化文件：

```text
backend/intern-pilot-backend/src/main/resources/sql/init.sql
```

3. 设置环境变量：

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

本地开发不想调用真实 AI 时，可以启用 mock profile：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev,mock"
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

查看日志：

```powershell
docker compose logs -f backend
```

停止服务：

```powershell
docker compose down
```

## API 文档

后端启动后访问：

```text
http://localhost:8080/doc.html
```

健康检查：

```text
http://localhost:8080/api/health
```

## 面试包装

项目可以重点包装为一个“AI + Java 后端 + 完整业务闭环”的实习项目。

推荐简历标题：

```text
InternPilot：AI 实习投递与简历优化平台
```

推荐简历精简描述：

```text
基于 Spring Boot 3 + Spring Security + JWT + MySQL + Redis + 大语言模型 API 构建，
支持简历上传解析、岗位 JD 管理、AI 匹配分析、分析报告生成和投递记录管理。
负责用户认证、简历解析、AI 分析、Redis 缓存、数据权限控制和接口文档等核心模块，
实现从岗位分析到投递跟踪的完整业务闭环。
```

完整面试材料见：

- [面试总结与简历包装文档](docs/18-interview-summary.md)
- [面试与简历精简包](docs/20-interview-resume-package.md)

## 项目亮点

1. 完整业务闭环：从简历上传、岗位管理、AI 分析到投递记录管理，覆盖真实实习求职流程。
2. 认证鉴权完整：基于 Spring Security + JWT 实现前后端分离场景下的无状态认证。
3. 数据权限严格：所有私有资源都绑定 userId，业务查询同时校验资源 ID 和当前用户 ID。
4. 文件处理真实：支持 PDF / DOCX 简历上传与解析，解析文本用于后续 AI 分析。
5. AI 工程化集成：通过 Prompt 约束模型输出结构化 JSON，并对异常输出做清洗解析。
6. Redis 缓存优化：缓存相同用户、简历、岗位组合的分析结果，减少重复 AI 调用。
7. 工程化完整：统一响应、全局异常、参数校验、Swagger 文档、Docker Compose 配置齐全。

## 文档目录

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
- [API 接口测试指南](docs/17-api-test-guide.md)
- [面试总结与简历包装](docs/18-interview-summary.md)
- [前端设计](docs/19-frontend-design.md)
- [面试与简历精简包](docs/20-interview-resume-package.md)

## 后续规划

- 完成前端页面和演示数据流
- AI 分析改为异步任务，支持进度查询或 WebSocket 推送
- 增加投递状态时间线和状态变更日志
- 增加投递数据看板
- 支持 OCR 解析扫描版 PDF
- 增加 AI 调用日志、成本统计和更细粒度的缓存失效策略
- 增加 GitHub Actions 自动测试
