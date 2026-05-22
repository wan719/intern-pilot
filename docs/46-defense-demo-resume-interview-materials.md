# InternPilot v1.3.1 答辩演示、简历包装与面试材料

本文档基于 `docs/45-defense-demo-and-resume-packaging.md` 整理，面向课程答辩、项目展示、简历投递和技术面试。材料只描述当前 `v1.3.1` 稳定版本，不公开管理员密码、DeepSeek API Key、邮箱 SMTP 授权码或 JWT_SECRET。

## 1. README 检查结论

当前 README 已包含：

- 项目定位、技术栈、系统架构图、RAG 流程图、数据库 ER 图。
- 登录、工作台、简历、岗位、AI 分析、面试题、管理员后台、RAG 管理等功能截图。
- Docker Compose 部署说明、环境变量说明、Actuator 健康检查和日志查看命令。
- 默认账号与线上管理员说明，明确不公开管理员密码。
- GitHub / Gitee 仓库说明、CI/CD、测试命令和 Release 流程。

本轮补充：

- README 增加 `v1.3.1` 稳定演示版本说明。
- README 增加在线演示地址 `http://43.136.182.179`。
- README 更新 v1.1.0 到 v1.3.1 更新日志。
- README 补充 AI 模型路由、Prompt 版本管理、PDF 导出、Spring Boot 工程增强、前端性能优化等最新能力。
- README 的 Nginx 示例补充 gzip、`/assets/` 长缓存、`index.html` 不强缓存。

需要人工确认：

- 在线演示地址在答辩当天可访问。
- GitHub Release `v1.3.1` 已发布且描述准确。
- README 截图是否是最新 UI，尤其是 PDF 导出、任务中心、管理员后台。
- 演示账号和管理员账号只通过私下安全渠道保存。

## 2. 答辩介绍稿

### 2.1 30 秒项目介绍

InternPilot 是一个面向大学生实习求职的 AI 求职工作台，基于 Spring Boot、Vue3、MySQL、Redis、Docker 和 DeepSeek API 构建。系统支持邮箱验证码注册登录、简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户反馈和管理员后台。项目已经完成 Docker 部署、线上访问和 GitHub Release v1.3.1 发布。

### 2.2 1 分钟项目介绍

InternPilot 定位为 AI 实习投递与简历优化平台，主要解决学生找实习时简历与岗位不匹配、岗位筛选效率低、面试准备缺少针对性的问题。

系统采用 Spring Boot + Vue3 前后端分离架构。后端使用 MySQL 存储用户、简历、岗位、AI 报告和反馈数据，Redis 用于验证码、AI 任务状态和缓存，Spring Security + JWT + RBAC 实现认证授权，AI 能力通过 DeepSeek API 实现。前端使用 Vue3、Vite、Pinia、Vue Router 和 Element Plus 构建用户工作台和管理员后台。

项目还实现了 WebSocket AI 分析进度、全局 AI 任务中心、Prompt 版本管理、模型路由、AI 报告 PDF 导出、Actuator 健康检查和 Docker Compose 线上部署，目前已发布 v1.3.1。

### 2.3 3 分钟答辩讲稿

各位老师好，我的项目是 InternPilot 智能实习领航员，这是一个面向大学生实习求职场景的 AI 求职工作台。

项目背景是，很多学生在找实习时会遇到几个问题：不知道自己的简历和岗位是否匹配，不知道简历应该如何优化，也不知道如何针对目标岗位准备面试。传统求职平台更多是岗位信息展示，而我的项目希望结合 AI 能力，为学生提供从简历分析、岗位推荐到面试准备的一体化辅助。

系统采用 Spring Boot + Vue3 的前后端分离架构。后端使用 MySQL 存储用户、简历、岗位、AI 报告、反馈等数据，Redis 用于邮箱验证码、AI 任务状态和 AI 缓存，使用 Spring Security + JWT + RBAC 实现登录认证和权限控制。前端使用 Vue3、Vite、Pinia、Vue Router 和 Element Plus 构建用户工作台和管理员后台。

