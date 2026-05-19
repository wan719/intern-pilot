
# docs/39-ai-task-center-and-feedback-design.md

# InternPilot 全局 AI 任务中心与用户反馈功能设计文档

## 1. 文档背景

当前 InternPilot 已完成：

- RBAC 权限系统
    
- WebSocket AI 分析进度
    
- DeepSeek API 接入
    
- AI 面试题生成
    
- RAG 岗位知识库
    
- 岗位推荐
    
- 管理员后台
    
- 邮箱验证码注册登录
    
- 单管理员初始化
    
- 用户中心
    
- 前端界面优化
    

当前存在一个明显的用户体验问题：

> 用户点击“AI 分析 / 岗位推荐 / 面试题生成”等耗时 AI 功能后，如果切换页面，页面内的 loading、进度条、生成状态容易消失。用户不知道任务是否还在执行，也不知道什么时候完成。

此外，系统目前缺少用户反馈入口。用户发现页面问题、AI 结果不准确、功能体验差时，无法直接提交反馈。

因此，本阶段新增两个产品级体验功能：

1. **全局 AI 任务中心**
    
2. **用户反馈功能**
    

---

## 2. 本阶段目标

### 2.1 全局 AI 任务中心目标

实现后，用户体验应变为：

```text
用户点击 AI 分析 / 岗位推荐 / 面试题生成
        ↓
任务进入全局任务中心
        ↓
用户切换页面后任务仍然显示“进行中”
        ↓
任务完成后右下角弹出通知
        ↓
用户可以点击“查看结果”
        ↓
用户也可以选择“不再提示”或“停止任务”
```

核心目标：

- AI 长任务状态不再只绑定在单个页面组件里
    
- 页面切换后任务状态不丢失
    
- 右下角显示全局任务提示
    
- 任务完成后主动通知用户
    
- AI 分析任务支持后端真实查询与取消
    
- 岗位推荐、面试题生成先采用前端全局任务追踪
    
- 为后续统一后端 AI 任务表预留扩展空间
    

---

### 2.2 用户反馈功能目标

实现后，用户可以在任意页面提交反馈：

```text
右下角反馈按钮
        ↓
打开反馈抽屉 / 弹窗
        ↓
选择反馈类型
        ↓
填写反馈标题和内容
        ↓
提交到后端
        ↓
管理员后台查看和处理
```

核心目标：

- 用户可以提交系统问题和建议
    
- 反馈自动记录当前页面路径
    
- 后端保存反馈记录
    
- 管理员可以查看、处理、回复反馈
    
- 后续可作为产品迭代依据
    

---

## 3. 本阶段不做什么

为了控制开发范围，本阶段暂时不做以下内容：

```text
1. 不把所有 AI 功能一次性改造成完整后端异步任务系统
2. 不实现真正的 DeepSeek 请求物理中断
3. 不做复杂任务队列，例如 RabbitMQ、Kafka、Quartz
4. 不做截图上传反馈
5. 不做反馈消息站内信通知
6. 不做多端同步任务中心
7. 不重构现有所有 AI 业务表
```

说明：

> 已经发出的 DeepSeek HTTP 请求通常无法保证被物理中断。系统中的“停止任务”第一阶段主要是将本地任务状态标记为取消，后续结果不再主动展示给用户。

---

## 4. 当前问题分析

### 4.1 AI 分析页面当前问题

当前项目中已有：

```text
backend:
- AnalysisTask
- AnalysisTaskController
- AnalysisTaskServiceImpl
- AnalysisProgressPublisher
- WebSocketConfig

frontend:
- src/api/analysisTask.ts
- src/utils/analysisSocket.ts
- src/views/analysis/AnalysisMatch.vue
```

但是目前 AI 分析进度主要保存在 `AnalysisMatch.vue` 页面组件内部。

典型问题：

```text
1. 用户进入 AI 分析页面
2. 点击开始分析
3. 页面中显示进度
4. 用户切换到其他页面
5. AnalysisMatch.vue 卸载
6. WebSocket 连接清理
7. 切回来后虽然可通过 localStorage 恢复部分任务，但没有全局通知和任务中心
```

因此需要把任务状态上移到全局。

---

### 4.2 岗位推荐和面试题生成当前问题

当前岗位推荐和面试题生成大概率还是同步请求模式：

