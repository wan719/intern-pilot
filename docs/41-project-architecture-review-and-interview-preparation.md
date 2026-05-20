
# InternPilot 项目全景复盘、架构理解与答辩/面试准备文档

## 1. 文档背景

InternPilot 智能实习领航员项目已经完成主要开发、部署和 Release 发布。当前系统已具备完整的前后端分离能力、AI 能力、权限体系、用户反馈体系和线上部署能力。

当前阶段不再以“继续堆功能”为主，而是进入：

```text
项目理解
  ↓
架构复盘
  ↓
答辩准备
  ↓
面试表达
  ↓
简历包装
  ↓
后续迭代规划
```

本文件用于帮助开发者系统掌握整个 InternPilot 项目，能够从项目背景、需求分析、架构设计、核心实现、测试部署、项目亮点、问题反思、后续优化等角度完整讲清楚项目。

---

## 2. 当前项目定位

### 2.1 项目名称

```text
InternPilot：智能实习领航员
```

也可以描述为：

```text
面向大学生的 AI 实习投递与简历优化平台
```

---

### 2.2 项目一句话介绍

InternPilot 是一个基于 Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API 构建的 AI 求职工作台，面向大学生实习求职场景，提供简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户中心、管理员后台和用户反馈等功能。

---

### 2.3 项目解决的问题

当前大学生在找实习过程中常见问题包括：

```text
1. 不知道自己的简历和岗位要求是否匹配
2. 不知道简历应该怎么优化
3. 不知道哪些岗位更适合自己
4. 面试准备缺少针对性
5. 岗位信息和求职知识分散
6. 普通求职平台更偏信息展示，缺少个性化 AI 分析
```

InternPilot 试图解决的是：

```text
让学生能够基于自己的简历和目标岗位，获得 AI 驱动的求职分析、岗位推荐和面试准备建议。
```

---

## 3. 项目功能总览

### 3.1 普通用户功能

```text
1. 邮箱验证码注册
2. 邮箱 + 密码登录
3. 用户中心
4. 简历上传与管理
5. 岗位信息管理
6. AI 简历岗位匹配分析
7. WebSocket AI 分析进度展示
8. 全局 AI 任务中心
9. 岗位推荐
10. AI 面试题生成
11. RAG 岗位知识库问答
12. 用户反馈提交
```

---

### 3.2 管理员功能

```text
1. 管理员后台首页
2. 用户管理
3. 角色管理
4. 权限管理
5. 操作日志管理
6. RAG 知识库管理
7. 用户反馈管理
8. 系统数据查看
```

---

### 3.3 系统级能力

```text
1. JWT 登录认证
2. Spring Security 权限控制
3. RBAC 角色权限模型
4. Redis 缓存与验证码存储
5. WebSocket 实时任务进度
6. DeepSeek API 接入
7. Mock AI 测试能力
8. Docker Compose 部署
9. Nginx 前端部署与反向代理
10. MySQL 数据持久化
11. Git 分支开发与 Release 发布
```

---

## 4. 项目技术栈

### 4.1 后端技术栈

```text
Java 17
Spring Boot
Spring Security
MyBatis
MySQL
Redis
WebSocket
JWT
DeepSeek API
JUnit
Mockito / MockMvc
Gradle
Docker
```

---

### 4.2 前端技术栈

```text
Vue 3
Vite
TypeScript
Vue Router
Pinia
Element Plus
Axios
Nginx
```

---

### 4.3 部署技术栈

```text
Linux 服务器
Docker
Docker Compose
Nginx
MySQL 容器
Redis 容器
GitHub
Gitee
GitHub Release
```

---

## 5. 项目开发流程复盘

本项目可以按照完整软件开发生命周期进行复盘。

---

## 5.1 系统规划阶段

### 5.1.1 项目背景

大学生找实习时通常需要反复修改简历、筛选岗位、准备面试，但传统工具往往只提供岗位展示，缺少针对个人简历和岗位要求的智能分析。

InternPilot 以 AI 为核心，围绕“简历—岗位—分析—推荐—面试准备”形成完整闭环。

---

### 5.1.2 项目目标

```text
1. 帮助学生管理简历和岗位信息
2. 使用 AI 分析简历与岗位匹配度
3. 根据简历和岗位生成优化建议
4. 推荐更适合用户的实习岗位
5. 生成针对目标岗位的面试题
6. 提供 RAG 知识库问答能力
7. 提供管理员后台进行系统管理
8. 支持线上部署和演示
```

---

### 5.1.3 目标用户

```text
1. 普通学生用户
   - 上传简历
   - 管理岗位
   - 使用 AI 分析和推荐
   - 生成面试题
   - 提交反馈

2. 管理员
   - 管理用户
   - 管理角色和权限
   - 管理 RAG 知识库
   - 查看操作日志
   - 处理用户反馈
```

---

### 5.1.3.1 系统用例图（UML）

```mermaid
flowchart LR
    Student["<<Actor>>\n学生用户"]
    Admin["<<Actor>>\n管理员"]
    DeepSeek["<<External System>>\nDeepSeek API"]
    Redis["<<External System>>\nRedis"]
    SMTP["<<External System>>\nSMTP 邮箱服务"]

    subgraph System["InternPilot 智能实习领航员"]
        UC_Register(("邮箱验证码注册"))
        UC_Login(("登录与 JWT 鉴权"))
        UC_Profile(("维护用户中心"))
        UC_Resume(("管理简历与版本"))
        UC_Job(("管理岗位 JD"))
        UC_Analysis(("AI 简历岗位匹配分析"))
        UC_Report(("查看分析报告"))
        UC_Interview(("生成 AI 面试题"))
        UC_Recommend(("生成岗位推荐"))
        UC_Application(("管理投递记录"))
        UC_Feedback(("提交用户反馈"))
        UC_Task(("查看 AI 任务中心"))

        UC_AdminDashboard(("查看后台看板"))
        UC_User(("用户管理"))
        UC_Role(("角色管理"))
        UC_Permission(("权限管理"))
        UC_Log(("操作日志管理"))
        UC_Rag(("RAG 知识库管理"))
        UC_FeedbackAdmin(("反馈处理"))
    end

    Student --> UC_Register
    Student --> UC_Login
    Student --> UC_Profile
    Student --> UC_Resume
    Student --> UC_Job
    Student --> UC_Analysis
    Student --> UC_Report
    Student --> UC_Interview
    Student --> UC_Recommend
    Student --> UC_Application
    Student --> UC_Feedback
    Student --> UC_Task

    Admin --> UC_Login
    Admin --> UC_AdminDashboard
    Admin --> UC_User
    Admin --> UC_Role
    Admin --> UC_Permission
    Admin --> UC_Log
    Admin --> UC_Rag
    Admin --> UC_FeedbackAdmin

    UC_Register -.验证码存储.-> Redis
    UC_Register -.发送邮件.-> SMTP
    UC_Analysis -.调用模型.-> DeepSeek
    UC_Interview -.调用模型.-> DeepSeek
    UC_Recommend -.调用模型.-> DeepSeek
    UC_Rag -.向量/知识检索.-> Redis
```

说明：Mermaid 没有原生 usecase 图语法，这里用 UML actor + use case 语义的 flowchart 表达，适合 GitHub/Gitee 直接渲染。

---

### 5.1.4 可行性分析

技术可行性：

```text
Spring Boot 适合构建后端 API 服务。
Vue3 适合构建前端交互页面。
MySQL 适合存储用户、简历、岗位、反馈等结构化数据。
Redis 适合存储验证码、任务状态和缓存。
DeepSeek API 可以提供 AI 文本生成能力。
Docker Compose 可以完成一体化部署。
```

时间可行性：

```text
项目按 docs/xx 设计文档逐步迭代，每一阶段聚焦一个功能模块，降低开发风险。
```

成本可行性：

```text
服务器、数据库、Redis、AI API 均可通过低成本方式部署和调用，适合作为学生项目。
```

团队能力可行性：

```text
项目采用主流 Java 全栈技术栈，适合软件工程学生学习和实践。
```

---

## 5.2 需求分析阶段

### 5.2.1 核心业务流程

#### 用户注册登录流程