核心功能包括邮箱验证码注册登录、用户中心、简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户反馈和管理员后台。其中 AI 分析功能会根据用户简历和目标岗位生成匹配分数、优势、不足、简历优化建议和面试准备建议，并支持导出 PDF。

在技术实现上，我使用 WebSocket 实现 AI 分析进度实时推送，并设计了全局 AI 任务中心，解决用户切换页面后任务状态丢失的问题。AI 调用方面，我接入 DeepSeek API，并加入模型路由、Prompt 版本管理、JSON 清洗、缓存 key 优化、重试和 fallback 机制。工程方面，我接入 Actuator、参数校验、全局异常处理、AOP 接口耗时统计、操作日志脱敏和 Docker healthcheck。

项目已经通过 Docker Compose 部署到服务器，并发布 GitHub Release v1.3.1。后续可以继续优化 Element Plus 按需导入、多模型 Provider、投递进度看板和管理员数据大屏。

## 3. PPT 大纲

1. 项目标题：InternPilot 智能实习领航员。
2. 项目背景与痛点：简历岗位不匹配、面试准备缺少针对性、投递记录分散。
3. 项目目标与用户角色：普通用户、管理员。
4. 系统功能总览：用户端、管理员端、AI 能力、工程能力。
5. 系统架构设计：Vue3 / Nginx / Spring Boot / MySQL / Redis / DeepSeek。
6. 核心业务流程：简历上传、岗位选择、AI 分析、报告查看、PDF 导出。
7. AI 简历分析模块：输入、输出、分数、优势、不足和建议。
8. WebSocket + AI 任务中心：长任务进度和页面切换状态保持。
9. RAG 与 AI 面试题：知识库检索增强和结构化题目生成。
10. RBAC 权限与管理员后台：用户、角色、权限、反馈、日志、RAG 管理。
11. 工程化与部署：Actuator、Validation、AOP、Docker Compose、healthcheck。
12. 项目亮点与难点：AI 稳定性、任务中心、权限一致性、线上部署。
13. 系统演示：按演示路线现场操作。
14. 总结与后续优化：当前成果、风险、不足和迭代计划。

## 4. 系统演示路线

### 4.1 5 分钟路线

1. 打开在线系统首页。
2. 展示登录页和 InternPilot 品牌。
3. 登录普通用户。
4. 进入用户工作台。
5. 展示简历管理和岗位管理。
6. 发起 AI 简历岗位匹配分析。
7. 展示 WebSocket 进度和全局 AI 任务中心。
8. 查看 AI 分析报告。
9. 导出 PDF。
10. 展示岗位推荐。
11. 展示 AI 面试题生成。
12. 展示 RAG 问答。
13. 提交用户反馈。
14. 切换管理员账号展示管理员后台。

### 4.2 8 分钟路线

1. 项目入口：在线地址、项目定位、登录注册。
2. 普通用户流程：工作台、简历、岗位、AI 分析、任务中心、报告、PDF、推荐、面试题、RAG、反馈。
3. 管理员流程：用户管理、角色权限、RAG 知识库、反馈处理、操作日志。
4. 工程能力：Docker Compose、`/actuator/health`、GitHub Release v1.3.1。

## 5. 项目亮点

- AI 求职闭环：简历、岗位、分析、推荐、面试题、投递记录形成连续流程。
- WebSocket 实时进度：AI 长任务执行过程可见，降低等待焦虑。
- 全局 AI 任务中心：页面切换后任务状态仍可见，完成后提供结果入口。
- Prompt 版本管理：不同 AI 场景有明确模板和版本，便于迭代和缓存隔离。
- 模型路由：不同场景可通过配置选择模型，测试使用 Mock，生产使用 DeepSeek。
- AI 输出稳定性：JSON 清洗、字段兜底、分数范围校验、重试和 fallback。
- RBAC 权限：后端 `@PreAuthorize` 做真实权限边界，前端只做体验层控制。
- 工程增强：Actuator、Validation、全局异常处理、AOP 耗时统计、操作日志脱敏。
- 部署闭环：Docker Compose 编排前端、后端、MySQL、Redis，并配置 healthcheck。
- 前端体验：路由懒加载、拆包、Nginx gzip、静态资源缓存和 PDF 导出。