```text
POST /api/job-recommendations/generate
POST /api/interview-questions/generate
```

前端用局部变量控制：

```text
generating = true
loading = true
```

问题：

```text
1. 状态只在当前页面有效
2. 切换页面后用户不知道任务是否完成
3. 任务完成后无法在其他页面弹出查看入口
4. 不适合作为“产品级 AI 长任务体验”
```

本阶段不强行重构它们的后端，而是在前端增加全局任务追踪。

---

## 5. 总体方案

本阶段采用：

```text
方案二 + 方案三的折中版
```

即：

```text
已有后端异步任务的 AI 分析：
    复用 analysis_task + WebSocket + 轮询兜底
    增加“我的运行中任务”“取消任务”等接口

暂未异步化的岗位推荐 / 面试题生成：
    前端用全局任务中心接管 loading、完成通知、查看结果跳转
    后端接口暂时保持不变

用户反馈：
    新增完整后端表、接口、前端反馈抽屉、管理员处理页面
```

---

## 6. 功能一：全局 AI 任务中心

### 6.1 任务类型设计

前端统一定义任务类型：

```ts
export type AiTaskType =
  | 'ANALYSIS_MATCH'
  | 'JOB_RECOMMENDATION'
  | 'INTERVIEW_QUESTION'
  | 'INTERVIEW_REGENERATE'
  | 'RAG_IMPORT'
  | 'RESUME_OPTIMIZE'
```

本阶段优先支持：

```text
ANALYSIS_MATCH        AI 简历岗位匹配分析
JOB_RECOMMENDATION   岗位推荐生成
INTERVIEW_QUESTION   AI 面试题生成
INTERVIEW_REGENERATE AI 面试题重新生成
```

预留：

```text
RAG_IMPORT
RESUME_OPTIMIZE
```

---

### 6.2 任务状态设计

前端统一任务状态：

```ts
export type AiTaskStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'DISMISSED'
```

状态含义：

|状态|含义|
|---|---|
|PENDING|已创建，等待执行|
|RUNNING|正在执行|
|COMPLETED|已完成|
|FAILED|执行失败|
|CANCELLED|用户已停止|
|DISMISSED|用户不再提示|

---

### 6.3 前端任务对象设计

新增文件：

```text
frontend/intern-pilot-frontend/src/stores/aiTaskCenter.ts
```

核心结构：

```ts
export interface GlobalAiTask {
  localTaskId: string
  backendTaskNo?: string
  type: AiTaskType
  title: string
  description?: string
  status: AiTaskStatus
  progress: number
  message?: string
  errorMessage?: string
  resultId?: number | string
  resultPath?: string
  sourcePath?: string
  createdAt: string
  updatedAt: string
  finishedAt?: string
  dismissible: boolean
  cancellable: boolean
}
```

字段说明：

|字段|说明|
|---|---|
|localTaskId|前端本地任务 ID|
|backendTaskNo|后端任务编号，AI 分析任务使用|
|type|任务类型|
|title|任务标题|
|description|任务描述|
|status|当前状态|
|progress|进度百分比|
|message|当前提示|
|errorMessage|错误信息|
|resultId|结果 ID，例如 reportId、batchId|
|resultPath|查看结果的前端路由|
|sourcePath|发起任务的页面|
|dismissible|是否可不再提示|
|cancellable|是否可停止|

---

## 7. 前端架构设计

### 7.1 新增文件

建议新增：

```text
src/stores/aiTaskCenter.ts
src/components/ai/AiTaskFloat.vue
src/components/ai/AiTaskDrawer.vue
src/components/ai/AiTaskItem.vue
src/composables/useAiTaskRunner.ts
src/api/feedback.ts
src/components/feedback/FeedbackFloat.vue
src/components/feedback/FeedbackDrawer.vue
src/views/admin/AdminFeedbackList.vue
```

---

### 7.2 全局挂载位置

在主布局中挂载：

```text
src/components/layout/AppLayout.vue
```

加入：

```vue
<AiTaskFloat />
<FeedbackFloat />
```

建议只在登录后显示：

```vue
<AiTaskFloat v-if="authStore.isLoggedIn" />
<FeedbackFloat v-if="authStore.isLoggedIn" />
```

管理员后台也可以显示反馈入口，但可以暂时只在普通用户布局显示。

---

## 8. 全局 AI 任务中心交互设计

