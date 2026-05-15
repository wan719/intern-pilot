# InternPilot WebSocket AI 分析进度增强与测试设计文档

## 一、文档目的

本文档用于设计 InternPilot 项目中 WebSocket AI 分析进度模块的后续优化方案。

当前项目已经实现了 AI 简历匹配分析、WebSocket 进度推送、前端进度展示等基础能力。后续优化目标是让 AI 分析过程具备更清晰的任务状态、更可靠的进度推送、更好的异常处理、更合理的用户隔离，并补充单元测试和集成测试，提升项目的稳定性和面试展示价值。

---

## 二、为什么当前阶段优先优化 WebSocket

### 2.1 RBAC 当前状态

RBAC 权限系统已经完成了核心闭环：

1. 后端 `/api/admin/**` 已改为登录即可进入安全链路
2. 具体接口使用 `@PreAuthorize` 控制权限
3. 前端刷新后可以通过 `/api/user/me` 恢复用户角色和权限
4. 前端路由和按钮已经接入权限判断
5. 已补充 `/api/admin/ping` 的 401 / 403 / 200 权限测试
6. 后端测试通过
7. 前端构建通过

因此 RBAC 当前已经达到阶段性可合并状态。

### 2.2 继续扩展 RBAC 的问题

继续扩展 RBAC 会进入以下高级能力：

1. 动态菜单
2. 权限缓存
3. Token 权限实时失效
4. 数据权限
5. 部门权限
6. 超级管理员机制

这些功能虽然有价值，但实现成本较高，而且当前项目的核心亮点是 AI 实习投递平台，不应该让权限系统继续占用过多开发周期。

### 2.3 优先优化 WebSocket 的价值

WebSocket AI 分析进度优化更适合当前阶段，原因如下：

1. 能明显提升用户体验
2. 能体现项目的实时交互能力
3. 能和 AI 分析、RAG、Redis、任务状态结合
4. 更容易做出前后端联动演示效果
5. 更适合面试展示技术深度
6. 可以补充单元测试和集成测试，提升工程完整度

---

## 三、当前 WebSocket 模块现状

### 3.1 已有能力

当前项目已经具备：

1. 后端 WebSocket 配置
2. AI 分析任务进度推送
3. 前端连接 WebSocket
4. 前端展示 AI 分析进度
5. AI 分析完成后展示结果

### 3.2 当前可能存在的问题

| 问题 | 说明 |
|---|---|
| 任务状态不够标准 | 需要统一任务状态枚举 |
| 进度消息结构不够规范 | 需要统一 WebSocket 消息 DTO |
| 异常状态不够清晰 | AI 分析失败时前端体验不够稳定 |
| 用户隔离不足 | 如果使用公开 topic，理论上知道 taskNo 就可以订阅 |
| 刷新页面后进度恢复不足 | 页面刷新后可能丢失任务状态 |
| 缺少任务查询接口 | 前端无法主动查询任务当前状态 |
| 测试覆盖不足 | 需要补充单元测试和集成测试 |

---

## 四、优化目标

### 4.1 总体目标

本阶段目标是：

> 将 WebSocket AI 分析进度模块从“能推送进度”升级为“任务状态清晰、消息结构规范、异常可追踪、用户隔离更合理、测试覆盖完整”的实时任务模块。

### 4.2 具体目标

1. 统一 AI 分析任务状态
2. 统一 WebSocket 进度消息格式
3. 增加任务状态查询接口
4. 增强 AI 分析异常进度推送
5. 前端支持任务进度恢复
6. 前端展示更清晰的进度阶段
7. 增加 WebSocket 服务层单元测试
8. 增加 AI 分析进度集成测试
9. 增加前端进度展示测试思路
10. 保持本地开发模式，不依赖 Docker

---

## 五、任务状态设计

### 5.1 任务状态枚举

建议新增或完善任务状态：