```text
用户进入注册页
  ↓
输入邮箱和密码
  ↓
请求发送邮箱验证码
  ↓
后端生成验证码并存入 Redis
  ↓
QQ 邮箱 SMTP 发送验证码
  ↓
用户提交验证码
  ↓
后端校验验证码
  ↓
创建用户
  ↓
用户登录
  ↓
后端生成 JWT
  ↓
前端保存 token 并进入工作台
```

---

#### AI 简历岗位匹配分析流程

```text
用户上传简历
  ↓
用户创建或选择目标岗位
  ↓
点击 AI 分析
  ↓
后端创建分析任务
  ↓
WebSocket 推送任务进度
  ↓
后端构造 Prompt
  ↓
调用 DeepSeek API
  ↓
解析 AI 返回结果
  ↓
保存分析报告
  ↓
前端右下角任务中心提示完成
  ↓
用户查看分析报告
```

---

#### 岗位推荐流程

```text
用户选择简历
  ↓
点击生成岗位推荐
  ↓
前端创建全局 AI 任务
  ↓
后端读取简历和岗位信息
  ↓
调用 AI / 推荐逻辑
  ↓
返回推荐结果
  ↓
任务中心提示生成完成
  ↓
用户查看推荐岗位
```

---

#### 面试题生成流程

```text
用户选择简历和目标岗位
  ↓
点击生成面试题
  ↓
前端创建全局 AI 任务
  ↓
后端构造面试题 Prompt
  ↓
调用 DeepSeek
  ↓
解析并保存面试题
  ↓
任务中心提示完成
  ↓
用户查看面试题
```

---

#### 用户反馈流程

```text
用户点击反馈入口
  ↓
填写反馈类型、标题、内容
  ↓
提交反馈
  ↓
后端保存 user_feedback
  ↓
管理员后台查看反馈
  ↓
管理员修改状态或回复
```

---

#### 邮箱验证码注册登录流程（按代码）

```text
前端 Register.vue 请求 POST /api/auth/captcha/register
  ↓
AuthController.sendRegisterCaptcha
  ↓
AuthServiceImpl 限制当前阶段仅支持 EMAIL
  ↓
CaptchaService 校验邮箱格式、是否已注册、冷却时间、每日次数
  ↓
根据 AUTH_EMAIL_CAPTCHA_PROVIDER 选择 smtp 或 mock
  ↓
EmailCaptchaSender / MockCaptchaSender 发送验证码
  ↓
Redis 写入 auth:captcha:*、auth:captcha:cooldown:*、auth:captcha:daily:*
  ↓
用户提交 POST /api/auth/register
  ↓
AuthServiceImpl 校验验证码、加密密码、创建 user、绑定 USER 角色
  ↓
用户 POST /api/auth/login
  ↓
校验邮箱、密码、账号状态
  ↓
JwtTokenProvider 生成 token
  ↓
前端保存 token，进入用户工作台
```

---

#### JWT 鉴权流程（按代码）

```text
前端 request.ts 从 localStorage 读取 token
  ↓
请求头携带 Authorization: Bearer token
  ↓
SecurityConfig 放行 /api/auth/**、/api/health、Knife4j、uploads、WebSocket 握手等白名单
  ↓
JwtAuthenticationFilter 解析 Bearer token
  ↓
JwtTokenProvider 校验签名和过期时间
  ↓
CustomUserDetailsService 根据 username 查询用户、角色和权限
  ↓
构造 UsernamePasswordAuthenticationToken
  ↓
写入 SecurityContext
  ↓
进入 Controller 和 @PreAuthorize 权限校验
```

---

#### RBAC 权限校验流程（按代码）

```text
数据库 user / role / permission / user_role / role_permission
  ↓
用户登录后 AuthServiceImpl 返回 roles 和 permissions
  ↓
前端 auth Store 保存用户权限
  ↓
router.beforeEach 根据 meta.permission 判断页面访问权限
  ↓
PermissionButton / hasPermission 控制按钮展示
  ↓
后端接口使用 @PreAuthorize("hasAuthority('xxx')")
  ↓
Spring Security 基于 CustomUserDetails.authorities 做最终权限判断
```

说明：前端权限用于体验，后端 `@PreAuthorize` 才是安全边界。

---

#### WebSocket 进度推送流程（按代码）

```text
前端 AnalysisMatch.vue 创建分析任务
  ↓
POST /api/analysis/tasks
  ↓
AnalysisTaskServiceImpl 创建 analysis_task 并返回 taskNo
  ↓
前端 analysisSocket.ts 使用 SockJS + STOMP 连接 /ws/analysis
  ↓
订阅 /topic/analysis/{taskNo}
  ↓
后端 AnalysisProgressPublisherImpl 使用 SimpMessagingTemplate 推送状态
  ↓
同时将进度消息写入 Redis：ai:analysis:task:{taskNo}
  ↓
前端收到消息后更新页面进度和 aiTaskCenter Store
  ↓
WebSocket 异常时前端使用 getAnalysisTaskDetailApi 轮询兜底
```

---

#### DeepSeek API 调用流程（按代码）

```text
业务 Service 构造 Prompt
  ↓
调用 AiClient.chat(prompt)
  ↓
生产环境 ConditionalOnProperty 选择 DeepSeekAiClient
  ↓
DeepSeekAiClient 根据 Prompt 识别 AiScenarioEnum
  ↓
RAG_QA 使用 AI_PRO_MODEL，其他常规任务使用 AI_MODEL
  ↓
构造 OpenAI 兼容 /chat/completions 请求
  ↓
通过 DEEPSEEK_API_KEY 设置 Bearer Token
  ↓
要求 JSON 场景返回 response_format=json_object
  ↓
解析 choices[0].message.content
  ↓
业务层解析 JSON、保存报告或生成结果
```

---

#### RAG 检索增强流程（按代码）

```text
管理员在 AdminRagKnowledgeList.vue 新增知识文档
  ↓
POST /api/admin/rag/knowledge
  ↓
RagKnowledgeServiceImpl 保存 rag_knowledge_document
  ↓
TextChunkUtils 将 content 切片
  ↓
EmbeddingClient 生成向量
  ↓
VectorUtils.toJson 后保存到 rag_knowledge_chunk.embedding
  ↓
AI 分析或面试题生成前根据简历/JD构造 query
  ↓
RagKnowledgeServiceImpl.search 生成 queryVector
  ↓
读取启用 chunk，计算余弦相似度，返回 TopK
  ↓
业务 Prompt 拼接 RAG 上下文后调用 DeepSeek
```

---

#### 全局 AI 任务中心流程（按前端代码）

```text
AI 分析 / 岗位推荐 / 面试题生成页面创建任务
  ↓
useAiTaskCenterStore.createTask 或 upsertByTaskNo
  ↓
Store 维护任务标题、状态、进度、来源页、结果路径
  ↓
localStorage 保存轻量任务记录，刷新后 restoreFromStorage
  ↓
AI 分析任务同步后端 running / recent / cancel 接口
  ↓
岗位推荐和面试题当前以本地任务追踪统一体验
  ↓
AiTaskFloat 显示右下角入口和红点
  ↓
AiTaskDrawer 展示任务列表、结果入口、清除与取消操作
```

---

#### 用户反馈流程（按代码）

```text
用户点击 FeedbackFloat 打开 FeedbackDrawer
  ↓
填写类型、标题、内容
  ↓
POST /api/feedback
  ↓
FeedbackService 保存 user_feedback，记录页面路径和浏览器信息
  ↓
用户可 GET /api/feedback/my 查看自己的反馈
  ↓
管理员进入 /admin/feedback
  ↓
AdminFeedbackController 基于 feedback:read/write/delete 权限管理反馈
  ↓
管理员可筛选、查看详情、改状态、回复、删除
```

---

#### Docker 部署流程（按配置）