### 8.1 右下角悬浮任务入口

位置：

```text
页面右下角
```

显示逻辑：

```text
无任务：
    不显示，或显示一个很小的 AI 图标

有运行中任务：
    显示“AI 任务 2 个进行中”

有完成未查看任务：
    显示“有 1 个 AI 结果可查看”

有失败任务：
    显示“有 1 个 AI 任务失败”
```

按钮样式建议：

```text
圆角卡片 + 图标 + 数量角标
```

---

### 8.2 点击后打开任务抽屉

抽屉标题：

```text
AI 任务中心
```

任务列表显示：

```text
[进行中] AI 简历匹配分析
正在调用 DeepSeek 生成分析报告
进度：60%
[停止任务]

[已完成] 岗位推荐生成
已生成 10 个推荐岗位
[查看结果] [不再提示]

[失败] 面试题生成
DeepSeek 服务暂时不可用
[重试] [不再提示]
```

---

### 8.3 任务完成通知

任务完成后右下角弹出通知：

```text
AI 分析完成
你的简历匹配分析报告已生成

[查看报告] [稍后再看]
```

岗位推荐完成：

```text
岗位推荐生成完成
系统已根据你的简历生成推荐岗位

[查看推荐] [稍后再看]
```

面试题完成：

```text
面试题生成完成
你的专属面试题报告已生成

[查看面试题] [稍后再看]
```

---

## 9. AI 分析任务后端增强设计

### 9.1 修改状态枚举

修改：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/enums/AnalysisTaskStatusEnum.java
```

新增：

```java
CANCELLED("CANCELLED", "任务已取消", 100);
```

同时修改：

```java
public boolean isTerminal() {
    return this == COMPLETED || this == FAILED || this == CANCELLED;
}
```

---

### 9.2 新增接口

修改：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/controller/analysis/AnalysisTaskController.java
```

新增接口：

```text
GET /api/analysis/tasks/running
POST /api/analysis/tasks/{taskNo}/cancel
GET /api/analysis/tasks/recent
```

接口说明：

|接口|说明|
|---|---|
|GET /api/analysis/tasks/running|查询当前用户运行中的 AI 分析任务|
|POST /api/analysis/tasks/{taskNo}/cancel|取消 AI 分析任务|
|GET /api/analysis/tasks/recent|查询当前用户最近任务，用于刷新后恢复|

---

### 9.3 Service 新增方法

修改：

```text
AnalysisTaskService.java
AnalysisTaskServiceImpl.java
```

新增方法：

```java
List<AnalysisTaskDetailResponse> listRunningTasks();

AnalysisTaskDetailResponse cancelTask(String taskNo);

List<AnalysisTaskDetailResponse> listRecentTasks(Integer limit);
```

---

### 9.4 取消任务逻辑

取消规则：

```text
1. 只能取消自己的任务
2. 已完成 / 已失败任务不能取消
3. 取消后状态改为 CANCELLED
4. WebSocket 推送 CANCELLED 消息
5. executeTask 每个阶段开始前检查是否已取消
```

伪代码：

```java
private boolean isCancelled(String taskNo, Long userId) {
    AnalysisTask latest = getTaskByTaskNoAndUserId(taskNo, userId);
    return latest != null && AnalysisTaskStatusEnum.CANCELLED.getCode().equals(latest.getStatus());
}
```

在 `executeTask` 中增加：

```java
if (isCancelled(taskNo, userId)) {
    return;
}
```

注意：

```text
如果任务已经进入 DeepSeek 调用中，无法保证立即中断外部请求。
请求返回后应再次检查任务是否已取消。
如果已取消，则不再推送 COMPLETED 通知。
```

---

## 10. 岗位推荐任务前端接入设计

### 10.1 当前接口保持不变

保留：

```text
POST /api/job-recommendations/generate
```

暂不改后端异步。

---

### 10.2 前端改造方式

修改：

```text
src/views/recommendation/JobRecommendationList.vue
```

当前逻辑大概是：

```ts
generating.value = true
const res = await generateJobRecommendationApi(form)
ElMessage.success('推荐生成成功')
router.push(`/job-recommendations/${res.batchId}`)
generating.value = false
```

改造为：