```java
public enum AiAnalysisTaskStatus {
    PENDING,
    PARSING_RESUME,
    BUILDING_CONTEXT,
    CALLING_AI,
    GENERATING_REPORT,
    COMPLETED,
    FAILED
}
````

### 5.2 状态说明

|状态|说明|
|---|---|
|PENDING|任务已创建，等待执行|
|PARSING_RESUME|正在解析简历内容|
|BUILDING_CONTEXT|正在构建 JD / RAG 上下文|
|CALLING_AI|正在调用 AI 模型|
|GENERATING_REPORT|正在生成分析报告|
|COMPLETED|分析完成|
|FAILED|分析失败|

### 5.3 进度百分比建议

|状态|建议进度|
|---|---|
|PENDING|0|
|PARSING_RESUME|15|
|BUILDING_CONTEXT|35|
|CALLING_AI|60|
|GENERATING_REPORT|85|
|COMPLETED|100|
|FAILED|当前进度或 100|

---

## 六、WebSocket 消息设计

### 6.1 消息 DTO

建议统一使用：

```java
public class AiAnalysisProgressMessage {

    private String taskNo;

    private Long userId;

    private String status;

    private Integer progress;

    private String message;

    private Long reportId;

    private String errorMessage;

    private LocalDateTime timestamp;
}
```

### 6.2 字段说明

|字段|说明|
|---|---|
|taskNo|AI 分析任务编号|
|userId|用户 ID|
|status|当前任务状态|
|progress|当前进度百分比|
|message|展示给前端的进度文案|
|reportId|分析完成后生成的报告 ID|
|errorMessage|失败原因|
|timestamp|消息时间|

### 6.3 成功消息示例

```json
{
  "taskNo": "ANALYSIS-20260514-001",
  "userId": 1,
  "status": "CALLING_AI",
  "progress": 60,
  "message": "正在调用 AI 模型生成简历匹配分析",
  "reportId": null,
  "errorMessage": null,
  "timestamp": "2026-05-14T10:30:00"
}
```

### 6.4 完成消息示例

```json
{
  "taskNo": "ANALYSIS-20260514-001",
  "userId": 1,
  "status": "COMPLETED",
  "progress": 100,
  "message": "AI 分析完成",
  "reportId": 12,
  "errorMessage": null,
  "timestamp": "2026-05-14T10:30:10"
}
```

### 6.5 失败消息示例

```json
{
  "taskNo": "ANALYSIS-20260514-001",
  "userId": 1,
  "status": "FAILED",
  "progress": 60,
  "message": "AI 分析失败",
  "reportId": null,
  "errorMessage": "AI 服务调用超时，请稍后重试",
  "timestamp": "2026-05-14T10:30:12"
}
```

---

## 七、WebSocket 推送路径设计

### 7.1 当前方案

当前可能使用类似：

```text
/topic/analysis/{taskNo}
```

优点：

1. 实现简单
    
2. 前端订阅方便
    
3. 适合 MVP
    

问题：

1. 属于公开 topic
    
2. 只要知道 taskNo 就可能订阅
    
3. 用户隔离不够严谨
    

### 7.2 第二阶段推荐方案

推荐升级为用户级消息：

```text
/user/queue/analysis-progress
```

或者：

```text
/user/queue/analysis-progress/{taskNo}
```

后端推送：

```java
messagingTemplate.convertAndSendToUser(
    userId.toString(),
    "/queue/analysis-progress",
    message
);
```

前端订阅：

```ts
client.subscribe('/user/queue/analysis-progress', (message) => {
  const progress = JSON.parse(message.body)
})
```

### 7.3 当前阶段实施建议

为了避免大改，可以分两步：

#### 阶段一：最小增强

保留现有 topic：

```text
/topic/analysis/{taskNo}
```

但增加：

1. taskNo 随机性
    
2. 后端查询任务状态时校验 userId
    
3. 前端只能订阅自己创建任务返回的 taskNo
    
4. 任务完成后取消订阅
    

#### 阶段二：安全增强

升级为：

```text
/user/queue/analysis-progress
```

当前建议优先完成阶段一，确保体验和测试闭环。

---

## 八、后端接口设计

### 8.1 创建 AI 分析任务

接口：

```text
POST /api/analysis/match/async
```

请求参数：

```json
{
  "resumeId": 1,
  "jobId": 2,
  "resumeVersionId": 3
}
```

返回结果：

```json
{
  "taskNo": "ANALYSIS-20260514-001",
  "status": "PENDING",
  "message": "AI 分析任务已创建"
}
```

### 8.2 查询任务状态

新增接口：

```text
GET /api/analysis/tasks/{taskNo}
```

权限要求：

```java
@PreAuthorize("hasAuthority('analysis:read')")
```

返回结果：

```json
{
  "taskNo": "ANALYSIS-20260514-001",
  "status": "CALLING_AI",
  "progress": 60,
  "message": "正在调用 AI 模型",
  "reportId": null,
  "errorMessage": null
}
```

### 8.3 查询最近任务

可选接口：

```text
GET /api/analysis/tasks/recent
```

用途：

1. 页面刷新后恢复最近未完成任务
    
2. 用户重新进入分析页面时恢复进度
    
3. 避免 WebSocket 消息丢失导致页面卡住
    

当前阶段可以先实现 `GET /api/analysis/tasks/{taskNo}`。

---

## 九、后端服务设计

### 9.1 核心服务

建议抽象：

```java
public interface AiAnalysisProgressService {