## 6. 项目难点与解决方案

| 难点 | 解决方案 |
| --- | --- |
| AI 分析是耗时任务 | 设计 `analysis_task` 任务表、Redis 状态缓存、WebSocket 进度推送和前端任务中心 |
| 页面切换导致任务状态丢失 | 使用 Pinia 全局 Store、localStorage 轻量持久化和后端任务查询接口恢复 |
| AI 返回格式不稳定 | Prompt 限定 JSON，增加 AiJsonSanitizer，处理代码块、前后多余文字、字段缺失和分数越界 |
| Prompt 或模型变更后缓存误命中 | 缓存 key 包含 scenario、model、promptVersion、业务对象信息和 promptHash |
| 权限前后端一致性 | 后端 Spring Security + `@PreAuthorize` 做强校验，前端根据权限控制菜单和按钮 |
| 线上配置安全 | `.env` 只保留在服务器，API Key、邮箱授权码、JWT_SECRET 通过环境变量注入 |
| Docker MySQL volume 不重复执行 init.sql | 线上变更使用 migration SQL，部署前备份数据库并手动迁移 |
| 前端构建体积偏大 | 路由动态 import、Vite manualChunks、ECharts 模块化引入、Logo 资源压缩、Nginx gzip |

## 7. 后续优化计划

短期：

- Element Plus 按需导入，继续降低 vendor chunk。
- 管理员数据大屏和反馈统计。
- 投递进度看板和面试复盘记录。

中期：

- 多模型 Provider 扩展。
- AI 分析质量评分和人工反馈闭环。
- RAG 检索排序优化，引入更专业的向量检索方案。

长期：

- 更完整的 CI/CD 部署流水线。
- 监控、告警和审计能力增强。
- 移动端适配和多组织管理。

## 8. 简历材料

### 8.1 项目名称

InternPilot 智能实习领航员｜AI 实习投递与简历优化平台

### 8.2 项目描述

基于 Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API 开发的 AI 实习投递与简历优化平台，面向大学生实习求职场景，提供邮箱验证码注册登录、简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户反馈、管理员后台和 Docker 线上部署等功能。

### 8.3 技术栈

Spring Boot、Spring Security、JWT、RBAC、MyBatis-Plus、MySQL、Redis、WebSocket、DeepSeek API、Vue3、Vite、Pinia、Vue Router、Element Plus、ECharts、Docker、Nginx、GitHub Actions。

### 8.4 项目职责

- 负责系统整体架构设计和核心业务开发，采用 Spring Boot + Vue3 前后端分离架构实现 AI 求职工作台。
- 使用 Spring Security + JWT + RBAC 实现登录认证、角色权限、菜单权限和接口权限控制。
- 接入 DeepSeek API，实现简历岗位匹配分析、岗位推荐、AI 面试题生成和 RAG 知识库问答。
- 使用 WebSocket 实现 AI 分析进度实时推送，设计全局 AI 任务中心解决页面切换后任务状态丢失问题。
- 使用 Redis 实现邮箱验证码、AI 任务状态、AI 缓存和缓存 key 版本隔离。
- 设计 AI 模型路由、Prompt 模板版本管理、JSON 清洗、失败重试、fallback 和日志脱敏机制。
- 接入 Actuator、Validation、全局异常处理、AOP 耗时统计和 Docker healthcheck，提升工程可观测性。
- 使用 Docker Compose 部署前端 Nginx、后端服务、MySQL 和 Redis，实现服务器在线访问并发布 GitHub Release v1.3.1。

### 8.5 项目亮点短版

- 基于 Spring Boot + Vue3 + DeepSeek API 实现 AI 简历岗位匹配、岗位推荐、面试题生成和 RAG 问答。
- 使用 JWT + RBAC + `@PreAuthorize` 实现用户、管理员、菜单、按钮和接口权限控制。
- 使用 WebSocket + Redis + Pinia 设计全局 AI 任务中心，实现长任务进度推送和状态恢复。
- 通过 Prompt 版本管理、模型路由、缓存 key 版本隔离、JSON 清洗和 fallback 提升 AI 调用稳定性。
- 使用 Docker Compose、Nginx、Actuator 和 healthcheck 完成线上部署与健康检查。