```ts
const aiTaskStore = useAiTaskCenterStore()

const task = aiTaskStore.createLocalTask({
  type: 'JOB_RECOMMENDATION',
  title: '岗位推荐生成',
  description: '正在根据简历和岗位库生成推荐结果',
  sourcePath: route.fullPath,
  cancellable: false,
  dismissible: true
})

try {
  aiTaskStore.updateTask(task.localTaskId, {
    status: 'RUNNING',
    progress: 30,
    message: '正在分析简历画像与岗位库'
  })

  const res = await generateJobRecommendationApi(form)

  aiTaskStore.completeTask(task.localTaskId, {
    resultId: res.batchId,
    resultPath: `/job-recommendations/${res.batchId}`,
    message: '岗位推荐生成完成'
  })
} catch (e) {
  aiTaskStore.failTask(task.localTaskId, '岗位推荐生成失败')
}
```

---

## 11. 面试题生成任务前端接入设计

### 11.1 当前接口保持不变

保留：

```text
POST /api/interview-questions/generate
POST /api/interview-questions/{reportId}/regenerate
```

暂不改后端异步。

---

### 11.2 前端改造方式

修改：

```text
src/views/interview/InterviewQuestionList.vue
```

生成面试题时：

```ts
const task = aiTaskStore.createLocalTask({
  type: 'INTERVIEW_QUESTION',
  title: 'AI 面试题生成',
  description: '正在根据简历和岗位 JD 生成专属面试题',
  sourcePath: route.fullPath,
  cancellable: false,
  dismissible: true
})
```

成功后：

```ts
aiTaskStore.completeTask(task.localTaskId, {
  resultId: res.reportId,
  resultPath: `/interview-questions/${res.reportId}`,
  message: '面试题生成完成'
})
```

失败后：

```ts
aiTaskStore.failTask(task.localTaskId, '面试题生成失败')
```

重新生成时任务类型使用：

```text
INTERVIEW_REGENERATE
```

---

## 12. 页面切换后任务恢复设计

### 12.1 localStorage 存储

前端任务中心使用 localStorage 保存非敏感任务状态。

key：

```text
internpilot:ai-task-center
```

存储内容：

```json
[
  {
    "localTaskId": "LOCAL_20260519_xxxx",
    "backendTaskNo": "TASK_20260519_xxxx",
    "type": "ANALYSIS_MATCH",
    "title": "AI 简历匹配分析",
    "status": "RUNNING",
    "progress": 60,
    "resultPath": "",
    "createdAt": "2026-05-19T10:00:00"
  }
]
```

注意：

```text
不要存储完整简历内容
不要存储完整 JD 内容
不要存储 DeepSeek 原始响应
不要存储 token
```

---

### 12.2 应用启动时恢复

在：

```text
src/main.ts
或
src/components/layout/AppLayout.vue
```

初始化：

```ts
const aiTaskStore = useAiTaskCenterStore()
aiTaskStore.restoreFromStorage()
aiTaskStore.syncRunningAnalysisTasks()
```

恢复逻辑：

```text
1. 从 localStorage 恢复本地任务
2. 调用 GET /api/analysis/tasks/running 查询后端运行中分析任务
3. 对运行中的后端任务重新建立 WebSocket
4. 对没有后端 taskNo 的本地任务，只展示最终状态，不重新执行
```

---

## 13. 全局通知规则

### 13.1 通知触发条件

以下状态触发通知：

```text
COMPLETED
FAILED
CANCELLED
```

通知规则：

```text
1. 同一个任务只通知一次
2. 用户点击“不再提示”后不重复通知
3. 当前正在结果页时，可以只显示轻提示
4. 用户不在发起页时，显示“查看结果”按钮
```

---

### 13.2 Element Plus 实现建议

使用：

```ts
ElNotification({
  title: 'AI 分析完成',
  message: '你的简历匹配报告已生成',
  type: 'success',
  duration: 0
})
```

由于 Element Plus 的 `message` 不方便放复杂按钮，建议第一版：

```text
通知只提示完成
点击通知后打开任务中心
任务中心里放“查看结果”按钮
```

这样实现更稳。

---

## 14. 用户反馈功能设计

## 14.1 前端入口

新增两个入口：

```text
1. 右下角悬浮“反馈”按钮
2. 用户头像菜单中增加“反馈建议”
```

第一版优先实现右下角悬浮按钮。

---

### 14.2 反馈表单字段

用户填写：