```text
服务器准备 Docker 和 Docker Compose
  ↓
进入 deploy，复制 .env.example 为 .env
  ↓
填写 MySQL、Redis、JWT、DeepSeek、SMTP 等真实环境变量
  ↓
docker compose --env-file .env -f docker-compose.yml up -d --build
  ↓
mysql 服务挂载 init.sql，只在首次初始化 volume 时执行
  ↓
redis 服务启用 requirepass
  ↓
backend 读取 prod 环境变量，连接 mysql / redis / DeepSeek
  ↓
frontend 构建 Vue dist，由 Nginx 提供静态资源
  ↓
Nginx 代理 /api、/uploads、/ws 到 backend
  ↓
线上验收登录、AI 分析、WebSocket、RAG、反馈和管理员后台
```

注意：已有 MySQL volume 不会自动重新执行 `init.sql`，最终发布阶段新增反馈表和权限需要手动执行 `V40__final_release_update.sql`。

---

### 5.2.2 核心流程序列图（UML）

#### 邮箱验证码注册与登录序列图

```mermaid
sequenceDiagram
    actor User as 学生用户
    participant FE as Vue 前端
    participant Auth as AuthController
    participant Captcha as CaptchaService
    participant Redis as Redis
    participant Mail as SMTP 邮箱服务
    participant UserSvc as AuthServiceImpl
    participant DB as MySQL
    participant JWT as JwtTokenProvider

    User->>FE: 输入邮箱并点击发送验证码
    FE->>Auth: POST /api/auth/captcha/register
    Auth->>UserSvc: sendRegisterCaptcha(request)
    UserSvc->>Captcha: sendRegisterCaptcha(request)
    Captcha->>DB: 查询邮箱是否已注册
    Captcha->>Redis: 检查 cooldown / daily / fail
    Captcha->>Mail: 发送验证码
    Captcha->>Redis: 保存验证码和冷却时间
    Auth-->>FE: Result.success()

    User->>FE: 输入验证码和密码提交注册
    FE->>Auth: POST /api/auth/register
    Auth->>UserSvc: register(request)
    UserSvc->>Captcha: validateCaptcha(email, code)
    Captcha->>Redis: 校验并删除验证码
    UserSvc->>DB: 写入 user / user_role
    UserSvc-->>FE: 用户信息

    User->>FE: 邮箱密码登录
    FE->>Auth: POST /api/auth/login
    Auth->>UserSvc: login(request)
    UserSvc->>DB: 查询用户、校验密码和状态
    UserSvc->>JWT: generateToken(userId, username, role)
    UserSvc-->>FE: token + user
    FE-->>User: 进入工作台
```

---

#### JWT 与 RBAC 权限校验序列图

```mermaid
sequenceDiagram
    actor User as 已登录用户
    participant FE as Vue 前端
    participant Router as Vue Router
    participant Req as Axios Request
    participant Filter as JwtAuthenticationFilter
    participant UDS as CustomUserDetailsService
    participant DB as MySQL
    participant Sec as Spring Security
    participant API as Controller

    User->>FE: 访问受保护页面
    FE->>Router: beforeEach(to)
    Router->>FE: 检查 token 和 meta.permission
    FE->>Req: 发起 API 请求
    Req->>Req: 注入 Authorization: Bearer token
    Req->>Filter: HTTP 请求进入过滤器链
    Filter->>Filter: 解析并校验 JWT
    Filter->>UDS: loadUserByUsername(username)
    UDS->>DB: 查询 user、roles、permissions
    UDS-->>Filter: CustomUserDetails(authorities)
    Filter->>Sec: 写入 SecurityContext
    Sec->>API: 执行 @PreAuthorize 权限校验
    API-->>FE: 返回业务数据或 403
```

---

#### AI 简历分析 + WebSocket 进度序列图

```mermaid
sequenceDiagram
    actor User as 学生用户
    participant FE as AnalysisMatch.vue
    participant TaskStore as aiTaskCenter Store
    participant TaskAPI as AnalysisTaskController
    participant TaskSvc as AnalysisTaskServiceImpl
    participant AnalysisSvc as AnalysisServiceImpl
    participant Publisher as AnalysisProgressPublisher
    participant WS as WebSocket/STOMP
    participant Redis as Redis
    participant AI as DeepSeekAiClient
    participant DB as MySQL

    User->>FE: 点击开始 AI 匹配
    FE->>TaskAPI: POST /api/analysis/tasks
    TaskAPI->>TaskSvc: createTask(request)
    TaskSvc->>DB: 插入 analysis_task(PENDING)
    TaskSvc->>Publisher: publish(PENDING)
    Publisher->>WS: /topic/analysis/{taskNo}
    Publisher->>Redis: ai:analysis:task:{taskNo}
    TaskSvc-->>FE: taskNo
    FE->>TaskStore: upsertByTaskNo(taskNo)
    FE->>WS: 订阅 /topic/analysis/{taskNo}

    TaskSvc->>TaskSvc: 异步线程 executeTask
    TaskSvc->>Publisher: publish(PARSING_RESUME)
    TaskSvc->>Publisher: publish(BUILDING_CONTEXT)
    TaskSvc->>Publisher: publish(CALLING_AI)
    TaskSvc->>AnalysisSvc: matchForUser(request, userId)
    AnalysisSvc->>DB: 读取简历、岗位、历史报告
    AnalysisSvc->>DB: 检索 RAG 上下文
    AnalysisSvc->>AI: chat(prompt)
    AI-->>AnalysisSvc: JSON 分析结果
    AnalysisSvc->>DB: 保存 analysis_report
    TaskSvc->>Publisher: publish(GENERATING_REPORT)
    TaskSvc->>Publisher: publish(COMPLETED, reportId)
    WS-->>FE: 推送进度和 reportId
    FE->>TaskStore: 更新进度、结果入口和红点
    User->>FE: 点击查看报告
```

---

#### RAG 知识库检索增强序列图

```mermaid
sequenceDiagram
    actor Admin as 管理员
    participant AdminFE as AdminRagKnowledgeList.vue
    participant RagAPI as AdminRagKnowledgeController
    participant RagSvc as RagKnowledgeServiceImpl
    participant Embed as EmbeddingClient
    participant DB as MySQL
    participant AnalysisSvc as AnalysisServiceImpl
    participant AI as DeepSeekAiClient

    Admin->>AdminFE: 新增知识文档
    AdminFE->>RagAPI: POST /api/admin/rag/knowledge
    RagAPI->>RagSvc: create(request)
    RagSvc->>DB: 保存 rag_knowledge_document
    RagSvc->>RagSvc: TextChunkUtils.splitToChunks
    loop 每个 chunk
        RagSvc->>Embed: embed(chunkContent)
        Embed-->>RagSvc: embedding
        RagSvc->>DB: 保存 rag_knowledge_chunk
    end

    AnalysisSvc->>RagSvc: search(query, topK)
    RagSvc->>Embed: embed(query)
    RagSvc->>DB: 查询启用 chunk
    RagSvc->>RagSvc: VectorUtils.cosineSimilarity 排序
    RagSvc-->>AnalysisSvc: TopK 相关知识片段
    AnalysisSvc->>AI: Prompt + RAG 上下文
    AI-->>AnalysisSvc: 增强后的 AI 结果
```

---

#### 用户反馈处理序列图

```mermaid
sequenceDiagram
    actor User as 学生用户
    actor Admin as 管理员
    participant FE as FeedbackDrawer.vue
    participant FeedbackAPI as FeedbackController
    participant FeedbackSvc as FeedbackServiceImpl
    participant DB as MySQL
    participant AdminFE as AdminFeedbackList.vue
    participant AdminAPI as AdminFeedbackController

    User->>FE: 填写反馈类型、标题、内容
    FE->>FeedbackAPI: POST /api/feedback
    FeedbackAPI->>FeedbackSvc: createFeedback(request)
    FeedbackSvc->>DB: 保存 user_feedback
    FeedbackSvc-->>FE: 反馈记录

    Admin->>AdminFE: 进入用户反馈管理
    AdminFE->>AdminAPI: GET /api/admin/feedback
    AdminAPI->>FeedbackSvc: listAllFeedbacks(type, status)
    FeedbackSvc->>DB: 查询反馈列表
    FeedbackSvc-->>AdminFE: 反馈数据
    Admin->>AdminFE: 修改状态或回复
    AdminFE->>AdminAPI: PUT /api/admin/feedback/{id}/status 或 reply
    AdminAPI->>FeedbackSvc: updateStatus / reply
    FeedbackSvc->>DB: 更新反馈状态和回复
```