    void publishProgress(Long userId, String taskNo, AiAnalysisTaskStatus status, Integer progress, String message);

    void publishCompleted(Long userId, String taskNo, Long reportId);

    void publishFailed(Long userId, String taskNo, String errorMessage);
}
```

### 9.2 实现类职责

```java
@Service
public class AiAnalysisProgressServiceImpl implements AiAnalysisProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishProgress(...) {
        // 构造 AiAnalysisProgressMessage
        // 推送 WebSocket
        // 可选：保存最新任务状态到 Redis 或数据库
    }
}
```

### 9.3 是否保存任务状态

建议保存。

优先级：

|方案|说明|当前建议|
|---|---|---|
|只推 WebSocket|简单，但刷新后丢状态|不推荐|
|Redis 保存任务状态|适合临时任务状态|推荐|
|MySQL 保存任务状态|适合长期追踪和审计|后续可做|

当前建议：

```text
WebSocket 实时推送 + Redis 保存最新状态
```

### 9.4 Redis Key 设计

```text
ai:analysis:task:{taskNo}
```

示例：

```text
ai:analysis:task:ANALYSIS-20260514-001
```

Value：

```json
{
  "taskNo": "ANALYSIS-20260514-001",
  "userId": 1,
  "status": "CALLING_AI",
  "progress": 60,
  "message": "正在调用 AI 模型",
  "reportId": null,
  "errorMessage": null
}
```

过期时间：

```text
24 小时
```

---

## 十、AI 分析流程增强设计

### 10.1 原流程

```text
用户点击分析
  ↓
后端执行 AI 分析
  ↓
分析完成后返回结果
```

### 10.2 增强后流程

```text
用户点击分析
  ↓
创建 taskNo
  ↓
返回 taskNo 给前端
  ↓
前端订阅 WebSocket
  ↓
后端异步执行分析
  ↓
推送 PENDING
  ↓
推送 PARSING_RESUME
  ↓
推送 BUILDING_CONTEXT
  ↓
推送 CALLING_AI
  ↓
推送 GENERATING_REPORT
  ↓
推送 COMPLETED 或 FAILED
  ↓
前端根据 reportId 跳转分析详情
```

### 10.3 异常处理流程

```text
AI 分析任意阶段异常
  ↓
捕获异常
  ↓
记录后端日志
  ↓
保存任务状态 FAILED
  ↓
推送 FAILED 消息
  ↓
前端展示失败原因
  ↓