|字段|必填|说明|
|---|---|---|
|type|是|反馈类型|
|title|是|反馈标题|
|content|是|反馈内容|
|contact|否|联系方式|
|allowContact|否|是否允许联系|
|pageUrl|自动|当前页面路径|
|browserInfo|自动|浏览器信息|

反馈类型：

```text
BUG              功能异常
SUGGESTION       使用建议
UI_UX            页面体验问题
AI_RESULT        AI 结果不准确
PERFORMANCE      系统响应太慢
OTHER            其他
```

---

### 14.3 后端表设计

新增表：

```sql
CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(32) NOT NULL COMMENT '反馈类型',
    title VARCHAR(120) NOT NULL COMMENT '反馈标题',
    content TEXT NOT NULL COMMENT '反馈内容',
    page_url VARCHAR(255) DEFAULT NULL COMMENT '反馈发生页面',
    contact VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
    allow_contact TINYINT DEFAULT 0 COMMENT '是否允许联系',
    browser_info VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    admin_reply TEXT DEFAULT NULL COMMENT '管理员回复',
    handled_by BIGINT DEFAULT NULL COMMENT '处理人ID',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
) COMMENT='用户反馈表';
```

---

### 14.4 反馈状态

```text
PENDING      待处理
PROCESSING   处理中
RESOLVED     已解决
IGNORED      已忽略
```

---

### 14.5 后端新增文件

建议新增：

```text
entity/UserFeedback.java
mapper/UserFeedbackMapper.java
dto/feedback/FeedbackCreateRequest.java
dto/feedback/FeedbackStatusUpdateRequest.java
dto/feedback/FeedbackReplyRequest.java
vo/feedback/FeedbackResponse.java
enums/FeedbackTypeEnum.java
enums/FeedbackStatusEnum.java
service/feedback/FeedbackService.java
service/feedback/impl/FeedbackServiceImpl.java
controller/feedback/FeedbackController.java
controller/admin/AdminFeedbackController.java
```

---

### 14.6 普通用户接口

```text
POST /api/feedback
GET /api/feedback/my
GET /api/feedback/my/{id}
```

说明：

|接口|权限|说明|
|---|---|---|
|POST /api/feedback|登录用户|提交反馈|
|GET /api/feedback/my|登录用户|查看我的反馈|
|GET /api/feedback/my/{id}|登录用户|查看我的反馈详情|

---

### 14.7 管理员接口

```text
GET /api/admin/feedback
GET /api/admin/feedback/{id}
PUT /api/admin/feedback/{id}/status
PUT /api/admin/feedback/{id}/reply
DELETE /api/admin/feedback/{id}
```

说明：

|接口|权限|说明|
|---|---|---|
|GET /api/admin/feedback|feedback:read|查询反馈列表|
|GET /api/admin/feedback/{id}|feedback:read|查询反馈详情|
|PUT /api/admin/feedback/{id}/status|feedback:write|修改处理状态|
|PUT /api/admin/feedback/{id}/reply|feedback:write|回复反馈|
|DELETE /api/admin/feedback/{id}|feedback:delete|删除反馈|

---

## 15. 权限设计

### 15.1 新增权限

在 RBAC 权限表中新增：

```text
feedback:read
feedback:write
feedback:delete
```

管理员角色拥有全部权限。

普通用户不需要单独分配反馈提交权限，只要登录即可提交自己的反馈。

---

### 15.2 Spring Security 配置

普通用户接口：

```text
/api/feedback/**
```

要求登录。

管理员接口：

```text
/api/admin/feedback/**
```

要求对应权限。

---

## 16. 前端页面设计

### 16.1 FeedbackFloat.vue

功能：

```text
1. 右下角悬浮反馈按钮
2. 点击打开 FeedbackDrawer
3. 登录后显示
4. 移动端适配
```

按钮文案：

```text
反馈
```

---

### 16.2 FeedbackDrawer.vue

字段：

```text
反馈类型
反馈标题
反馈内容
联系方式，可选
是否允许联系
当前页面，自动填充
```

按钮：

```text
取消
提交反馈
```

提交成功提示：

```text
反馈提交成功，感谢你的建议
```

---

### 16.3 AdminFeedbackList.vue

管理员后台新增菜单：

```text
用户反馈
```

页面功能：

```text
1. 反馈列表
2. 按类型筛选
3. 按状态筛选
4. 查看反馈详情
5. 修改状态
6. 填写管理员回复
7. 删除无效反馈
```

