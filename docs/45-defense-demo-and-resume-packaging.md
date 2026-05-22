# InternPilot 答辩演示、项目包装与简历面试材料整理文档

## 1. 文档背景

InternPilot 当前已经完成 `v1.3.1` 版本发布与服务器上线。

当前版本名称：

```text
InternPilot v1.3.1 - AI 工程化、Spring Boot 工程增强与前端体验优化版
```

当前系统已完成：

```text
1. 邮箱验证码注册登录
2. JWT 鉴权
3. RBAC 权限系统
4. 用户中心
5. 简历管理
6. 岗位管理
7. AI 简历岗位匹配分析
8. WebSocket AI 分析进度
9. 全局 AI 任务中心
10. 岗位推荐
11. AI 面试题生成
12. RAG 岗位知识库问答
13. 用户反馈
14. 管理员后台
15. DeepSeek API 接入
16. AI 模型路由与 Prompt 版本管理
17. Spring Boot Actuator / Validation / AOP / Docker healthcheck
18. AI 分析报告 PDF 导出
19. 前端性能优化
20. Docker Compose 线上部署
21. GitHub Release v1.3.1
```

本阶段目标不是继续开发新功能，而是将项目整理成：

```text
可答辩
可演示
可写进简历
可用于面试讲解
可作为作品集展示
```

---

## 2. 本阶段目标

本阶段需要完成：

```text
1. 固化 v1.3.1 稳定演示版本
2. 整理线上最终验收清单
3. 设计 5 分钟功能演示路线
4. 设计 10 分钟详细答辩讲稿
5. 整理 PPT 大纲
6. 整理项目技术亮点
7. 整理项目难点与解决方案
8. 整理简历项目描述
9. 整理面试高频问题回答
10. 整理后续迭代规划
```

---

## 3. 本阶段不做什么

当前阶段不再做：

```text
1. 不新增大型业务功能
2. 不重构核心架构
3. 不直接修改 main 分支
4. 不随意修改线上稳定配置
5. 不公开管理员密码
6. 不公开 DeepSeek API Key
7. 不公开 QQ 邮箱授权码
8. 不再为了炫技引入复杂技术
```

当前重点是：

```text
讲清楚项目
演示稳定流程
体现工程能力
包装简历亮点
```

---

# 4. 当前版本状态

## 4.1 当前稳定版本

```text
版本号：v1.3.1
分支：main
开发集成分支：dev
线上部署目录：~/intern-pilot
服务器 IP：43.136.182.179
部署方式：Docker Compose
部署文件：deploy/docker-compose.yml
```

---

## 4.2 当前版本定位

`v1.3.1` 是当前推荐用于答辩、展示和简历包装的稳定版本。

版本特点：

```text
1. 功能完整
2. AI 能力明确
3. 工程能力充分
4. 已上线部署
5. 已发布 GitHub Release
6. 可在线演示
7. 可讲解技术架构
```

---

# 5. 线上最终验收清单

答辩或提交前，必须最后检查一遍线上系统。

## 5.1 服务器状态检查

进入服务器：

```bash
ssh ubuntu@43.136.182.179
cd ~/intern-pilot
```

查看容器状态：

```bash
docker compose -f deploy/docker-compose.yml ps
```

期望：

```text
backend   running / healthy
frontend  running / healthy
mysql     running / healthy
redis     running / healthy
```

后端健康检查：

```bash
curl http://localhost:8080/actuator/health
```

期望：

```json
{"status":"UP"}
```

查看后端日志：

```bash
docker logs -f internpilot-backend
```

查看前端日志：

```bash
docker logs -f internpilot-frontend
```

---

## 5.2 线上功能验收

必须检查：

```text
1. 首页能访问
2. 登录页能正常打开
3. 注册页能正常打开
4. 邮箱验证码能正常发送
5. 普通用户能注册登录
6. 管理员账号能登录
7. 用户中心正常
8. 简历管理正常
9. 岗位管理正常
10. AI 分析能调用 DeepSeek
11. WebSocket 进度正常
12. 全局 AI 任务中心正常
13. AI 分析报告正常
14. AI 报告 PDF 导出正常
15. 岗位推荐正常
16. AI 面试题生成正常
17. RAG 问答正常
18. 用户反馈正常
19. 管理员后台正常
20. 普通用户不能访问管理员后台
```