### 8.6 可量化成果表达

- 完成普通用户、管理员、AI 分析、RAG、反馈、投递记录等 10+ 核心业务模块。
- 设计并实现 4 类 AI 能力：简历分析、岗位推荐、面试题生成、RAG 问答。
- 覆盖前端、后端、数据库、缓存、AI、部署和 CI/CD 的完整工程链路。
- 支持 Docker Compose 一键部署 4 个服务：frontend、backend、mysql、redis。
- 前端通过路由懒加载和 manualChunks 将主要页面 chunk 控制在十几 KB 级别，优化首屏加载体验。

## 9. 面试问答

### 9.1 JWT 登录流程

用户登录时，后端校验邮箱和密码，成功后生成 JWT 返回给前端。前端保存 token，后续请求通过 `Authorization: Bearer <token>` 携带。后端 JWT 过滤器解析 token，获取用户身份和权限，放入 SecurityContext，再由 Spring Security 判断接口是否允许访问。

### 9.2 RBAC 权限设计

系统设计用户、角色、权限三类模型。用户和角色是多对多关系，角色和权限也是多对多关系。后端通过 `@PreAuthorize` 控制接口权限，前端根据权限动态显示菜单和按钮。真正的安全边界在后端，防止用户绕过前端直接访问管理员接口。

### 9.3 WebSocket 进度推送

AI 分析是耗时任务，不能只让用户等待 HTTP 请求。系统创建任务后返回 taskNo，后端在解析简历、构建上下文、调用 AI、保存报告等阶段推送进度消息，前端通过 WebSocket 更新进度条和全局任务中心。

### 9.4 DeepSeek API 接入

项目将 AI 调用抽象为 AiClient 接口，生产环境使用 DeepSeekAiClient，测试环境使用 MockAiClient。业务层不直接依赖具体模型实现，方便测试和扩展。API Key 通过环境变量注入，不写入代码或仓库。

### 9.5 Prompt 版本管理

不同 AI 场景有独立 Prompt 模板和版本，例如简历分析、岗位推荐、面试题生成、RAG 问答。日志和缓存 key 包含 promptVersion、model 和 promptHash，避免 Prompt 变化后误命中旧缓存，也方便排查 AI 输出问题。

### 9.6 RAG 实现

管理员维护岗位知识库，系统将文档切分成知识片段并生成 embedding。用户提问或生成面试题时，系统先检索相关知识片段，再将上下文和问题一起放入 Prompt 调用 DeepSeek，使回答更贴近知识库内容。

### 9.7 Redis 使用

Redis 主要用于邮箱验证码存储和 TTL 控制、AI 任务状态缓存、AI 分析缓存和用户权限缓存。验证码设置较短 TTL，AI 任务状态设置较长 TTL，AI 缓存 key 包含 scenario、model、promptVersion 和 promptHash，避免缓存污染。

### 9.8 Docker 部署

项目使用 Docker Compose 部署 frontend、backend、mysql、redis。前端由 Nginx 提供静态资源，并代理 `/api`、`/uploads` 和 `/ws` 到后端。后端通过环境变量读取 MySQL、Redis、DeepSeek API Key、邮箱授权码等配置。生产环境 `.env` 只保存在服务器。

### 9.9 Actuator 健康检查

后端接入 Spring Boot Actuator，开放 `/actuator/health` 和 `/actuator/info`。Spring Security 放行 health 端点，Docker Compose healthcheck 调用 `/actuator/health` 判断后端是否 UP，生产环境不公开过多 actuator 端点。

### 9.10 项目难点与不足

难点主要包括 AI 长任务进度展示、页面切换后的任务状态保持、AI JSON 输出不稳定、权限前后端一致性和线上 Docker 数据迁移。当前不足是 Element Plus 仍为全量注册，vendor chunk 偏大；岗位推荐仍以规则和已有数据为主，后续可以增强 AI 解释；监控告警还可以接入更完整的可观测性方案。