---

## 5.3 系统设计阶段

### 5.3.1 总体架构

```text
浏览器
  ↓
Vue3 前端
  ↓
Nginx 反向代理
  ↓
Spring Boot 后端
  ↓
MySQL / Redis / DeepSeek API
```

---

### 5.3.2 架构图

```mermaid
flowchart LR
    User[用户浏览器] --> Frontend[Vue3 + Vite 前端]
    Frontend --> Nginx[Nginx]
    Nginx --> Backend[Spring Boot 后端]
    Backend --> MySQL[(MySQL)]
    Backend --> Redis[(Redis)]
    Backend --> DeepSeek[DeepSeek API]
    Backend --> WebSocket[WebSocket 进度推送]
    WebSocket --> Frontend
```

---

### 5.3.3 后端分层设计

后端采用典型 Spring Boot 分层结构：

```text
controller
  接收 HTTP 请求，处理参数，返回统一响应

service
  编写业务逻辑，组织领域流程

ai
  封装 AI Client、Prompt 构造、AI 结果解析、缓存 Key 和场景枚举

captcha
  封装邮箱验证码、Mock 验证码和短信验证码发送能力

mapper
  访问数据库

entity
  对应数据库表

dto
  接收前端请求参数

vo / response
  返回前端展示数据

config
  存放配置类，例如 Security、WebSocket、Redis

security
  处理 JWT、权限认证、用户身份解析

annotation / aspect
  自定义操作日志注解与 AOP 记录逻辑

exception
  统一异常处理

common
  统一返回结果、分页结构、常量

enums
  业务状态、类型、权限相关枚举

runner
  系统启动后的初始化或补偿任务，例如 RAG 示例知识初始化
```

---

### 5.3.4 前端分层设计

前端采用 Vue3 工程结构：

```text
views
  页面级组件

components
  可复用业务组件和通用组件

api
  封装后端接口请求

router
  前端路由配置

stores
  Pinia 全局状态管理

utils
  工具函数，例如 WebSocket、token、格式化

assets
  图片、图标、样式资源

types
  第三方库类型补充

styles
  全局样式和响应式样式
```

---

### 5.3.5 架构核对结论

本次按当前代码核对，实际结构与 README / 文档总体一致，但需要注意以下更精确的表达：

```text
1. 后端不仅有 controller / service / mapper / entity / dto / vo，还单独拆出了 ai、captcha、annotation、aspect、enums、runner 等包。
2. AI 相关能力没有散落在业务 Service 中，而是集中在 ai/client、ai/prompt、ai/parser、ai/scenario、ai/cache 下。
3. 验证码能力位于 captcha 包和 service/auth/CaptchaService 中，当前业务层仅开放邮箱验证码注册。
4. 管理员后台接口主要位于 controller/admin，下辖用户、角色、权限、操作日志、RAG 知识库、用户反馈等能力。
5. 前端目录比早期文档更完整，新增了 components/ai、components/feedback、styles、types、utils/useResponsiveSize 等。
6. README 已覆盖当前核心功能：AI 任务中心、用户反馈、管理员独立后台、多端适配、Docker 部署和 V40 数据库迁移说明。
```

当前需要持续留意的小问题：

```text
1. docs/41 早期草稿中部分表名写成了 job、job_recommendation、rag_document 等泛化名称，实际代码中是 job_description、job_recommendation_batch、job_recommendation_item、rag_knowledge_document、rag_knowledge_chunk。
2. 岗位推荐和面试题生成当前前端已接入全局 AI 任务中心，但后端主要仍是同步业务接口，尚未完全统一到 analysis_task 这种后端异步任务表。
3. RAG 当前主要用于知识管理、检索和增强 AI 分析/面试题上下文，严格意义上的独立“用户侧 RAG 问答页”仍可作为后续优化点。
```

---

### 5.3.6 数据库核心模块

主要表可以按领域分为：

```text
用户与权限：
- user
- role
- permission
- user_role
- role_permission

简历与岗位：
- resume
- resume_version
- job_description
- application_record

AI 分析：
- analysis_task
- analysis_report

岗位推荐：
- job_recommendation_batch
- job_recommendation_item

面试题：
- interview_question_report
- interview_question

RAG：
- rag_knowledge_document
- rag_knowledge_chunk

系统管理：
- system_operation_log
- user_feedback
```

实际表名以项目代码为准。

---

## 6. 核心模块复盘

## 6.1 认证与登录模块

### 6.1.1 技术点

```text
Spring Security
JWT
Redis
QQ 邮箱 SMTP
BCrypt 密码加密
```

---

### 6.1.2 设计思路

登录认证采用 JWT 无状态认证。

流程：

```text
用户登录
  ↓
后端校验账号密码
  ↓
生成 JWT
  ↓
前端保存 token
  ↓
后续请求携带 Authorization Header
  ↓
后端 JWT Filter 解析 token
  ↓
设置 SecurityContext
  ↓
进入 Controller
```

---

### 6.1.3 面试讲法

可以这样讲：

```text
我在项目中使用 Spring Security + JWT 实现前后端分离认证。用户登录成功后，后端生成 JWT 返回给前端，前端在后续请求中通过 Authorization Header 携带 token。后端通过自定义 JWT 过滤器解析 token，获取用户身份和权限，并放入 SecurityContext。这样后端接口可以通过认证信息判断当前用户身份，并结合 RBAC 控制接口访问权限。
```

---

## 6.2 RBAC 权限模块

### 6.2.1 RBAC 模型

RBAC 的核心是：

```text
用户 User
  ↓
用户拥有角色 Role
  ↓
角色拥有权限 Permission
  ↓
权限控制接口和页面按钮
```

---

### 6.2.2 项目中的权限控制

后端：

```text
Spring Security + @PreAuthorize
```

前端：

```text
路由权限
菜单权限
按钮权限
```

---

### 6.2.3 权限示例

```text
admin:dashboard
user:read
user:update
role:read
role:update
permission:read
rag:read
rag:manage
operation-log:read
feedback:read
feedback:write
feedback:delete
```

---

### 6.2.4 面试讲法

```text
项目中我实现了 RBAC 权限模型。数据库中用户、角色、权限通过中间表建立多对多关系。后端接口使用 Spring Security 的 @PreAuthorize 进行权限校验，前端根据当前用户权限动态控制菜单和按钮显示。这样可以做到普通用户和管理员拥有不同的访问范围，同时避免只依赖前端隐藏菜单造成越权风险。
```

---

## 6.3 AI 简历分析模块

### 6.3.1 模块目标

AI 简历分析用于判断用户简历和目标岗位之间的匹配程度，并生成优化建议。

---

### 6.3.2 核心流程

```text
创建分析任务
  ↓
保存任务状态
  ↓
WebSocket 推送进度
  ↓
构造 Prompt
  ↓
调用 DeepSeek
  ↓
解析结果
  ↓
保存报告
  ↓
通知前端完成
```

---

### 6.3.2.1 AI 分析模块类图（UML）