---

## 5.3 安全检查

必须确认：

```text
1. README 没有管理员密码
2. README 没有 demo / 123456 演示账号
3. GitHub 仓库没有 .env
4. GitHub 仓库没有 DeepSeek API Key
5. GitHub 仓库没有 QQ 邮箱授权码
6. GitHub 仓库没有 JWT_SECRET
7. 线上使用 DeepSeek 真接口
8. Mock 只用于测试环境
```

---

# 6. 答辩演示路线

## 6.1 推荐 5 分钟演示路线

这条路线适合课堂答辩或快速展示。

```text
1. 打开线上系统首页
2. 展示登录页和 InternPilot 品牌 Logo
3. 登录普通用户
4. 进入用户工作台
5. 展示简历管理
6. 展示岗位管理
7. 发起 AI 简历岗位匹配分析
8. 展示 WebSocket 进度和全局 AI 任务中心
9. 查看 AI 分析报告
10. 导出 PDF
11. 展示岗位推荐
12. 展示 AI 面试题生成
13. 展示 RAG 问答
14. 提交用户反馈
15. 切换管理员账号展示管理员后台
```

---

## 6.2 推荐 8 分钟完整演示路线

```text
第一部分：项目入口
1. 打开线上地址
2. 介绍项目定位：AI 求职工作台
3. 展示登录 / 注册页面

第二部分：普通用户流程
4. 登录普通用户
5. 进入用户工作台
6. 展示简历管理
7. 展示岗位管理
8. 发起 AI 分析
9. 展示 AI 任务中心
10. 展示分析报告
11. 导出 PDF
12. 展示岗位推荐
13. 展示面试题生成
14. 展示 RAG 问答
15. 提交反馈

第三部分：管理员流程
16. 登录管理员
17. 展示用户管理
18. 展示角色权限管理
19. 展示 RAG 知识库管理
20. 展示用户反馈处理
21. 展示操作日志或系统管理能力

第四部分：工程能力
22. 简单介绍 Docker 部署
23. 展示 /actuator/health
24. 展示 GitHub Release v1.3.1
```

---

# 7. 答辩讲稿

## 7.1 30 秒项目介绍

```text
InternPilot 是一个面向大学生实习求职的 AI 求职工作台，基于 Spring Boot、Vue3、MySQL、Redis、Docker 和 DeepSeek API 构建。系统支持邮箱验证码注册登录、简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户反馈和管理员后台。项目已完成 Docker 部署、线上访问和 GitHub Release 发布。
```

---

## 7.2 1 分钟项目介绍

```text
我的项目叫 InternPilot，定位是面向大学生的 AI 实习投递与简历优化平台。它主要解决学生找实习时简历和岗位不匹配、岗位筛选效率低、面试准备不充分的问题。

系统采用 Spring Boot + Vue3 前后端分离架构，后端使用 MySQL 存储用户、简历、岗位、报告等数据，Redis 存储验证码、AI 任务状态和缓存，使用 Spring Security + JWT + RBAC 实现认证和权限控制。AI 能力通过 DeepSeek API 实现，包括简历岗位匹配分析、岗位推荐、面试题生成和 RAG 问答。

项目还实现了 WebSocket AI 分析进度、全局 AI 任务中心、用户反馈、管理员后台、PDF 导出、Actuator 健康检查和 Docker Compose 线上部署。目前已发布 v1.3.1 版本。
```

---

## 7.3 3 分钟答辩讲稿