允许用户重新发起分析
```

---

## 十一、前端设计

### 11.1 前端 WebSocket 工具

建议保留或完善：

```text
src/api/analysisSocket.ts
```

职责：

1. 创建 STOMP Client
    
2. 连接 WebSocket
    
3. 订阅任务进度
    
4. 取消订阅
    
5. 断线重连
    
6. 销毁连接
    

### 11.2 页面状态

分析页面需要维护：

```ts
const taskNo = ref('')
const progress = ref(0)
const status = ref('')
const message = ref('')
const errorMessage = ref('')
const reportId = ref<number | null>(null)
const analyzing = ref(false)
```

### 11.3 页面展示

不同状态展示不同文案：

|状态|前端文案|
|---|---|
|PENDING|AI 分析任务已创建|
|PARSING_RESUME|正在解析简历内容|
|BUILDING_CONTEXT|正在构建岗位与知识库上下文|
|CALLING_AI|正在调用 AI 模型|
|GENERATING_REPORT|正在生成分析报告|
|COMPLETED|分析完成|
|FAILED|分析失败，请稍后重试|

### 11.4 页面刷新恢复

用户刷新页面后，如果当前有 taskNo，可以调用：

```text
GET /api/analysis/tasks/{taskNo}
```

根据返回状态恢复页面：

1. 如果任务还在进行中，重新连接 WebSocket
    
2. 如果任务已完成，展示完成状态并提供查看报告入口
    
3. 如果任务失败，展示错误信息
    

### 11.5 取消订阅

任务完成或失败后，需要取消订阅：

```ts
subscription.unsubscribe()
```

避免页面重复接收消息或内存泄漏。

---

## 十二、本地开发方式

本阶段继续使用本地开发模式。

|模块|方式|
|---|---|
|后端|IDEA 或 `.\gradlew.bat bootRun`|
|前端|`npm run dev`|
|MySQL|本机 MySQL|
|Redis|本机 Redis|
|Docker|不参与本阶段开发|

启动后端：

```powershell
cd backend/intern-pilot-backend
.\gradlew.bat bootRun
```

启动前端：

```powershell
cd frontend/intern-pilot-frontend
npm run dev
```

---

## 十三、单元测试设计

### 13.1 AiAnalysisProgressServiceTest

测试目标：

1. 进度消息构造正确
    
2. 推送进度时调用 `SimpMessagingTemplate`
    
3. 完成消息包含 reportId
    
4. 失败消息包含 errorMessage
    
5. Redis 中保存任务状态
    

测试用例：

|用例|输入|预期|
|---|---|---|
|publishProgress_success|userId、taskNo、status、progress|WebSocket 推送一次|
|publishCompleted_success|reportId|状态为 COMPLETED，progress 为 100|
|publishFailed_success|errorMessage|状态为 FAILED，包含错误原因|
|saveTaskStatus_success|progress message|Redis 保存成功|
|getTaskStatus_success|taskNo|能读取任务状态|

### 13.2 AiAnalysisTaskStatusTest

测试目标：

1. 枚举值完整
    
2. 状态到进度映射正确
    
3. 状态到文案映射正确
    

测试用例：

|状态|预期进度|
|---|---|
|PENDING|0|
|PARSING_RESUME|15|
|BUILDING_CONTEXT|35|
|CALLING_AI|60|
|GENERATING_REPORT|85|
|COMPLETED|100|

### 13.3 AnalysisServiceImplTest

测试目标：

1. 分析流程中按顺序推送进度
    
2. AI 调用成功时推送 COMPLETED
    
3. AI 调用失败时推送 FAILED
    
4. RAG 构建失败时不影响整体异常处理
    
5. 生成报告后 reportId 正确返回
    

测试用例：

|场景|预期|
|---|---|
|正常分析|推送多个进度，最后 COMPLETED|
|AI 调用异常|推送 FAILED|
|简历不存在|推送 FAILED|
|岗位不存在|推送 FAILED|
|Redis 异常|不影响主流程，但记录日志|

---

## 十四、集成测试设计

### 14.1 AnalysisProgressIntegrationTest

测试目标：

验证从 HTTP 发起分析任务到任务状态更新的完整流程。

测试流程：

```text
1. 使用测试用户登录
2. 创建测试简历
3. 创建测试岗位
4. 调用异步分析接口
5. 获取 taskNo
6. 查询任务状态接口
7. 验证状态存在
8. 等待任务完成
9. 查询最终状态
10. 验证状态为 COMPLETED 或 FAILED
```

### 14.2 接口测试用例

|用例|预期|
|---|---|
|未登录创建分析任务|401|
|普通用户创建自己的分析任务|200|
|查询自己的 taskNo|200|
|查询别人的 taskNo|403 或 404|
|查询不存在的 taskNo|404|
|AI 成功时最终状态 COMPLETED|通过|
|AI 失败时最终状态 FAILED|通过|

### 14.3 WebSocket 推送集成测试

如果测试复杂，可以分两级：

#### 方案一：轻量集成测试

不真的建立 WebSocket 连接，只验证：

1. `SimpMessagingTemplate` 被调用
    
2. 消息 DTO 正确
    
3. Redis 状态正确
    
4. HTTP 查询能查到状态
    

这是当前阶段推荐方案。

#### 方案二：完整 WebSocket 集成测试

使用 STOMP 客户端连接测试端点，订阅 topic 或 queue，然后触发分析任务，等待接收消息。

当前项目阶段不强制做完整 WebSocket 客户端测试，避免测试复杂度过高。

---

## 十五、前端测试设计

当前如果项目没有前端测试框架，可以先写测试方案，不强制实现。

### 15.1 推荐测试点

|测试项|预期|
|---|---|
|点击开始分析后显示进度条|成功|
|收到 CALLING_AI 消息后进度变为 60|成功|
|收到 COMPLETED 后进度变为 100|成功|
|收到 FAILED 后展示错误信息|成功|
|页面离开后取消订阅|成功|
|刷新页面后调用任务状态查询接口|成功|

### 15.2 后续可引入

后续可以考虑：

1. Vitest
    
2. Vue Test Utils
    
3. Playwright
    

当前阶段重点仍然后端测试和手动前端验证。

---

## 十六、验收标准

本阶段完成后，需要满足：

1. AI 分析任务有清晰 taskNo
    
2. AI 分析任务有统一状态枚举
    
3. WebSocket 进度消息格式统一
    
4. AI 分析成功时推送 COMPLETED
    
5. AI 分析失败时推送 FAILED
    
6. 前端可以展示实时进度
    
7. 前端可以展示失败原因
    
8. 前端任务完成后可以跳转报告详情
    
9. 页面刷新后可以查询任务状态
    
10. 用户不能查询别人的任务状态
    
11. 单元测试覆盖进度服务
    
12. 集成测试覆盖任务创建和状态查询
    
13. 后端 `.\gradlew.bat test --no-daemon` 通过
    
14. 前端 `npm run build` 通过
    

---

## 十七、开发任务拆分

### 17.1 后端任务

|编号|任务|
|---|---|
|WS-BE-01|新增或完善 AiAnalysisTaskStatus 枚举|
|WS-BE-02|新增 AiAnalysisProgressMessage DTO|
|WS-BE-03|新增 AiAnalysisProgressService|
|WS-BE-04|推送进度时同步保存 Redis 状态|
|WS-BE-05|新增任务状态查询接口|
|WS-BE-06|AI 分析流程中补充阶段性进度|
|WS-BE-07|AI 异常时推送 FAILED|
|WS-BE-08|查询任务状态时校验 userId|
|WS-BE-09|增加单元测试|
|WS-BE-10|增加集成测试|

### 17.2 前端任务

|编号|任务|
|---|---|
|WS-FE-01|完善 analysisSocket.ts|
|WS-FE-02|分析页面接入统一 ProgressMessage|
|WS-FE-03|展示任务状态和进度条|
|WS-FE-04|失败时展示错误信息|
|WS-FE-05|完成后提供查看报告入口|
|WS-FE-06|页面离开时取消订阅|
|WS-FE-07|页面刷新后查询任务状态|
|WS-FE-08|构建通过|

### 17.3 测试任务

|编号|任务|
|---|---|
|WS-TEST-01|AiAnalysisProgressServiceTest|
|WS-TEST-02|AiAnalysisTaskStatusTest|
|WS-TEST-03|AnalysisServiceImplTest 补进度推送测试|
|WS-TEST-04|AnalysisProgressIntegrationTest|
|WS-TEST-05|任务状态查询权限测试|
|WS-TEST-06|前端手动测试清单|

---

## 十八、分支设计

本阶段新建分支：

```text
feature/websocket-ai-progress
```

分支来源：

```text
dev
```

开发完成后合并路径：

```text
feature/websocket-ai-progress
  ↓