表格字段：

```text
反馈标题
反馈类型
提交用户
当前状态
页面路径
创建时间
操作
```

---

## 17. 页面改造点

### 17.1 AnalysisMatch.vue

改造目标：

```text
1. 不再自己独立维护完整任务生命周期
2. 点击开始分析后创建后端任务
3. 将任务注册到 aiTaskCenterStore
4. 页面展示从 store 中读取的当前任务状态
5. 页面卸载时不再直接清理全局任务
6. WebSocket 连接由任务中心统一管理
```

注意：

```text
页面可以显示任务进度，但不拥有任务生命周期。
```

---

### 17.2 JobRecommendationList.vue

改造目标：

```text
1. 生成岗位推荐时创建本地全局任务
2. 请求完成后更新任务为 COMPLETED
3. resultPath 指向 /job-recommendations/{batchId}
4. 右下角任务中心显示完成通知
```

---

### 17.3 InterviewQuestionList.vue

改造目标：

```text
1. 生成面试题时创建本地全局任务
2. 重新生成面试题时也创建本地任务
3. 成功后 resultPath 指向 /interview-questions/{reportId}
4. 失败后任务中心展示失败信息
```

---

## 18. 开发步骤建议

### 第 1 步：先提交当前登录注册页优化

当前你还在：

```text
feature/frontend-ui-polish
```

先提交：

```bash
git status
git add .
git commit -m "Polish login and register pages"
git push origin feature/frontend-ui-polish
```

然后合并到 dev：

```bash
git checkout dev
git pull origin dev
git merge feature/frontend-ui-polish
git push origin dev
```

---

### 第 2 步：新建本轮功能分支

```bash
git checkout dev
git pull origin dev
git checkout -b feature/ai-task-center-feedback
```

---

### 第 3 步：新增设计文档

```bash
mkdir -p docs
touch docs/39-ai-task-center-and-feedback-design.md
```

把本文档内容复制进去。

---

### 第 4 步：先做全局 AI 任务中心

推荐顺序：

```text
1. 新增 aiTaskCenter Store
2. 新增 AiTaskFloat / AiTaskDrawer
3. AppLayout 挂载全局任务入口
4. analysisTask.ts 增加 running / cancel 接口
5. 后端 AnalysisTask 增加 running / cancel 能力
6. AnalysisMatch.vue 接入全局任务中心
7. JobRecommendationList.vue 接入本地任务追踪
8. InterviewQuestionList.vue 接入本地任务追踪
```

---

### 第 5 步：再做用户反馈功能

推荐顺序：

```text
1. 新增 user_feedback 表
2. 新增 Feedback entity / mapper / service / controller
3. 新增普通用户提交接口
4. 新增 FeedbackFloat / FeedbackDrawer
5. 新增管理员反馈列表
6. 新增 RBAC 权限
7. 前后端联调
```

---

## 19. 测试清单

### 19.1 AI 任务中心测试

必须测试：

```text
1. 点击 AI 分析后，任务中心显示任务进行中
2. 切换页面后，任务中心仍然显示任务
3. 再切回 AI 分析页，进度仍然显示
4. AI 分析完成后，右下角通知完成
5. 点击查看结果，可以进入分析报告页面
6. AI 分析失败后，任务中心显示失败
7. 点击停止任务后，后端状态变为 CANCELLED
8. 已完成任务点击“不再提示”后，从任务中心隐藏
9. 刷新页面后，运行中的 AI 分析任务可以恢复
10. WebSocket 断开时，轮询兜底仍然能更新状态
```

---

### 19.2 岗位推荐测试

```text
1. 点击生成岗位推荐后，任务中心出现任务
2. 切换到其他页面，任务仍然显示
3. 生成成功后，通知用户
4. 点击查看结果进入推荐详情页
5. 生成失败后，任务中心显示失败原因
```

---

### 19.3 面试题生成测试

```text
1. 点击生成面试题后，任务中心出现任务
2. 切换页面后不丢失任务提示
3. 生成成功后可跳转详情页
4. 重新生成面试题也能进入任务中心
5. 失败后显示失败状态
```

---

### 19.4 用户反馈测试