```text
各位老师好，我的项目是 InternPilot 智能实习领航员，这是一个面向大学生实习求职场景的 AI 求职工作台。

项目背景是，很多大学生在找实习时会遇到几个问题：不知道自己的简历和岗位是否匹配，不知道简历应该如何优化，也不知道如何针对目标岗位准备面试。传统求职平台更多是岗位信息展示，而我的项目希望结合 AI 能力，为学生提供从简历分析、岗位推荐到面试准备的一体化辅助。

系统采用 Spring Boot + Vue3 的前后端分离架构。后端使用 MySQL 存储用户、简历、岗位、AI 报告、反馈等数据，Redis 用于邮箱验证码、AI 任务状态和缓存，使用 Spring Security + JWT + RBAC 实现登录认证和权限控制。前端使用 Vue3、Vite、Pinia、Vue Router 和 Element Plus 构建用户工作台和管理员后台。

核心功能包括：邮箱验证码注册登录、用户中心、简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户反馈和管理员后台。其中 AI 分析功能会根据用户简历和目标岗位生成匹配分数、优势、不足、简历优化建议和面试准备建议。

在技术实现上，我使用 WebSocket 实现 AI 分析进度实时推送，并设计了全局 AI 任务中心，解决用户切换页面后 AI 任务状态丢失的问题。AI 调用方面，我接入 DeepSeek API，并在后续迭代中加入了模型路由、Prompt 版本管理、JSON 清洗、缓存 key 优化、重试和 fallback 机制。工程方面，我接入了 Spring Boot Actuator、参数校验、全局异常处理、AOP 接口耗时统计、操作日志脱敏和 Docker healthcheck。

项目已经通过 Docker Compose 部署到服务器，并发布了 GitHub Release v1.3.1。后续可以继续优化多模型 Provider、Element Plus 按需导入、投递进度看板和管理员数据大屏。
```

---

# 8. PPT 大纲

## 8.1 推荐 PPT 结构

```text
第 1 页：项目标题
第 2 页：项目背景与痛点
第 3 页：项目目标与用户角色
第 4 页：系统功能总览
第 5 页：系统架构设计
第 6 页：核心业务流程
第 7 页：AI 简历分析模块
第 8 页：WebSocket + AI 任务中心
第 9 页：RAG 与 AI 面试题生成
第 10 页：RBAC 权限与管理员后台
第 11 页：工程化与部署
第 12 页：项目亮点与难点
第 13 页：系统演示
第 14 页：项目总结与后续优化
```

---

## 8.2 每页内容建议

### 第 1 页：项目标题

```text
InternPilot 智能实习领航员
面向大学生的 AI 实习投递与简历优化平台
技术栈：Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API
```

---

### 第 2 页：项目背景与痛点

```text
1. 学生不知道简历和岗位是否匹配
2. 简历优化缺少针对性建议
3. 岗位筛选效率低
4. 面试准备缺少目标岗位导向
5. 传统平台更偏信息展示，缺少个性化 AI 分析
```

---

### 第 3 页：项目目标与用户角色

```text
普通用户：
- 管理简历
- 管理岗位
- AI 分析
- 岗位推荐
- 面试题生成
- RAG 问答
- 提交反馈

管理员：
- 用户管理
- 角色权限管理
- RAG 知识库管理
- 反馈处理
- 操作日志查看
```

---

### 第 4 页：系统功能总览

```text
用户端：
注册登录、用户中心、简历管理、岗位管理、AI 分析、岗位推荐、面试题、RAG、反馈

管理端：
用户管理、角色权限、知识库管理、反馈管理、操作日志

系统能力：
JWT、RBAC、WebSocket、Redis、DeepSeek、Docker、Actuator
```

---

### 第 5 页：系统架构设计

```text
浏览器
  ↓
Vue3 前端
  ↓
Nginx
  ↓
Spring Boot 后端
  ↓
MySQL / Redis / DeepSeek API
```

可放 Mermaid 架构图或自己画图。

---

### 第 6 页：核心业务流程

```text
用户上传简历
  ↓
选择目标岗位
  ↓
发起 AI 分析
  ↓
WebSocket 推送进度
  ↓
DeepSeek 生成报告
  ↓
任务中心通知完成
  ↓
用户查看报告 / 导出 PDF
```

---

### 第 7 页：AI 简历分析模块

```text
输入：
- 简历内容
- 岗位描述

输出：
- 匹配总分
- 维度得分
- 优势分析
- 不足分析
- 简历优化建议
- 面试准备建议
- 风险提示
```

