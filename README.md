# InternPilot

InternPilot 是一个面向大学生实习求职场景的 AI 实习投递与简历优化平台，支持简历上传解析、岗位 JD 管理、AI 匹配度分析、简历优化建议生成和投递流程跟踪，帮助学生提升岗位匹配度与投递效率。

## 项目定位

本项目面向大学生实习求职场景，结合 Spring Boot 后端工程能力与大语言模型分析能力，构建一个完整的 AI 求职辅助平台。

## 核心功能

- 用户注册与登录
- JWT 身份认证
- 简历上传与解析
- 岗位 JD 管理
- AI 简历与岗位匹配分析
- 简历优化建议生成
- 实习投递记录管理
- Redis 分析结果缓存
- Swagger 接口文档
- Docker Compose 部署

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- Spring Security
- JWT
- MyBatis Plus
- MySQL
- Redis
- Swagger / Knife4j

### 前端

- Vue 3
- Vite
- Element Plus
- Axios
- Pinia

### AI

- DeepSeek API / OpenAI API / Qwen API
- Prompt Engineering
- JSON Structured Output

### 部署

- Docker
- Docker Compose
- Nginx

## 项目文档

- [可行性分析报告](docs/01-feasibility-analysis.md)
- [项目背景与目标](docs/02-project-background-and-goals.md)
- [技术选型文档](docs/03-technical-selection.md)
- [项目规划企划](docs/04-project-planning.md)
- [系统分析文档](docs/05-system-analysis.md)
- [需求分析文档](docs/06-requirements-analysis.md)
- [概要设计文档](docs/07-outline-design.md)
- [数据库设计文档](docs/08-database-design.md)
- [接口设计文档](docs/09-api-design.md)
- [后端工程初始化设计文档](docs/10-backend-initialization.md)
- [用户认证与 JWT 鉴权设计文档](docs/11-auth-jwt-design.md)
- [简历上传与解析模块设计文档](docs/12-resume-upload-parse-design.md)
- [岗位 JD 管理模块设计文档](docs/13-job-description-design.md)
- [AI 分析模块设计文档](docs/14-ai-analysis-design.md)

## 项目状态

当前项目已完成文档规划、后端工程初始化和用户认证 / JWT 鉴权基础能力，后续将继续实现简历模块、岗位模块、AI 分析模块和投递记录模块。