dev
  ↓
main
```

创建分支：

```bash
git checkout dev
git pull origin dev
git checkout -b feature/websocket-ai-progress
```

提交建议：

```bash
git add .
git commit -m "完善 WebSocket AI 分析进度与测试设计"
```

实现完成后提交：

```bash
git add .
git commit -m "增强 WebSocket AI 分析进度与任务状态测试"
```

---

## 十九、开发优先级

### P0：必须完成

1. 统一进度消息 DTO
    
2. 统一任务状态枚举
    
3. AI 成功推送 COMPLETED
    
4. AI 失败推送 FAILED
    
5. 新增任务状态查询接口
    
6. 查询任务状态校验 userId
    
7. 后端单元测试通过
    
8. 后端集成测试通过
    
9. 前端构建通过
    

### P1：重要增强

1. Redis 保存任务状态
    
2. 页面刷新后恢复进度
    
3. 前端取消订阅
    
4. 前端失败状态展示
    
5. 任务完成后跳转报告详情
    

### P2：后续优化

1. `/user/queue` 用户级 WebSocket 推送
    
2. WebSocket CONNECT 鉴权
    
3. 完整 STOMP 集成测试
    
4. 任务历史记录入库
    
5. 支持取消 AI 分析任务
    
6. 支持多个 AI 任务并发展示
    

---

## 二十、面试表达方式

本阶段可以在面试中这样介绍：

> 我在 InternPilot 项目中实现了 AI 分析任务的 WebSocket 实时进度推送。用户发起简历和岗位匹配分析后，后端会生成 taskNo，并在分析过程中按阶段推送任务状态，例如解析简历、构建上下文、调用 AI、生成报告等。前端通过 WebSocket 实时展示进度条和状态文案。为了提升可靠性，我还设计了任务状态查询接口，并使用 Redis 保存最新任务状态，解决页面刷新后进度丢失的问题。同时，我为进度服务、任务状态查询和权限隔离补充了单元测试和集成测试。

可强调亮点：

1. WebSocket 实时通信
    
2. AI 异步任务进度管理
    
3. Redis 保存任务状态
    
4. 前端进度恢复
    
5. 失败状态推送
    
6. 用户任务隔离
    
7. 单元测试和集成测试覆盖
    

---

## 二十一、总结

本阶段不继续深入扩展 RBAC，而是优先增强 WebSocket AI 分析进度模块。

原因是 RBAC 当前已经完成核心闭环，继续扩展会进入复杂企业权限体系；而 WebSocket 优化可以更直接提升 InternPilot 的项目体验和展示效果。

本阶段完成后，InternPilot 将具备：

1. 更好的 AI 分析实时反馈
    
2. 更完整的异步任务状态管理
    
3. 更稳定的异常处理能力
    
4. 更清晰的前后端实时通信链路
    
5. 更完善的测试覆盖
    
6. 更适合实习面试展示的项目亮点
    

````

---

你接下来可以这样走：

```bash
git checkout dev
git pull origin dev
git checkout -b feature/websocket-ai-progress
````

然后先提交文档：

```bash
git add docs/31-websocket-ai-progress-enhancement-test-design.md
git commit -m "新增 WebSocket AI 分析进度增强与测试设计文档"
```

后续再让 Codex 按这个文档做最小实现。