```mermaid
classDiagram
    class AnalysisTaskController {
        -AnalysisTaskService analysisTaskService
        +createTask(request) Result
        +getTaskDetail(taskNo) Result
        +listRunningTasks() Result
        +cancelTask(taskNo) Result
        +listRecentTasks(limit) Result
    }

    class AnalysisController {
        -AnalysisService analysisService
        +match(request) Result
        +listReports(params) Result
        +getReportDetail(id) Result
        +deleteReport(id) Result
    }

    class AnalysisTaskService {
        <<interface>>
        +createTask(request)
        +getTaskDetail(taskNo)
        +listRunningTasks()
        +cancelTask(taskNo)
        +listRecentTasks(limit)
    }

    class AnalysisTaskServiceImpl {
        -AnalysisTaskMapper analysisTaskMapper
        -AnalysisService analysisService
        -AnalysisProgressPublisher progressPublisher
        -Executor analysisTaskExecutor
        +createTask(request)
        +executeTask(taskNo, userId)
        +cancelTask(taskNo)
        -updateProgress(task, status, progress)
    }

    class AnalysisService {
        <<interface>>
        +match(request)
        +matchForUser(request, userId)
        +listReports(params)
        +getReportDetail(reportId)
        +deleteReport(reportId)
    }

    class AnalysisServiceImpl {
        -ResumeMapper resumeMapper
        -JobDescriptionMapper jobDescriptionMapper
        -AnalysisReportMapper analysisReportMapper
        -AiClient aiClient
        -RagKnowledgeService ragKnowledgeService
        +matchForUser(request, userId)
        -buildRagContext(resumeText, job)
        -buildPrompt(resume, job, ragContext)
        -parseAiResult(content)
    }

    class AnalysisProgressPublisher {
        <<interface>>
        +publish(taskNo, userId, status, progress, message, reportId, errorMessage)
    }

    class AnalysisProgressPublisherImpl {
        -SimpMessagingTemplate messagingTemplate
        -RedisTemplate redisTemplate
        +publish(...)
    }

    class AiClient {
        <<interface>>
        +chat(prompt) String
    }

    class DeepSeekAiClient {
        -AiProperties aiProperties
        -RestTemplate restTemplate
        +chat(prompt) String
        -detectScenario(prompt)
        -selectModel(scenario)
    }

    class MockAiClient {
        +chat(prompt) String
    }

    class RagKnowledgeService {
        <<interface>>
        +search(request)
    }

    class AnalysisTask {
        +Long id
        +String taskNo
        +Long userId
        +Long resumeId
        +Long jobId
        +String status
        +Integer progress
        +Long reportId
        +String errorMessage
    }

    class AnalysisReport {
        +Long id
        +Long userId
        +Long resumeId
        +Long jobId
        +Integer matchScore
        +String strengths
        +String weaknesses
        +String suggestions
    }

    class AiTaskCenterStore {
        +tasks
        +createTask(options)
        +upsertByTaskNo(options)
        +updateTask(id, updates)
        +syncBackendTasks()
        +restoreFromStorage()
    }

    AnalysisTaskController --> AnalysisTaskService
    AnalysisController --> AnalysisService
    AnalysisTaskService <|.. AnalysisTaskServiceImpl
    AnalysisService <|.. AnalysisServiceImpl
    AnalysisProgressPublisher <|.. AnalysisProgressPublisherImpl
    AiClient <|.. DeepSeekAiClient
    AiClient <|.. MockAiClient
    AnalysisTaskServiceImpl --> AnalysisService
    AnalysisTaskServiceImpl --> AnalysisProgressPublisher
    AnalysisTaskServiceImpl --> AnalysisTask
    AnalysisServiceImpl --> AiClient
    AnalysisServiceImpl --> RagKnowledgeService
    AnalysisServiceImpl --> AnalysisReport
    AnalysisProgressPublisherImpl --> AnalysisTask
    AiTaskCenterStore ..> AnalysisTaskController : 调用任务接口
```

说明：类图重点展示 AI 分析链路的职责边界。后端 `analysis_task` 负责任务状态，`analysis_report` 负责最终结果；前端 `AiTaskCenterStore` 负责跨页面展示和持久化任务体验。

---

### 6.3.3 为什么需要任务表

AI 分析是耗时操作，不能简单用同步接口处理。

任务表的作用：

```text
1. 保存任务状态
2. 支持页面刷新后查询任务
3. 支持 WebSocket 推送进度
4. 支持任务失败记录
5. 支持任务取消
6. 支持全局 AI 任务中心展示
```

---

### 6.3.4 面试讲法

```text
AI 分析属于耗时任务，如果直接同步等待，会导致用户体验差，也容易超时。因此我设计了 analysis_task 任务表，前端发起分析后，后端先创建任务并返回 taskNo，然后异步执行分析流程。执行过程中通过 WebSocket 推送 PENDING、PARSING_RESUME、BUILDING_CONTEXT、CALLING_AI、GENERATING_REPORT、SUCCESS、FAILED 等状态，前端根据状态实时更新进度条。后续又加入全局 AI 任务中心，使用户切换页面后任务状态仍然可见。
```

---

## 6.4 WebSocket AI 进度模块

### 6.4.1 使用原因

AI 任务不是瞬间完成的，用户需要知道当前进度。

WebSocket 适合：

```text
1. 实时推送任务状态
2. 避免前端频繁轮询
3. 提升用户等待体验
```

---

### 6.4.2 设计重点

```text
1. 后端任务状态变更时推送消息
2. 前端订阅对应任务
3. 页面进度条实时更新
4. Redis 保存任务状态作为兜底
5. 页面切换后任务中心继续显示
```

---

### 6.4.3 面试讲法

```text
我使用 WebSocket 实现 AI 分析进度实时展示。后端在任务执行的关键阶段发布进度消息，前端建立 WebSocket 连接并订阅任务状态。相比前端轮询，WebSocket 能减少无效请求，并且能让用户更及时地看到任务状态。为了避免页面切换导致状态丢失，我又把任务状态上移到 Pinia 全局任务中心，并使用 localStorage 做轻量持久化。
```

---

## 6.5 DeepSeek API 接入模块

### 6.5.1 设计目标

```text
1. 线上使用 DeepSeek 真接口
2. 测试环境保留 MockAiClient
3. 不在代码中硬编码 API Key
4. 不同 AI 场景可以使用不同 Prompt
5. 为后续模型路由预留扩展空间
```

---

### 6.5.2 Provider 设计思想

AI 调用应该抽象为接口：

```text
AiClient
  ↓
DeepSeekAiClient
  ↓
MockAiClient
```

这样可以实现：

```text
1. 测试环境用 Mock
2. 生产环境用 DeepSeek
3. 后续接入其他模型时不影响业务层
```

---

### 6.5.3 面试讲法

```text
我没有把 DeepSeek 调用直接写死在业务代码里，而是抽象了 AI Client。线上配置使用 DeepSeekAiClient，测试环境可以切换到 MockAiClient。这样做的好处是业务层不依赖具体模型供应商，后续如果接入其他大模型，可以通过新增 Provider 实现扩展。同时 API Key 通过环境变量配置，避免泄露到代码仓库。
```

---

## 6.6 RAG 岗位知识库模块

### 6.6.1 RAG 的作用

RAG 用于让系统基于已有岗位知识库和文档进行问答，而不是完全依赖模型自身记忆。

---

### 6.6.2 基本流程

```text
管理员上传知识文档
  ↓
后端解析文档
  ↓
切分 chunk
  ↓
存储知识片段
  ↓
用户提问
  ↓
检索相关 chunk
  ↓
拼接上下文
  ↓
调用 DeepSeek
  ↓
返回问答结果
```

---

### 6.6.3 面试讲法

```text
RAG 模块的核心思路是先把岗位相关知识文档解析并切分成 chunk，用户提问时先检索相关知识片段，再把检索结果和用户问题一起构造成 Prompt 发送给大模型。这样可以让回答更贴近系统知识库内容，而不是完全依赖模型自身知识。
```

---

## 6.7 全局 AI 任务中心模块

### 6.7.1 解决的问题

原本 AI 分析、岗位推荐、面试题生成等功能的 loading 状态只保存在页面组件里。用户切换页面后，组件卸载，任务状态就会消失。

全局 AI 任务中心解决：

```text
1. 页面切换后任务状态不丢失
2. 任务完成后右下角通知
3. 用户可以查看结果
4. 用户可以停止或放弃任务
5. 多个 AI 任务可以统一管理
```

---

### 6.7.2 设计思路

前端：

```text
Pinia aiTaskCenter Store
  ↓
localStorage 轻量持久化
  ↓
AiTaskFloat 右下角入口
  ↓
AiTaskDrawer 任务列表
```

后端：

```text
复用 analysis_task
支持 running / recent / cancel 接口
```

---

### 6.7.3 面试讲法

```text
我在项目后期发现 AI 功能存在一个体验问题：用户切换页面后，页面内 loading 状态会丢失。为了解决这个问题，我设计了全局 AI 任务中心，把任务状态从页面组件上移到 Pinia 全局 Store，并使用 localStorage 做轻量持久化。对于已有后端任务的 AI 分析，任务中心会同步后端任务状态；对于岗位推荐和面试题生成，先通过前端任务追踪实现体验统一。这样用户在任意页面都能看到 AI 任务进度，并在完成后收到右下角通知。
```