---

### 第 8 页：WebSocket + AI 任务中心

```text
解决问题：
页面切换后 AI 任务状态丢失

设计：
- analysis_task 任务表
- Redis 任务状态缓存
- WebSocket 进度推送
- Pinia 全局任务中心
- localStorage 轻量持久化
- 右下角完成通知
```

---

### 第 9 页：RAG 与 AI 面试题生成

```text
RAG：
知识库文档 → chunk 切分 → 检索相关上下文 → DeepSeek 回答

面试题：
简历 + 岗位 → Prompt 模板 → DeepSeek → 结构化面试题
```

---

### 第 10 页：RBAC 权限与管理员后台

```text
RBAC：
用户 → 角色 → 权限

后端：
Spring Security + @PreAuthorize

前端：
路由权限 + 菜单权限 + 按钮权限

管理员后台：
用户、角色、权限、RAG、反馈、日志
```

---

### 第 11 页：工程化与部署

```text
1. Docker Compose 部署
2. Nginx 前端服务与反向代理
3. MySQL / Redis 容器化
4. Actuator 健康检查
5. Docker healthcheck
6. GitHub Release v1.3.1
7. 线上系统访问
```

---

### 第 12 页：项目亮点与难点

```text
亮点：
- AI 求职闭环
- WebSocket 实时进度
- 全局 AI 任务中心
- Prompt 版本管理
- RBAC 权限系统
- PDF 导出
- Docker 线上部署

难点：
- AI 返回格式不稳定
- 页面切换任务状态丢失
- 权限前后端一致性
- 线上数据库迁移
- Docker healthcheck 与生产配置
```

---

### 第 13 页：系统演示

```text
演示顺序：
登录 → 工作台 → AI 分析 → 任务中心 → 报告 → PDF → 推荐 → 面试题 → RAG → 反馈 → 管理后台
```

---

### 第 14 页：总结与后续优化

```text
当前成果：
已完成 v1.3.1，支持线上访问和 Release 发布。

后续优化：
1. Element Plus 按需导入
2. 多模型 Provider 扩展
3. 投递进度看板
4. 管理员数据大屏
5. 更完善的监控告警
```

---

# 9. 项目技术亮点

## 9.1 AI 能力亮点

```text
1. 接入 DeepSeek API，实现真实 AI 能力
2. 使用 Prompt 模板版本管理，方便后续优化
3. 按 AI 场景进行模型路由
4. AI 返回 JSON 清洗，增强解析稳定性
5. 支持失败重试和 fallback
6. 缓存 key 包含 scenario、model、promptVersion、promptHash
7. Mock AI 仅用于测试，线上使用真实 DeepSeek
```

---

## 9.2 后端工程亮点

```text
1. Spring Security + JWT + RBAC 权限控制
2. Controller / Service / Mapper 分层清晰
3. DTO / VO 分离
4. GlobalExceptionHandler 统一异常处理
5. Validation 参数校验
6. AOP 接口耗时统计
7. 操作日志脱敏
8. Actuator 健康检查
9. Redis 缓存和验证码 TTL
10. Docker healthcheck
```

---

## 9.3 前端体验亮点

```text
1. Vue3 + Vite + Pinia + Element Plus
2. 全局 AI 任务中心
3. AI 任务完成右下角通知
4. AI 报告 PDF 导出
5. 路由懒加载
6. Vite manualChunks 拆包
7. Logo 资源优化
8. Nginx gzip 和静态资源缓存
```

---

## 9.4 产品亮点

```text
1. 面向大学生实习求职，场景明确
2. 从简历到岗位、分析、推荐、面试题形成闭环
3. 用户反馈形成产品迭代闭环
4. 管理员后台支持系统维护
5. 支持线上访问和演示
```

---

# 10. 项目难点与解决方案

## 10.1 难点一：AI 分析是耗时任务

问题：

```text
AI 分析需要调用大模型，不能像普通接口一样瞬间返回。
```

解决：

```text
1. 设计 analysis_task 任务表
2. 后端分阶段更新任务状态
3. 使用 WebSocket 推送进度
4. Redis 缓存任务状态
5. 前端展示进度条
```