```text
1. 登录用户可以打开反馈抽屉
2. 不填标题不能提交
3. 不填内容不能提交
4. 选择反馈类型后可以提交
5. 提交后数据库生成 user_feedback 记录
6. 我的反馈列表可以看到自己提交的反馈
7. 管理员可以查看所有反馈
8. 管理员可以修改反馈状态
9. 管理员可以回复反馈
10. 普通用户不能访问管理员反馈接口
```

---

## 20. 验收标准

本阶段完成后，应满足：

```text
1. AI 分析任务切换页面不消失
2. AI 分析任务可从全局任务中心查看
3. AI 分析完成后有右下角通知
4. 岗位推荐生成也能进入全局任务提示
5. 面试题生成也能进入全局任务提示
6. 用户可以提交反馈
7. 管理员可以查看和处理反馈
8. 不影响已有登录注册、用户中心、管理员后台
9. npm run build 通过
10. ./gradlew test 通过
```

---

## 21. 推荐提交信息

```bash
git add .
git commit -m "Add global AI task center and user feedback module"
git push origin feature/ai-task-center-feedback
```

合并 dev 后：

```bash
git checkout dev
git pull origin dev
git merge feature/ai-task-center-feedback
git push origin dev
```

---

# 给 Trae + DeepSeek Pro 的实现提示词

下面这段可以直接发给 Trae + DeepSeek Pro：

我正在开发 InternPilot 智能实习领航员项目，技术栈是 Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API。当前分支是 `feature/ai-task-center-feedback`，请严格按照 `docs/39-ai-task-center-and-feedback-design.md` 实现“全局 AI 任务中心 + 用户反馈功能”。

当前项目已有：

- 后端 `AnalysisTask`、`AnalysisTaskController`、`AnalysisTaskServiceImpl`
    
- WebSocket AI 分析进度
    
- 前端 `src/api/analysisTask.ts`
    
- 前端 `src/utils/analysisSocket.ts`
    
- 前端 `src/views/analysis/AnalysisMatch.vue`
    
- 岗位推荐页面 `JobRecommendationList.vue`
    
- 面试题页面 `InterviewQuestionList.vue`
    
- Element Plus、Pinia、Vue Router
    
- RBAC 管理员后台
    

实现要求：

1. 新增全局 AI 任务中心
    

- 新增 `src/stores/aiTaskCenter.ts`
    
- 新增 `src/components/ai/AiTaskFloat.vue`
    
- 新增 `src/components/ai/AiTaskDrawer.vue`
    
- 新增 `src/components/ai/AiTaskItem.vue`
    
- 在 `AppLayout.vue` 中挂载全局 AI 任务入口
    
- 支持任务状态：PENDING、RUNNING、COMPLETED、FAILED、CANCELLED、DISMISSED
    
- 支持任务类型：ANALYSIS_MATCH、JOB_RECOMMENDATION、INTERVIEW_QUESTION、INTERVIEW_REGENERATE
    
- 任务状态保存到 localStorage，但不要保存敏感内容
    

2. 改造 AI 分析任务
    

- 后端 `AnalysisTaskStatusEnum` 增加 `CANCELLED`
    
- 后端新增接口：
    
    - `GET /api/analysis/tasks/running`
        
    - `POST /api/analysis/tasks/{taskNo}/cancel`
        
    - `GET /api/analysis/tasks/recent`
        
- `AnalysisTaskServiceImpl` 支持查询当前用户运行中任务、取消任务、查询最近任务
    
- `executeTask` 每个阶段前检查是否已取消
    
- 已取消任务不再推送 COMPLETED
    
- 前端 `AnalysisMatch.vue` 改为接入全局任务中心，页面只展示任务，不独占任务生命周期
    

3. 改造岗位推荐和面试题生成
    

- 岗位推荐接口暂时保持同步，不改后端异步
    
- 面试题生成接口暂时保持同步，不改后端异步
    
- 但前端需要在生成开始时创建全局任务，成功后标记 COMPLETED，失败后标记 FAILED
    
- 完成后任务中心可以点击查看结果
    

4. 新增用户反馈功能
    

- 新增 `user_feedback` 表
    
- 新增后端 entity、mapper、dto、vo、service、controller
    
- 普通用户接口：
    
    - `POST /api/feedback`
        
    - `GET /api/feedback/my`
        
    - `GET /api/feedback/my/{id}`
        
