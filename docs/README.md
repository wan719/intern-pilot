# 技术文档

公开仓库维护产品说明、技术设计、接口、部署方式和版本变更。验收执行证据、历史截图、逐任务实施计划、答辩评分及个人运维记录独立归档，不作为本仓库的运行依赖。

## 首选入口

| 需要了解 | 文档 |
| --- | --- |
| 安装、启动、测试 | [项目 README](../README.md) / [English README](../README_EN.md) |
| 指定 Tag 升级、健康检查、数据库与回滚 | [部署指南](deployment.md) |
| v1.4.0 变更与验证范围 | [发布说明](releases/v1.4.0.md) |
| 当前界面信息架构与交互约束 | [UI 设计基线](architecture/ui-design.md) |
| 数据库初始化与迁移源码 | [SQL 目录](../backend/intern-pilot-backend/src/main/resources/sql/) |
| 容器编排与生产配置示例 | [Compose](../deploy/docker-compose.yml) / [环境变量示例](../deploy/.env.example) |

## 系统设计

- [项目背景与目标](02-project-background-and-goals.md)、[可行性分析](01-feasibility-analysis.md)、[项目规划](04-project-planning.md)。
- [技术选型](03-technical-selection.md)、[系统分析](05-system-analysis.md)、[需求分析](06-requirements-analysis.md)。
- [概要设计](07-outline-design.md)、[数据库设计](08-database-design.md)、[接口设计](09-api-design.md)。
- [后端工程](10-backend-initialization.md)、[前端初始设计](19-frontend-design.md)、[API 测试指南](17-api-test-guide.md)。

## 功能与工程设计

- 认证与权限：[JWT](11-auth-jwt-design.md)、[RBAC](20-rbac-permission-design.md)、[RBAC 加固](30-rbac-permission-enhancement-local-dev-design.md)、[验证码设计](36-auth-phone-email-captcha-design.md)、[真实验证码](36-auth-phone-email-captcha-real-online-design.md)、[邮箱认证与用户中心](37-email-auth-single-admin-user-center-design.md)。
- 核心业务：[简历解析](12-resume-upload-parse-design.md)、[简历版本](25-resume-version-design.md)、[岗位](13-job-description-design.md)、[投递](15-application-record-design.md)、[岗位推荐](26-job-recommendation-design.md)。
- AI：[匹配分析](14-ai-analysis-design.md)、[WebSocket 进度](21-websocket-ai-progress-design.md)、[进度测试](31-websocket-ai-progress-enhancement-test-design.md)、[缓存与 Mock](32-ai-analysis-cache-and-mock-ai-enhancement-design.md)、[面试题](22-ai-interview-question-design.md)、[面试题测试](33-interview-question-enhancement-test-design.md)、[RAG](27-rag-job-knowledge-base-design.md)、[模型路由与 Prompt](42-ai-model-router-and-prompt-optimization-design.md)。
- 平台：[操作日志](23-system-operation-log-design.md)、[管理后台](24-admin-console-design.md)、[任务中心与反馈](39-ai-task-center-and-feedback-design.md)。
- 工程：[工程化增强](16-engineering-enhancement.md)、[测试增强](28-testing-enhancement.md)、[CI/CD 设计](29-cicd-and-online-demo.md)、[体验修复设计](34-product-experience-bugfix-and-acceptance-design.md)、[早期 UI 优化](38-frontend-ui-polish-and-user-experience-design.md)、[Spring Boot 工程能力](43-spring-boot-engineering-enhancement-design.md)、[PDF 导出与性能](44-ai-report-pdf-export-and-frontend-performance-design.md)。

## 文档边界

编号文档保留各阶段设计背景，部分使用规划语气或包含早期配置示例；它们不是实时线上状态，也不保证每项规划均已实现。当前行为以所用版本源码与测试为准，部署按本目录的部署指南执行；v1.4.0 界面以 UI 设计基线为准。

新增文档应面向仓库使用者，避免个人聊天记录、评分表、私有目录绝对路径、凭据及无日期的“全部通过”声明。变更记录应区分设计要求、本地测试结果与生产验收结果。