答辩讲法：

```text
AI 分析属于耗时任务，如果直接同步等待，用户体验会很差。所以我将它设计成任务化流程，前端发起任务后，后端返回 taskNo，再通过 WebSocket 推送任务进度。这样用户能看到分析进行到哪一步。
```

---

## 10.2 难点二：页面切换后 AI 任务状态丢失

问题：

```text
原来任务状态保存在页面组件中，切换页面后组件卸载，进度会消失。
```

解决：

```text
1. 新增 Pinia 全局 AI 任务中心
2. 使用 localStorage 做轻量持久化
3. 后端提供 running / recent / cancel 接口
4. 右下角显示 AI 任务状态
5. 任务完成后通知用户查看结果
```

答辩讲法：

```text
我发现如果用户在 AI 分析过程中切换页面，页面内的 loading 状态会丢失。为了解决这个问题，我把任务状态上移到全局 Store，并设计了右下角 AI 任务中心，让用户在任何页面都能看到任务状态。
```

---

## 10.3 难点三：AI 返回格式不稳定

问题：

```text
大模型可能返回多余解释、JSON 代码块、字段缺失或分数越界。
```

解决：

```text
1. Prompt 中明确要求 JSON 输出
2. 增加 AiJsonSanitizer
3. 去除代码块
4. 截取 JSON 主体
5. 字段缺失兜底
6. 分数限制在 0-100
```

答辩讲法：

```text
大模型输出并不总是严格稳定，所以我没有直接信任模型返回，而是增加了 JSON 清洗和解析兜底逻辑，比如去掉 markdown 代码块、截取 JSON 主体、字段缺失时给默认值、分数越界时进行限制。
```

---

## 10.4 难点四：RBAC 前后端权限一致

问题：

```text
只隐藏前端菜单不能真正防止越权。
```

解决：

```text
1. 后端使用 Spring Security + @PreAuthorize
2. 前端根据权限动态显示菜单和按钮
3. 普通用户访问管理员接口返回 403
4. 管理员权限由角色权限表控制
```

答辩讲法：

```text
我没有只依赖前端隐藏菜单，而是在后端接口层使用 @PreAuthorize 做真正权限校验。前端菜单和按钮只是体验层控制，安全边界在后端。
```

---

## 10.5 难点五：Docker MySQL init.sql 不重复执行

问题：

```text
Docker MySQL 使用 volume 后，init.sql 只会在首次初始化时执行。
```

解决：

```text
1. 将线上数据库变更写成 migration SQL
2. 部署时手动执行迁移
3. README 和部署文档中记录注意事项
4. 不依赖 init.sql 更新已有线上库
```

答辩讲法：

```text
我部署时遇到一个真实问题：MySQL 容器使用 volume 后，init.sql 不会重复执行。所以后续新增表和权限不能只改 init.sql，需要写迁移 SQL 并手动执行。这也是我项目中学到的一个比较真实的部署经验。
```

---

# 11. 简历项目包装

## 11.1 简历项目名称

```text
InternPilot 智能实习领航员
```

推荐写法：

```text
InternPilot 智能实习领航员｜AI 实习投递与简历优化平台
```

---

## 11.2 简历项目描述

```text
基于 Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API 开发的 AI 实习投递与简历优化平台，面向大学生实习求职场景，提供邮箱验证码注册登录、简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户反馈、管理员后台和 Docker 线上部署等功能。
```

---

## 11.3 简历技术栈

```text
Spring Boot、Spring Security、JWT、MyBatis、MySQL、Redis、WebSocket、DeepSeek API、Vue3、Vite、Pinia、Element Plus、Docker、Nginx
```

---

## 11.4 简历项目职责写法