---

## 6.8 用户反馈模块

### 6.8.1 模块价值

用户反馈模块让系统形成产品闭环：

```text
用户发现问题
  ↓
提交反馈
  ↓
管理员查看
  ↓
处理或回复
  ↓
后续迭代优化
```

---

### 6.8.2 设计内容

普通用户：

```text
提交反馈
查看我的反馈
查看反馈详情
```

管理员：

```text
查看全部反馈
筛选反馈
修改状态
回复反馈
删除无效反馈
```

---

### 6.8.3 面试讲法

```text
用户反馈功能是为了让系统具备真实产品的迭代闭环。普通用户可以在使用过程中提交功能异常、使用建议、AI 结果不准确等反馈，系统会自动记录反馈类型、标题、内容、当前页面和浏览器信息。管理员可以在后台查看、处理和回复反馈。这个模块不仅提升用户体验，也能为后续产品迭代提供依据。
```

---

## 6.9 Docker 部署模块

### 6.9.1 部署结构

线上部署使用 Docker Compose 管理：

```text
frontend
backend
mysql
redis
```

---

### 6.9.2 请求链路

```text
用户浏览器
  ↓
Nginx 前端服务
  ↓
/api 反向代理到 Spring Boot
  ↓
Spring Boot 访问 MySQL / Redis / DeepSeek
```

---

### 6.9.3 重要经验

```text
Docker MySQL 使用 volume 后，init.sql 只会在第一次初始化时执行。
后续新增表、字段、权限，不能只改 init.sql，必须手动执行 migration SQL。
```

---

### 6.9.4 面试讲法

```text
项目使用 Docker Compose 部署，包含前端 Nginx、后端 Spring Boot、MySQL 和 Redis 四个服务。前端构建后由 Nginx 提供静态资源，并将 /api 请求反向代理到后端。后端通过环境变量读取数据库、Redis、DeepSeek API Key、邮箱授权码等配置。部署过程中我也处理了 MySQL volume 导致 init.sql 不重复执行的问题，后续数据库变更通过迁移 SQL 处理。
```

---

## 7. 项目亮点总结

### 7.1 技术亮点

```text
1. Spring Boot + Vue3 前后端分离架构
2. Spring Security + JWT + RBAC 权限控制
3. DeepSeek API 接入真实 AI 能力
4. WebSocket 实时展示 AI 分析进度
5. Redis 存储验证码和任务状态
6. RAG 岗位知识库问答
7. 全局 AI 任务中心优化长任务体验
8. Docker Compose 一体化部署
9. 管理员后台与权限控制
10. 用户反馈闭环
```

---

### 7.2 产品亮点

```text
1. 面向大学生实习求职，场景明确
2. 从简历、岗位、分析、推荐到面试题形成闭环
3. AI 长任务体验接近真实产品
4. 反馈系统支持后续迭代
5. 单管理员策略避免公开演示账号风险
6. 前端界面经过整体优化
```

---

### 7.3 工程亮点

```text
1. 使用 docs/xx 设计文档驱动开发
2. 使用 feature 分支进行功能隔离
3. 使用 dev 分支集成，main 分支发布
4. Trae + DeepSeek Pro 实现，Codex 复查修复
5. Mock 仅用于测试，线上使用真实 DeepSeek
6. 发布 Release 并打版本标签
7. README、部署文档、验收文档完整
```

---

## 8. 项目不足与后续优化方向

### 8.1 当前不足

```text
1. AI 结果质量仍依赖 Prompt 和模型稳定性
2. 岗位推荐和面试题生成还没有完全后端异步任务化
3. RAG 检索能力仍可以进一步优化
4. 缺少完整 CI/CD 自动部署流水线
5. 缺少更完善的运行监控和告警
6. 缺少 PDF 导出等结果沉淀能力
7. 移动端适配可以继续优化
```

---

### 8.2 后续优化方向

建议优先级：

```text
1. AI Prompt 优化和模型路由
2. AI 分析报告导出 PDF
3. Spring Boot Actuator 运行监控
4. CI/CD 自动部署
5. 统一后端 AI 任务表
6. 用户反馈数据统计
7. 投递进度看板
8. 简历优化版本对比
```

---

## 9. 答辩准备

## 9.1 30 秒项目介绍

```text
InternPilot 是一个面向大学生实习求职的 AI 求职工作台，使用 Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API 构建。系统支持邮箱验证码注册登录、简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、全局 AI 任务中心、用户反馈和管理员后台。项目已经完成 Docker 部署和线上访问。
```

---

## 9.2 1 分钟项目介绍

```text
我的项目叫 InternPilot，定位是面向大学生的 AI 实习投递与简历优化平台。它解决的是学生找实习时不知道简历和岗位是否匹配、如何优化简历、如何准备面试的问题。

系统采用 Spring Boot + Vue3 前后端分离架构，后端使用 MySQL 存储业务数据，Redis 存储验证码和任务状态，使用 Spring Security + JWT + RBAC 做认证和权限控制。AI 能力通过 DeepSeek API 实现，包括简历岗位匹配分析、岗位推荐、面试题生成和 RAG 问答。

项目还实现了 WebSocket AI 分析进度展示和全局 AI 任务中心，用户切换页面后任务状态不会丢失，任务完成后会在右下角通知。系统也有管理员后台和用户反馈模块，已经通过 Docker Compose 部署到服务器。
```

---

## 9.3 3 分钟答辩讲稿结构

```text
1. 项目背景
   大学生实习求职过程中存在简历优化难、岗位匹配难、面试准备难的问题。

2. 项目目标
   构建一个 AI 求职工作台，围绕简历、岗位、分析、推荐、面试准备形成闭环。

3. 技术架构
   后端 Spring Boot，前端 Vue3，数据库 MySQL，缓存 Redis，AI 使用 DeepSeek，部署使用 Docker Compose。

4. 核心功能
   邮箱注册登录、用户中心、简历管理、岗位管理、AI 分析、岗位推荐、面试题生成、RAG 问答、管理员后台、用户反馈。

5. 技术亮点
   RBAC 权限控制、WebSocket AI 进度、全局 AI 任务中心、DeepSeek 接入、RAG、Docker 部署。

6. 项目成果
   系统已完成线上部署和 Release 发布，支持真实 DeepSeek 调用。

7. 后续优化
   继续优化 Prompt、模型路由、PDF 导出、CI/CD 和运行监控。
```

---

## 9.4 项目技术亮点

```text
1. 认证权限完整
   使用 Spring Security + JWT + RBAC，实现登录认证、接口权限、菜单权限和按钮权限控制。

2. AI 能力可替换
   AI 调用抽象为 AiClient，线上 DeepSeek，测试 Mock，避免业务层依赖具体模型。

3. 长任务体验完整
   AI 分析通过 analysis_task 表、异步线程池、WebSocket、Redis 和前端任务中心实现进度追踪。

4. RAG 上下文增强
   管理员维护岗位知识，系统切片、向量化、相似度检索，再拼接进 AI Prompt。

5. 产品闭环完整
   从简历、岗位、分析、推荐、面试题到投递记录，再到用户反馈和管理员处理，形成完整业务链路。

6. 部署可落地
   Docker Compose 管理 frontend、backend、mysql、redis，Nginx 统一处理静态资源和反向代理。
```

---

## 9.5 项目难点与解决方案

```text
1. AI 调用耗时长、用户等待体验差
   解决方案：引入 analysis_task 表、异步线程池和 WebSocket 推送；前端使用全局 AI 任务中心保留任务状态。

2. 页面切换后任务状态丢失
   解决方案：把任务状态从页面组件上移到 Pinia Store，并用 localStorage 做轻量持久化。

3. AI 返回格式不稳定
   解决方案：Prompt 中明确要求 JSON 输出，DeepSeek 请求中对结构化场景设置 response_format=json_object，后端再做解析和异常兜底。

4. 权限容易只做前端隐藏
   解决方案：前端只负责展示控制，后端通过 @PreAuthorize 做真正权限校验。

5. 线上 init.sql 不重复执行
   解决方案：将最终发布阶段变更沉淀为 V40__final_release_update.sql，并在 README 和部署说明中提醒手动执行迁移。

6. README 和项目状态容易不一致
   解决方案：最终阶段对照真实目录、接口、Docker 配置、SQL 权限和构建命令做文档核对。
```