- 管理员接口：
    
    - `GET /api/admin/feedback`
        
    - `GET /api/admin/feedback/{id}`
        
    - `PUT /api/admin/feedback/{id}/status`
        
    - `PUT /api/admin/feedback/{id}/reply`
        
    - `DELETE /api/admin/feedback/{id}`
        
- 新增反馈类型：BUG、SUGGESTION、UI_UX、AI_RESULT、PERFORMANCE、OTHER
    
- 新增反馈状态：PENDING、PROCESSING、RESOLVED、IGNORED
    
- 前端新增 `FeedbackFloat.vue`、`FeedbackDrawer.vue`
    
- 管理员后台新增 `AdminFeedbackList.vue`
    
- RBAC 新增权限：feedback:read、feedback:write、feedback:delete
    

5. 输出要求  
    完成后请输出：
    

- 做了什么
    
- 修改了哪些文件
    
- 新增了哪些文件
    
- 遇到的问题和解决方案
    
- 如何测试
    
- 还需要我提供什么
    

---

# 给 Codex 的复查提示词

下面这段用于 Trae 实现完成后，让 Codex 检查修复：

请你作为代码审查与修复助手，检查 InternPilot 项目中 `feature/ai-task-center-feedback` 分支的实现质量。本轮功能对应 `docs/39-ai-task-center-and-feedback-design.md`，目标是实现“全局 AI 任务中心 + 用户反馈功能”。

请重点检查：

1. 后端 AI 分析任务
    

- `AnalysisTaskStatusEnum` 是否正确新增 CANCELLED
    
- `isTerminal()` 是否包含 CANCELLED
    
- `GET /api/analysis/tasks/running` 是否只返回当前用户任务
    
- `POST /api/analysis/tasks/{taskNo}/cancel` 是否校验用户权限
    
- 已完成 / 已失败任务是否不能取消
    
- `executeTask` 是否在关键阶段检查取消状态
    
- WebSocket 是否能推送 CANCELLED / FAILED / COMPLETED
    
- 是否存在并发状态覆盖问题，例如用户取消后又被更新为 COMPLETED
    

2. 前端全局 AI 任务中心
    

- `aiTaskCenter` Pinia Store 是否设计合理
    
- localStorage 是否没有保存敏感内容
    
- 页面刷新后运行中的 AI 分析任务是否能恢复
    
- 页面切换后任务状态是否不丢失
    
- WebSocket 和轮询是否没有重复创建、内存泄漏
    
- 任务完成通知是否不会重复弹出
    
- 查看结果路由是否正确
    

3. 岗位推荐和面试题生成
    

- 生成任务是否接入全局任务中心
    
- 成功后是否能跳转结果页
    
- 失败后是否能显示失败状态
    
- 原有页面 loading 是否没有冲突
    
- 切换页面后是否仍然能看到完成提示
    

4. 用户反馈模块
    

- `user_feedback` 表结构是否合理
    
- 普通用户是否只能查看自己的反馈
    
- 管理员是否能查看全部反馈
    
- 管理员修改状态和回复是否有权限控制
    
- RBAC 权限是否初始化
    
- 前端反馈表单是否有必填校验
    
- 当前页面路径和浏览器信息是否正确记录
    
- 普通用户不能访问 `/api/admin/feedback/**`
    

5. 工程质量
    

- 是否符合现有包结构和命名风格
    
- 是否符合 Result 返回格式
    
- 是否有明显 NPE 或空值问题
    
- 是否有 TypeScript 类型错误
    
- 是否破坏现有登录、用户中心、管理员后台、AI 分析、岗位推荐、面试题功能
    
- 是否能通过：
    
    - 后端：`./gradlew test --no-daemon --max-workers=1`
        
    - 前端：`npm run build`
        

请直接进行检查、修复和验证。完成后输出：

- 检查了什么
    
- 修复了什么
    
- 发现的问题和解决方案
    
- 测试命令和结果
    
- 仍然存在的风险
    
- 需要我手动验证的地方
    

---

# 你现在的下一步

先把登录注册页优化从 `feature/frontend-ui-polish` 提交并合并到 `dev`，然后新建：

```bash
git checkout dev
git pull origin dev
git checkout -b feature/ai-task-center-feedback
```

再把上面的文档保存为：

```text
docs/39-ai-task-center-and-feedback-design.md
```

这一版做完后，你的项目产品感会明显提升：不只是“能调用 AI”，而是“AI 长任务体验像正式系统”。