```text
- 负责系统整体架构设计与核心业务开发，采用 Spring Boot + Vue3 前后端分离架构实现 AI 求职工作台。
- 使用 Spring Security + JWT + RBAC 实现用户认证、角色权限、菜单权限和接口权限控制。
- 接入 DeepSeek API，实现简历岗位匹配分析、岗位推荐、AI 面试题生成和 RAG 知识库问答。
- 使用 WebSocket 实现 AI 分析进度实时推送，并设计全局 AI 任务中心解决页面切换后任务状态丢失问题。
- 使用 Redis 实现邮箱验证码存储、AI 任务状态缓存和 AI 分析缓存，并规范缓存 key 与 TTL。
- 设计 Prompt 版本管理和 AI 模型路由机制，增强 AI JSON 清洗、失败重试、fallback 和日志脱敏能力。
- 接入 Spring Boot Actuator、Validation、AOP 耗时统计、全局异常处理和 Docker healthcheck，提升系统可观测性和工程质量。
- 使用 Docker Compose 部署前端 Nginx、后端服务、MySQL 和 Redis，实现服务器在线访问，并发布 GitHub Release v1.3.1。
```

---

## 11.5 简历亮点短版

适合简历空间不够时使用：

```text
- 基于 Spring Boot + Vue3 + DeepSeek API 实现 AI 简历岗位匹配分析、岗位推荐、AI 面试题生成和 RAG 问答。
- 使用 Spring Security + JWT + RBAC 实现认证授权，结合 WebSocket 实现 AI 任务进度实时推送。
- 设计全局 AI 任务中心、Prompt 版本管理、模型路由、AI 缓存和 JSON 清洗机制，提升 AI 功能稳定性。
- 使用 Redis、Actuator、AOP、Docker Compose、Nginx gzip 和 healthcheck 完成缓存、监控、日志与线上部署优化。
```

---

# 12. 面试高频问题

## 12.1 这个项目是做什么的？

```text
InternPilot 是一个面向大学生实习求职的 AI 求职工作台。用户可以上传简历、管理岗位，然后系统通过 DeepSeek API 分析简历和岗位的匹配度，生成优化建议，也可以推荐岗位、生成面试题，并通过 RAG 知识库回答求职相关问题。系统还包括管理员后台、用户反馈、权限管理和线上部署。
```

---

## 12.2 你为什么做这个项目？

```text
因为大学生找实习时经常不知道自己的简历和岗位是否匹配，也不知道如何针对岗位准备面试。我希望通过 AI 将简历分析、岗位推荐和面试准备串起来，形成一个完整的求职辅助平台。
```

---

## 12.3 你的后端架构是怎么设计的？

```text
后端采用 Spring Boot 分层架构，主要包括 Controller、Service、Mapper、Entity、DTO、VO、Config、Security、Exception 等模块。Controller 负责接收请求，Service 处理业务逻辑，Mapper 访问 MySQL，Redis 用于验证码和任务状态缓存，AI 调用通过 AiClient 抽象接入 DeepSeek 和 Mock。
```

---

## 12.4 JWT 登录流程怎么实现？

```text
用户登录时，后端校验邮箱和密码，校验成功后生成 JWT 返回给前端。前端保存 token，后续请求通过 Authorization Header 携带 token。后端通过 JWT 过滤器解析 token，获取用户身份和权限，放入 SecurityContext，再由 Spring Security 判断接口是否允许访问。
```

---

## 12.5 RBAC 是怎么实现的？

```text
项目中设计了用户、角色、权限三类核心模型。用户和角色是多对多关系，角色和权限也是多对多关系。后端使用 @PreAuthorize 对接口进行权限控制，前端根据用户权限动态显示菜单和按钮。真正的权限校验在后端完成，防止用户绕过前端直接请求管理员接口。
```

---

## 12.6 WebSocket 在项目中解决了什么问题？

```text
AI 分析是耗时任务，如果只用普通 HTTP 请求，用户不知道任务执行到哪一步。所以我使用 WebSocket 推送 AI 分析进度。后端在解析简历、构建上下文、调用 AI、生成报告等阶段推送状态，前端实时更新进度条和任务中心。
```

---

## 12.7 全局 AI 任务中心为什么要做？

```text
最初 AI 任务状态保存在页面组件中，用户切换页面后状态会丢失。为了解决这个问题，我将任务状态上移到 Pinia 全局 Store，并通过 localStorage 做轻量持久化，同时结合后端任务状态查询和 WebSocket，实现页面切换后任务仍然可见，完成后右下角通知用户。
```