---

## 9.6 项目不足与后续优化

```text
1. 岗位推荐和面试题生成还没有完全统一成后端异步任务模型
   后续可以抽象统一 ai_task 表，让所有 AI 长任务都由后端统一调度。

2. RAG 向量检索仍是 MySQL JSON + 内存相似度计算
   小规模演示足够，后续可接入 pgvector、Milvus、Qdrant 或 Elasticsearch 向量检索。

3. AI 结果质量依赖 Prompt
   后续可以做 Prompt 版本管理、模型路由、结果评分和失败重试。

4. 线上可观测性不足
   后续可以接入 Spring Boot Actuator、接口耗时统计、AI 调用日志和异常告警。

5. 结果沉淀能力还可以增强
   后续可以加入分析报告 PDF 导出、面试题收藏、刷题记录和投递进度看板。
```

---

## 10. 面试高频问题准备

### 10.1 你这个项目是做什么的？

回答：

```text
这是一个面向大学生实习求职的 AI 求职工作台。用户可以上传简历、管理岗位，然后使用 AI 分析简历和岗位的匹配度，生成优化建议，也可以生成岗位推荐和面试题。系统还支持 RAG 岗位知识库问答、用户反馈和管理员后台。
```

---

### 10.2 为什么选择 Spring Boot + Vue3？

回答：

```text
Spring Boot 适合快速构建企业级后端服务，生态完善，和 Spring Security、MyBatis、Redis、WebSocket 集成方便。Vue3 适合构建前后端分离的交互页面，配合 Vite、Pinia、Element Plus 可以快速完成后台系统和用户工作台。这个组合适合中小型全栈项目，也符合企业常见技术栈。
```

---

### 10.3 JWT 登录流程怎么实现？

回答：

```text
用户登录时，后端校验邮箱和密码，校验通过后生成 JWT 返回给前端。前端保存 token，之后每次请求都在 Authorization Header 中携带 token。后端通过 JWT Filter 解析 token，获取用户 ID 和权限信息，并设置到 SecurityContext 中。接口再结合 Spring Security 判断是否允许访问。
```

---

### 10.4 RBAC 是怎么做的？

回答：

```text
项目中有用户表、角色表、权限表，以及用户角色关系表和角色权限关系表。用户登录后，后端查询用户拥有的角色和权限，生成认证信息。后端接口使用 @PreAuthorize 控制权限，前端根据权限动态显示菜单和按钮。这样普通用户和管理员可以访问不同的功能。
```

---

### 10.5 WebSocket 在项目里解决了什么问题？

回答：

```text
AI 分析是耗时任务，如果用户只看到一个 loading，会不知道执行到了哪一步。所以我使用 WebSocket 推送 AI 分析进度。后端在解析简历、构建上下文、调用 AI、生成报告等阶段推送状态，前端实时更新进度条。这样提升了等待体验。
```

---

### 10.6 为什么需要全局 AI 任务中心？

回答：

```text
最初 AI 任务的状态保存在页面组件里，用户切换页面后组件卸载，任务状态就会消失。为了解决这个问题，我把任务状态上移到 Pinia 全局 Store，并用 localStorage 做轻量持久化。同时结合后端任务状态查询和 WebSocket，用户在任意页面都可以看到任务进度，任务完成后右下角会提示查看结果。
```

---

### 10.7 DeepSeek API 是怎么接入的？

回答：

```text
项目中把 AI 调用抽象成 AiClient 接口，生产环境使用 DeepSeekAiClient，测试环境使用 MockAiClient。业务层不直接依赖具体模型实现，这样方便测试，也方便后续扩展其他模型。API Key 通过环境变量注入，避免写死在代码里。
```

---

### 10.8 RAG 是什么？你项目里怎么做的？

回答：

```text
RAG 是检索增强生成，核心思路是在调用大模型之前，先从知识库中检索相关内容，再把检索结果和用户问题一起放入 Prompt。我的项目中管理员可以维护岗位知识库，系统将文档切分成知识片段，用户提问时检索相关片段，再调用 DeepSeek 生成回答。
```

---

### 10.9 Redis 在项目里用来做什么？

回答：

```text
Redis 主要用于邮箱验证码存储、验证码过期控制、AI 任务状态缓存以及部分业务缓存。比如用户注册时，后端生成验证码后存入 Redis，并设置过期时间，注册时再从 Redis 中取出验证码进行校验。
```

---

### 10.10 Docker 部署过程中遇到过什么问题？

回答：

```text
一个典型问题是 MySQL 容器使用 volume 后，init.sql 只会在数据库首次初始化时执行。后续如果新增表或权限，单纯修改 init.sql 不会影响已有线上数据库。解决方案是编写 migration SQL，在部署时手动执行数据库迁移，并在部署文档中明确记录这个注意事项。
```

---

### 10.11 为什么验证码要放 Redis？

回答：

```text
验证码是有时效性的临时数据，不适合长期存入 MySQL。项目中 CaptchaService 把验证码、冷却时间、失败次数、每日发送次数都放到 Redis，并设置 TTL。这样可以自动过期，也方便限制频繁发送和错误次数。
```

---

### 10.12 你怎么防止验证码被频繁刷？

回答：

```text
项目里有三类限制：第一是 cooldown key，短时间内不能重复发送；第二是 daily key，限制每天发送次数；第三是 fail key，限制验证码错误次数。超过限制后会返回明确错误，避免无限尝试。
```

---

### 10.13 AI 返回不是合法 JSON 怎么办？

回答：

```text
我主要从两层处理。第一是在 Prompt 和 DeepSeek 请求里明确要求只返回 JSON，结构化场景还设置 response_format=json_object。第二是在后端解析阶段捕获异常，返回明确的 AI 服务错误，避免前端只看到 unknown error。后续可以继续增强 JSON 修复和重试策略。
```

---

### 10.14 为什么要保留 MockAiClient？

回答：

```text
MockAiClient 主要用于测试和 CI。真实 DeepSeek API 依赖网络和密钥，如果测试直接调用真实接口，会不稳定也会增加成本。通过 AiClient 抽象，测试环境可以切到 Mock，生产环境再用 DeepSeek，这样业务代码不用改。
```

---

### 10.15 analysis_task 表的作用是什么？

回答：

```text
analysis_task 用来记录 AI 分析任务的状态、进度、用户、简历、岗位、报告 ID、错误信息和开始结束时间。它让 AI 分析从同步等待变成异步任务，也支持页面刷新后恢复状态、任务取消、任务中心展示和 WebSocket 推送。
```

---

### 10.16 前端权限和后端权限有什么区别？

回答：

```text
前端权限主要是为了用户体验，比如隐藏没有权限的菜单和按钮；后端权限才是安全边界。InternPilot 前端通过 router meta 和 hasPermission 控制展示，后端通过 @PreAuthorize 校验接口权限，即使用户手动构造请求，没有权限也会被拒绝。
```

---

### 10.17 用户反馈模块为什么有价值？

回答：

```text
用户反馈让项目从“功能堆叠”变成更接近真实产品。用户可以提交问题和建议，系统记录反馈类型、标题、内容、当前页面和浏览器信息；管理员在后台处理和回复。这有利于后续迭代，也能在答辩中体现产品闭环。
```

---

### 10.18 RAG 为什么没有直接用向量数据库？

回答：

```text
这个项目是课程项目和小规模演示场景，知识量不大，所以当前用 MySQL 存储 chunk 和 embedding JSON，再在内存中计算余弦相似度，复杂度和部署成本更低。后续如果知识库规模变大，可以替换成 pgvector、Milvus 或 Qdrant。
```

---

### 10.19 你怎么保证线上不使用 Mock AI？

回答：

```text
部署配置里 SPRING_PROFILES_ACTIVE 使用 prod，AI_PROVIDER 使用 deepseek。项目还保留 OnlineProfileGuard 这类线上配置保护思路，README 和 .env.example 都明确说明 Mock 只用于 test / CI。最终发布检查也会确认线上使用 DeepSeek 真接口。
```

---

### 10.20 如果让你继续优化，你最先做什么？

回答：

```text
我会优先做三个方向：第一，把岗位推荐和面试题也统一成后端 AI 任务模型；第二，优化 Prompt 版本管理、模型路由和失败重试；第三，增强可观测性，比如 Actuator、接口耗时统计、AI 调用日志和异常告警。这样能让系统从可演示进一步接近可维护。
```

---

## 11. 简历写法建议

### 11.1 简历项目描述

```text
InternPilot 智能实习领航员：基于 Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API 的 AI 实习投递与简历优化平台，面向大学生实习求职场景，提供简历管理、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库问答、用户反馈和管理员后台等功能。
```

---

### 11.2 简历项目亮点

```text
- 使用 Spring Security + JWT + RBAC 实现登录认证、角色权限、菜单权限和接口权限控制
- 接入 DeepSeek API，实现简历岗位匹配分析、岗位推荐、AI 面试题生成和 RAG 问答
- 使用 WebSocket 实现 AI 分析任务实时进度推送，并设计全局 AI 任务中心解决页面切换后任务状态丢失问题
- 使用 Redis 实现邮箱验证码存储、过期控制和 AI 任务状态缓存
- 使用 Docker Compose 部署 Spring Boot、Vue/Nginx、MySQL、Redis，实现服务器在线访问
- 实现管理员后台、操作日志、用户反馈模块，形成完整系统管理与产品迭代闭环
```

---

## 12. 当前阶段学习重点

项目完成后，建议按以下顺序学习和复盘：

```text
1. 完整跑通项目所有核心流程
2. 画出系统架构图
3. 画出登录认证流程图
4. 画出 AI 分析任务流程图
5. 画出 RBAC 权限模型
6. 画出 Docker 部署结构
7. 整理 10 个面试问题
8. 整理 3 分钟答辩讲稿
9. 准备 README 和截图
10. 规划下一轮技术优化
```

---

## 13. 后续文档建议

完成本文档后，建议下一阶段文档为：

```text
docs/42-ai-model-router-and-prompt-optimization-design.md
```

目标：

```text
1. Prompt 版本管理
2. AI 场景枚举增强
3. 模型路由策略
4. DeepSeek Flash / Pro 场景区分
5. AI 结果质量评分
6. 失败重试与降级策略
7. Prompt 调试日志
8. 多模型 Provider 扩展预留
```

再下一阶段可以做：

```text
docs/43-spring-boot-engineering-enhancement-design.md
```

目标：

```text
1. Spring Boot Actuator
2. 接口耗时统计
3. AOP 操作日志增强
4. 统一参数校验
5. 缓存注解优化
6. 定时任务清理
7. 健康检查接口
8. 生产环境可观测性
```

---

## 14. 本次项目全景核对报告

### 14.1 检查范围

本次按当前代码和 README 核对了以下内容：

```text
1. 后端包结构：backend/intern-pilot-backend/src/main/java/com/internpilot
2. 前端目录结构：frontend/intern-pilot-frontend/src
3. 核心 Controller、Service、Store、工具类和部署配置
4. README 当前功能清单、部署说明、安全说明和目录结构
5. 数据库初始化与最终发布迁移脚本
6. Docker Compose 和 Nginx 代理配置
```

---

### 14.2 后端实际结构核对

当前后端实际包结构包括：

```text
ai
annotation
aspect
captcha
common
config
controller
dto
entity
enums
exception
mapper
runner
security
service
util
vo
```

结论：

```text
1. 文档中原有 controller / service / mapper / entity / dto / vo / config / security / exception / common 描述是正确的。
2. 需要补充 ai、captcha、annotation、aspect、enums、runner，这些已经在本文档 5.3.3 中补齐。
3. AI 能力集中在 ai 包下，结构比普通 Service 调用更清晰。
4. 操作日志由 annotation + aspect 实现，属于答辩时可以讲的工程亮点。
```

---

### 14.3 前端实际结构核对

当前前端实际目录包括：

```text
api
assets
components
router
stores
styles
types
utils
views
```

结论：

```text
1. 文档中 views、components、api、router、stores、utils、assets 描述正确。
2. 需要补充 styles 和 types。
3. components 下已经拆分 ai、common、feedback、layout，说明 UI 已经从页面内堆叠逐步走向组件化。
4. stores 下 auth、aiTaskCenter、feedback 分别承载登录态、AI 任务中心和反馈状态。
```

---

### 14.4 核心功能覆盖情况

| 模块 | 文档覆盖 | README 覆盖 | 代码存在 |
| --- | --- | --- | --- |
| 邮箱验证码注册登录 | 已覆盖 | 已覆盖 | `AuthController`、`AuthServiceImpl`、`CaptchaService` |
| JWT 鉴权 | 已覆盖 | 已覆盖 | `JwtAuthenticationFilter`、`JwtTokenProvider` |
| RBAC 权限 | 已覆盖 | 已覆盖 | `PermissionMapper`、`@PreAuthorize`、前端 `hasPermission` |
| 简历管理 | 已覆盖 | 已覆盖 | `ResumeController`、`ResumeService` |
| 简历版本 | 已覆盖 | 已覆盖 | `ResumeVersionController`、`ResumeVersionService` |
| 岗位管理 | 已覆盖 | 已覆盖 | `JobController`、`JobService` |
| AI 分析 | 已覆盖 | 已覆盖 | `AnalysisController`、`AnalysisTaskController` |
| WebSocket 进度 | 已覆盖 | 已覆盖 | `WebSocketConfig`、`AnalysisProgressPublisherImpl` |
| DeepSeek API | 已覆盖 | 已覆盖 | `DeepSeekAiClient` |
| AI 任务中心 | 已覆盖 | 已覆盖 | `aiTaskCenter.ts`、`AiTaskFloat.vue`、`AiTaskDrawer.vue` |
| 岗位推荐 | 已覆盖 | 已覆盖 | `JobRecommendationController`、`JobRecommendationService` |
| AI 面试题 | 已覆盖 | 已覆盖 | `InterviewQuestionController`、`InterviewQuestionService` |
| RAG 知识库 | 已覆盖 | 已覆盖 | `AdminRagKnowledgeController`、`RagKnowledgeServiceImpl` |
| 用户反馈 | 已覆盖 | 已覆盖 | `FeedbackController`、`AdminFeedbackController`、`FeedbackService` |
| 管理员后台 | 已覆盖 | 已覆盖 | `AdminLayout.vue`、`views/admin/*` |
| Docker 部署 | 已覆盖 | 已覆盖 | `deploy/docker-compose.yml`、`frontend/nginx.conf` |

---

### 14.5 发现的问题和建议

```text
1. docs/41 早期写法中部分数据库表名偏概念化，本次已改成实际表名。
2. README 当前结构与功能基本一致，可以继续保留原结构，只在功能迭代后增量更新。
3. 岗位推荐和面试题生成虽然接入了前端 AI 任务中心，但后端还不是统一异步任务模型，后续可以作为重点优化。
4. RAG 当前适合小规模演示，如果后续扩大知识库，建议接入专业向量检索。
5. 最终提交前建议重新截图，确保 README 展示图与当前新版 UI、logo、favicon 一致。
6. 如果已经线上部署，需要手动确认服务器 .env 未提交、DeepSeek 使用真接口、MySQL 是否已执行 V40 迁移。
```

---

### 14.6 手动确认清单

```text
1. 线上服务器 .env 是否只保存在服务器，不在 Git 仓库中
2. 线上 AI_PROVIDER 是否为 deepseek
3. 线上 DEEPSEEK_API_KEY 是否可用
4. 线上 SMTP 授权码是否可用且未泄露
5. MySQL 已有 volume 是否已手动执行 V40__final_release_update.sql
6. README 截图是否与当前 UI 一致
7. main 合并、tag、GitHub Release、Gitee 同步是否已经完成
8. 答辩演示账号和管理员密码是否只通过私下安全渠道保存
```

---