---

## 12.8 DeepSeek API 是怎么接入的？

```text
我把 AI 调用抽象成 AiClient 接口，生产环境使用 DeepSeekAiClient，测试环境使用 MockAiClient。业务层不直接依赖具体模型实现，这样方便测试，也方便后续扩展其他模型。API Key 通过环境变量配置，不写死在代码中。
```

---

## 12.9 Prompt 优化做了什么？

```text
我为不同 AI 场景设计了 Prompt 模板版本管理，例如简历分析、岗位推荐、面试题生成和 RAG 问答都有对应模板和版本号。缓存 key 中包含 scenario、model、promptVersion 和 promptHash，避免 Prompt 更新后误命中旧缓存。同时增加 JSON 清洗和解析兜底，提升 AI 输出稳定性。
```

---

## 12.10 Redis 用在什么地方？

```text
Redis 主要用于邮箱验证码存储和过期控制、AI 任务状态缓存、AI 分析缓存等场景。验证码设置 5 分钟 TTL，AI 任务状态设置较长 TTL，缓存 key 统一使用常量和 builder 管理，避免硬编码散落在业务代码中。
```

---

## 12.11 RAG 是怎么做的？

```text
RAG 是检索增强生成。项目中管理员可以维护岗位知识库，系统将文档切分成知识片段。用户提问时，系统先检索相关知识片段，再把上下文和问题一起构造成 Prompt 调用 DeepSeek，这样回答会更贴近系统知识库内容。
```

---

## 12.12 Docker 部署怎么做的？

```text
项目使用 Docker Compose 部署，包含 frontend、backend、mysql、redis 四个服务。前端 Vue 打包后由 Nginx 提供静态资源，并通过 /api 反向代理到后端。后端通过环境变量读取 MySQL、Redis、DeepSeek API Key、邮箱授权码等配置。后来还增加了 Actuator 和 Docker healthcheck 判断服务健康状态。
```

---

## 12.13 你项目中遇到过什么真实问题？

```text
一个真实问题是 Docker MySQL 使用 volume 后，init.sql 只会在数据库第一次初始化时执行。后续新增表或权限时，单纯修改 init.sql 不会影响已有线上数据库。我的解决方式是写 migration SQL，并在部署文档中说明线上数据库需要手动迁移。
```

---

## 12.14 你的项目还有什么不足？

```text
当前项目已经完成主要功能，但仍有一些可以优化的地方。例如 Element Plus 目前仍是全量注册，前端 vendor chunk 偏大；岗位推荐部分仍以规则推荐为主，后续可以进一步接入 AI 解释；系统监控还可以接入更完整的可观测性方案；后续也可以增加投递进度看板和管理员数据大屏。
```

---

# 13. 后续迭代计划

## 13.1 短期可做

```text
1. Element Plus 按需导入，继续优化前端体积
2. 管理员数据大屏
3. 投递进度看板
4. 面试复盘记录
5. 用户反馈统计
```

---

## 13.2 中期可做

```text
1. 多模型 Provider 扩展
2. AI 分析质量评分
3. AI 调用日志后台
4. 更完善的 RAG 检索排序
5. 简历优化版本对比
```

---

## 13.3 长期可做

```text
1. 更完整的 CI/CD 部署流水线优化
2. 系统监控和告警
3. 多租户或学校组织管理
4. 移动端适配
5. 实习投递全流程管理
```

---

# 14. 当前阶段最终检查

提交答辩或简历前确认：

```text
1. GitHub README 最新
2. GitHub Release v1.3.1 已发布
3. 在线地址可访问
4. README 不公开敏感信息
5. main 分支稳定
6. dev 分支可继续开发
7. 服务器服务正常
8. 演示账号策略清晰
9. PPT 截图是最新界面
10. 你能用 1 分钟讲清楚项目
11. 你能用 5 分钟演示完整流程
12. 你能回答 JWT、RBAC、WebSocket、Redis、AI、Docker 相关问题
```
