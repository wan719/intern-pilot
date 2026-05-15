# InternPilot AI 面试题生成增强与测试设计文档

## 一、文档目的

本文档用于设计 InternPilot 项目中 AI 面试题生成模块的后续增强方案。

当前项目已经具备：

1. 用户认证与 JWT 鉴权
2. RBAC 权限系统
3. 简历上传与解析
4. 岗位 JD 管理
5. AI 简历匹配分析
6. WebSocket AI 分析进度推送
7. AI 分析缓存增强
8. Mock AI 多场景适配
9. RAG 岗位知识库基础能力
10. AI 面试题生成基础能力

在完成 `32-ai-analysis-cache-and-mock-ai-enhancement-design.md` 后，AI 调用底座已经更加稳定，因此本阶段可以开始增强面试题生成模块。

本阶段目标是将 AI 面试题功能从“能生成题目”升级为“能根据简历、岗位、分析短板生成结构化面试题，并支持分类、难度、参考答案、追问问题和测试覆盖”的完整功能模块。

---

## 二、当前面试题模块现状

### 2.1 已有能力

当前面试题模块已经具备：

1. 基于 AI 生成面试题
2. 保存面试题记录
3. 查询面试题列表
4. 查看面试题详情
5. 前端展示面试题页面
6. Mock AI 已支持面试题生成场景
7. RAG 构建异常已有 warn 日志兜底

### 2.2 当前不足

| 问题 | 说明 |
|---|---|
| 题目结构不够细 | 需要区分题目分类、难度、参考答案、追问问题 |
| 生成依据不够明确 | 需要支持基于简历、岗位 JD、分析短板生成题目 |
| 题目和岗位关联不足 | 需要让题目更贴近目标岗位 |
| 题目和简历短板关联不足 | 需要针对用户薄弱点生成题目 |
| Mock 数据需要更稳定 | 需要保证本地演示环境可直接展示 |
| 测试覆盖不足 | 需要补充单元测试和集成测试 |

---

## 三、本阶段建设目标

### 3.1 总体目标

本阶段目标是：

> 增强 InternPilot 的 AI 面试题生成模块，使其支持基于简历、岗位 JD、AI 分析短板生成结构化面试题，并补充单元测试、集成测试和 README 展示说明。

### 3.2 具体目标

1. 支持按题目类型生成面试题
2. 支持按难度生成面试题
3. 支持生成参考答案
4. 支持生成追问问题
5. 支持基于岗位 JD 生成题目
6. 支持基于简历内容生成题目
7. 支持基于 AI 分析短板生成题目
8. 支持题目重新生成
9. 支持前端展示分类、难度、答案、追问
10. 补充单元测试
11. 补充集成测试
12. 更新 README 项目亮点和接口说明

---

## 四、本阶段不做什么

本阶段不做过重业务扩展，暂时不做：

1. 面试题收藏
2. 刷题记录
3. 用户答题打分
4. AI 口语面试
5. 语音识别
6. 视频面试
7. 面试题分享
8. 面试题公开题库
9. 复杂推荐算法
10. 多轮 AI 面试官对话

这些功能可以放到后续阶段。

---

## 五、面试题生成业务场景

### 5.1 基于岗位 JD 生成题目

输入：

```text
岗位 JD
岗位技能要求
岗位类型
公司名称
````

输出：

```text
与该岗位强相关的面试题
```

适合场景：

```text
用户收藏了一个 Java 后端实习岗位
希望提前准备这个岗位可能问到的问题
```

---

### 5.2 基于简历生成题目

输入：

```text
用户简历内容
项目经历
技术栈
实习经历
```

输出：

```text
围绕用户简历项目经历的追问题
```

适合场景：

```text
用户想知道面试官会如何追问自己的项目
```

---

### 5.3 基于 AI 分析短板生成题目

输入：

```text
AI 简历匹配分析报告
weaknesses
suggestions
岗位要求
```

输出：

```text
针对用户薄弱点的补强题目
```

适合场景：

```text
AI 分析指出用户 Redis、并发、项目部署经验不足
系统自动生成相关面试题
```

---

## 六、题目结构设计

### 6.1 面试题字段设计

建议每道题包含：

|字段|说明|
|---|---|
|category|题目分类|
|difficulty|题目难度|
|question|题目内容|
|answer|参考答案|
|followUps|追问问题|
|keywords|关键词|
|source|生成依据|
|sortOrder|排序|

### 6.2 题目分类

建议分类：

```text
Java基础
Spring Boot
Spring Security
MyBatis
MySQL
Redis
Vue
项目经验
系统设计
场景题
HR问题
```

### 6.3 难度等级

建议难度：

|难度|说明|
|---|---|
|BASIC|基础题|
|MEDIUM|中等题|
|HARD|困难题|

前端展示可转换为：

```text
基础
中等
困难
```

---

## 七、DTO 设计

### 7.1 InterviewQuestionGenerateRequest

```java
public class InterviewQuestionGenerateRequest {

    private Long resumeId;

    private Long resumeVersionId;

    private Long jobId;

    private Long analysisReportId;

    private Integer questionCount;

    private List<String> categories;

    private List<String> difficulties;

    private Boolean includeAnswer;

    private Boolean includeFollowUps;
}
```

字段说明：

|字段|说明|
|---|---|
|resumeId|简历 ID|
|resumeVersionId|简历版本 ID，可选|
|jobId|岗位 ID|
|analysisReportId|AI 分析报告 ID，可选|
|questionCount|题目数量|
|categories|题目分类|
|difficulties|难度列表|
|includeAnswer|是否生成参考答案|
|includeFollowUps|是否生成追问|

---

### 7.2 InterviewQuestionItemDTO

```java
public class InterviewQuestionItemDTO {

    private String category;

    private String difficulty;

    private String question;

    private String answer;

    private List<String> followUps;

    private List<String> keywords;

    private String source;

    private Integer sortOrder;
}
```

---

### 7.3 InterviewQuestionGenerateResult

```java
public class InterviewQuestionGenerateResult {

    private Long recordId;

    private Long userId;

    private Long resumeId;

    private Long jobId;

    private Long analysisReportId;

    private List<InterviewQuestionItemDTO> questions;

    private LocalDateTime createdAt;
}
```

---

## 八、数据库设计增强

### 8.1 当前表可复用方案

如果当前已有面试题记录表，例如：

```text
interview_question
interview_question_record
```

可以优先复用，不一定新建表。

建议保存方式：

|字段|说明|
|---|---|
|id|主键|
|user_id|用户 ID|
|resume_id|简历 ID|
|resume_version_id|简历版本 ID|
|job_id|岗位 ID|
|analysis_report_id|分析报告 ID|
|title|记录标题|
|question_json|题目 JSON|
|question_count|题目数量|
|generated_by|生成方式，AI / MOCK|
|deleted|逻辑删除|
|created_at|创建时间|
|updated_at|更新时间|

### 8.2 question_json 示例

```json
{
  "questions": [
    {
      "category": "Java基础",
      "difficulty": "BASIC",
      "question": "HashMap 的底层数据结构是什么？",
      "answer": "JDK 8 中 HashMap 由数组、链表和红黑树组成。",
      "followUps": [
        "HashMap 为什么线程不安全？",
        "ConcurrentHashMap 如何保证线程安全？"
      ],
      "keywords": ["HashMap", "数组", "链表", "红黑树"],
      "source": "根据岗位技能要求 Java 集合生成",
      "sortOrder": 1
    }
  ]
}
```

---

## 九、Prompt 设计

### 9.1 Prompt 目标

Prompt 应要求 AI 输出稳定 JSON，避免返回散文。

核心要求：

1. 只返回 JSON
    
2. 不要 Markdown 代码块
    
3. 每道题包含分类、难度、问题、参考答案、追问、关键词
    
4. 题目要结合简历和岗位
    
5. 题目要针对用户短板
    
6. 题目数量可控
    

---

### 9.2 Prompt 模板示例

```text
你是一个 Java 后端实习面试官。

请根据下面的信息，为候选人生成结构化面试题。

【候选人简历】
{resumeContent}

【目标岗位 JD】
{jobContent}

【AI 匹配分析短板】
{weaknesses}

【生成要求】
1. 生成 {questionCount} 道面试题。
2. 题目分类必须从以下范围选择：
Java基础、Spring Boot、Spring Security、MyBatis、MySQL、Redis、Vue、项目经验、系统设计、场景题、HR问题。
3. 难度必须为 BASIC、MEDIUM、HARD。
4. 每道题必须包含 question、answer、followUps、keywords。
5. 题目要贴合岗位 JD。
6. 项目经验题要围绕候选人的项目经历追问。
7. 针对短板生成补强题。
8. 只返回 JSON，不要返回 Markdown 代码块，不要额外解释。

【返回 JSON 格式】
{
  "questions": [
    {
      "category": "Java基础",
      "difficulty": "BASIC",
      "question": "问题内容",
      "answer": "参考答案",
      "followUps": ["追问1", "追问2"],
      "keywords": ["关键词1", "关键词2"],
      "source": "生成依据",
      "sortOrder": 1
    }
  ]
}
```

---

## 十、后端接口设计

### 10.1 生成面试题

```text
POST /api/interview-questions/generate
```

权限：

```java
@PreAuthorize("hasAuthority('interview-question:create')")
```

请求：

```json
{
  "resumeId": 1,
  "resumeVersionId": 3,
  "jobId": 2,
  "analysisReportId": 10,
  "questionCount": 8,
  "categories": ["Java基础", "Spring Boot", "项目经验"],
  "difficulties": ["BASIC", "MEDIUM"],
  "includeAnswer": true,
  "includeFollowUps": true
}
```

响应：

```json
{
  "recordId": 12,
  "questionCount": 8,
  "questions": [
    {
      "category": "Spring Boot",
      "difficulty": "MEDIUM",
      "question": "Spring Boot 自动配置的原理是什么？",
      "answer": "Spring Boot 通过自动配置类、条件注解和配置元数据完成自动装配。",
      "followUps": [
        "常见的条件注解有哪些？",
        "如何自定义 starter？"
      ],
      "keywords": ["自动配置", "条件注解", "starter"],
      "source": "根据岗位技能要求 Spring Boot 生成",
      "sortOrder": 1
    }
  ]
}
```

---

### 10.2 查询面试题列表

```text
GET /api/interview-questions
```

权限：

```java
@PreAuthorize("hasAuthority('interview-question:read')")
```

查询参数：

```text
resumeId
jobId
keyword
page
size
```

---

### 10.3 查询面试题详情

```text
GET /api/interview-questions/{id}
```

权限：

```java
@PreAuthorize("hasAuthority('interview-question:read')")
```

安全要求：

```text
只能查询自己的面试题记录
管理员可在后台扩展查看所有记录
```

---

### 10.4 删除面试题记录

```text
DELETE /api/interview-questions/{id}
```

权限：

```java
@PreAuthorize("hasAuthority('interview-question:delete')")
```

---

### 10.5 重新生成面试题

```text
POST /api/interview-questions/{id}/regenerate
```

权限：

```java
@PreAuthorize("hasAuthority('interview-question:create')")
```

说明：

```text
复用原记录的 resumeId、jobId、analysisReportId 等上下文，重新调用 AI 生成题目。
```

---

## 十一、后端服务设计

### 11.1 InterviewQuestionService

```java
public interface InterviewQuestionService {

    InterviewQuestionGenerateResult generate(Long userId, InterviewQuestionGenerateRequest request);

    PageResult<InterviewQuestionRecordVO> list(Long userId, InterviewQuestionQueryRequest request);

    InterviewQuestionDetailVO getDetail(Long userId, Long recordId);

    void delete(Long userId, Long recordId);

    InterviewQuestionGenerateResult regenerate(Long userId, Long recordId);
}
```

---

### 11.2 核心流程

```text
用户请求生成面试题
  ↓
校验 resumeId / jobId / analysisReportId 是否属于当前用户
  ↓
读取简历内容
  ↓
读取岗位 JD
  ↓
读取 AI 分析报告中的 weaknesses / suggestions
  ↓
构建 Prompt
  ↓
调用 AiClient
  ↓
使用 JsonUtils / AiResponseParser 解析 JSON
  ↓
校验 questions 字段
  ↓
保存面试题记录
  ↓
返回题目列表
```

---

## 十二、Mock AI 适配

### 12.1 MockAiClient 已具备场景能力

在 `32` 阶段中，MockAiClient 已经支持：

```text
INTERVIEW_QUESTION_GENERATION
```

本阶段需要检查：

1. Mock 返回是否包含 questions
    
2. 每个 question 是否包含 category
    
3. 每个 question 是否包含 difficulty
    
4. 每个 question 是否包含 answer
    
5. 每个 question 是否包含 followUps
    
6. 每个 question 是否包含 keywords
    

---

### 12.2 Mock 面试题返回示例

```json
{
  "questions": [
    {
      "category": "Java基础",
      "difficulty": "BASIC",
      "question": "HashMap 的底层数据结构是什么？",
      "answer": "JDK 8 中 HashMap 底层由数组、链表和红黑树组成。",
      "followUps": [
        "HashMap 为什么线程不安全？",
        "ConcurrentHashMap 如何保证线程安全？"
      ],
      "keywords": ["HashMap", "集合", "红黑树"],
      "source": "根据岗位 Java 基础要求生成",
      "sortOrder": 1
    },
    {
      "category": "项目经验",
      "difficulty": "MEDIUM",
      "question": "你的 InternPilot 项目中为什么要使用 WebSocket 展示 AI 分析进度？",
      "answer": "AI 分析属于耗时任务，WebSocket 可以实时推送任务阶段，提高用户体验。",
      "followUps": [
        "如果 WebSocket 断开怎么办？",
        "你如何保存任务状态？"
      ],
      "keywords": ["WebSocket", "异步任务", "Redis"],
      "source": "根据简历项目经历生成",
      "sortOrder": 2
    }
  ]
}
```

---

## 十三、前端设计

### 13.1 面试题生成页面

页面功能：

1. 选择简历
    
2. 选择岗位
    
3. 选择 AI 分析报告，可选
    
4. 选择题目数量
    
5. 选择题目分类
    
6. 选择难度
    
7. 是否包含参考答案
    
8. 是否包含追问问题
    
9. 点击生成
    
10. 展示生成结果
    

---

### 13.2 面试题列表页面

展示字段：

|字段|说明|
|---|---|
|标题|面试题记录标题|
|岗位|关联岗位|
|简历|关联简历|
|题目数量|questions 数量|
|创建时间|generatedAt|
|操作|查看、重新生成、删除|

---

### 13.3 面试题详情页面

每道题展示：

```text
题目分类
难度标签
题目内容
参考答案
追问问题
关键词
生成依据
```

前端展示建议：

```text
分类用 Tag
难度用不同颜色 Tag
答案默认折叠
追问问题用列表
关键词用 Tag
```

---

## 十四、权限设计

### 14.1 权限码

需要确认初始化 SQL 中存在：

```text
interview-question:read
interview-question:create
interview-question:delete
```

可选：

```text
interview-question:update
```

### 14.2 权限控制

|操作|权限|
|---|---|
|查看面试题列表|interview-question:read|
|查看面试题详情|interview-question:read|
|生成面试题|interview-question:create|
|重新生成面试题|interview-question:create|
|删除面试题|interview-question:delete|

---

## 十五、异常处理设计

### 15.1 常见异常

|场景|错误码|
|---|---|
|简历不存在|RESUME_NOT_FOUND|
|岗位不存在|JOB_NOT_FOUND|
|分析报告不存在|ANALYSIS_REPORT_NOT_FOUND|
|无权访问该简历|RESUME_ACCESS_DENIED|
|无权访问该岗位|JOB_ACCESS_DENIED|
|AI 返回空内容|AI_RESPONSE_EMPTY|
|AI 返回 JSON 解析失败|AI_RESPONSE_PARSE_FAILED|
|面试题 JSON 缺少 questions|INTERVIEW_QUESTION_PARSE_FAILED|

---

### 15.2 解析失败处理

如果 AI 返回：

```json
{
  "data": []
}
```

而不是：

```json
{
  "questions": []
}
```

应该抛出：

```text
INTERVIEW_QUESTION_PARSE_FAILED
```

前端展示：

```text
面试题生成失败，请稍后重试
```

---

## 十六、单元测试设计

### 16.1 InterviewQuestionPromptBuilderTest

测试目标：

1. Prompt 包含简历内容
    
2. Prompt 包含岗位 JD
    
3. Prompt 包含分析短板
    
4. Prompt 包含题目数量
    
5. Prompt 明确要求只返回 JSON
    
6. Prompt 包含分类和难度要求
    

---

### 16.2 InterviewQuestionParserTest

测试目标：

1. 合法 JSON 可以解析
    
2. Markdown JSON 代码块可以解析
    
3. JSON 前后有文字可以解析
    
4. 缺少 questions 时抛异常
    
5. questions 为空数组时按业务规则处理
    
6. question 缺少必要字段时抛异常或补默认值
    

---

### 16.3 InterviewQuestionServiceImplTest

测试目标：

1. 正常生成面试题
    
2. 简历不存在时失败
    
3. 岗位不存在时失败
    
4. 分析报告不存在时失败
    
5. 查询别人记录时返回无权限
    
6. AI 返回空内容时抛 AI_RESPONSE_EMPTY
    
7. AI 返回非法 JSON 时抛 AI_RESPONSE_PARSE_FAILED
    
8. AI 返回缺少 questions 时抛 INTERVIEW_QUESTION_PARSE_FAILED
    
9. 重新生成时复用原上下文
    
10. 删除时只逻辑删除自己的记录
    

---

### 16.4 MockAiClient 面试题场景测试

如果 `32` 阶段已有 MockAiClientTest，本阶段补充：

1. 面试题 prompt 返回 questions
    
2. questions 中包含 category
    
3. questions 中包含 difficulty
    
4. questions 中包含 answer
    
5. questions 中包含 followUps
    

---

## 十七、集成测试设计

### 17.1 InterviewQuestionIntegrationTest

测试流程：

```text
1. 使用测试用户登录
2. 创建测试简历
3. 创建测试岗位
4. 创建或准备分析报告
5. 调用 POST /api/interview-questions/generate
6. 断言返回 200
7. 断言返回 recordId
8. 断言 questions 数量正确
9. 调用 GET /api/interview-questions
10. 断言列表包含刚生成的记录
11. 调用 GET /api/interview-questions/{id}
12. 断言详情包含题目、答案、追问
```

---

### 17.2 权限集成测试

|场景|预期|
|---|---|
|未登录生成面试题|401|
|普通用户生成自己的面试题|200|
|普通用户查询自己的面试题|200|
|普通用户查询别人的面试题|403 或 404|
|普通用户删除别人的面试题|403 或 404|
|无 create 权限生成面试题|403|
|无 read 权限查询面试题|403|

---

### 17.3 Mock AI 集成测试

测试目标：

```text
在 mock profile 下调用真实接口，确保面试题生成链路稳定。
```

断言：

1. 返回 questions
    
2. questions 数量大于 0
    
3. 第一题包含 category
    
4. 第一题包含 difficulty
    
5. 第一题包含 answer
    
6. 第一题包含 followUps
    

---

## 十八、前端测试设计

当前如果没有 Vitest / Playwright，可以先做手动测试清单。

### 18.1 手动测试清单

|测试项|预期|
|---|---|
|进入面试题生成页|正常显示|
|不选择简历直接生成|提示请选择简历|
|不选择岗位直接生成|提示请选择岗位|
|选择简历和岗位后生成|生成成功|
|题目分类正常显示|正常|
|难度标签正常显示|正常|
|参考答案正常展示|正常|
|追问问题正常展示|正常|
|重新生成|成功|
|删除记录|成功|
|普通用户无法查看别人记录|无权限|

---

## 十九、本地开发方式

本阶段继续使用本地开发模式。

|模块|方式|
|---|---|
|后端|IDEA 或 `.\gradlew.bat bootRun`|
|前端|`npm run dev`|
|MySQL|本机 MySQL|
|Redis|本机 Redis|
|Docker|不参与本阶段开发|

### 19.1 后端测试

```powershell
cd backend/intern-pilot-backend
.\gradlew.bat test --no-daemon
```

通过标准：

```text
BUILD SUCCESSFUL
```

### 19.2 前端构建

```powershell
cd frontend/intern-pilot-frontend
npm run build
```

通过标准：

```text
vite build 成功
```

---

## 二十、开发任务拆分

### 20.1 后端任务

|编号|任务|
|---|---|
|IQ-BE-01|检查现有面试题实体和表结构|
|IQ-BE-02|完善 InterviewQuestionGenerateRequest|
|IQ-BE-03|新增或完善 InterviewQuestionItemDTO|
|IQ-BE-04|新增 InterviewQuestionPromptBuilder|
|IQ-BE-05|新增 InterviewQuestionParser|
|IQ-BE-06|生成逻辑接入简历、岗位、分析报告上下文|
|IQ-BE-07|校验 resumeId / jobId / analysisReportId 用户归属|
|IQ-BE-08|保存结构化 questions JSON|
|IQ-BE-09|支持重新生成|
|IQ-BE-10|支持逻辑删除|
|IQ-BE-11|补充权限注解|
|IQ-BE-12|补充单元测试|
|IQ-BE-13|补充集成测试|

---

### 20.2 前端任务

|编号|任务|
|---|---|
|IQ-FE-01|完善面试题生成表单|
|IQ-FE-02|支持选择简历|
|IQ-FE-03|支持选择岗位|
|IQ-FE-04|支持选择题目数量|
|IQ-FE-05|支持选择分类|
|IQ-FE-06|支持选择难度|
|IQ-FE-07|详情页展示参考答案|
|IQ-FE-08|详情页展示追问问题|
|IQ-FE-09|详情页展示关键词|
|IQ-FE-10|支持重新生成|
|IQ-FE-11|支持删除|
|IQ-FE-12|前端构建通过|

---

### 20.3 测试任务

|编号|任务|
|---|---|
|IQ-TEST-01|PromptBuilder 单元测试|
|IQ-TEST-02|Parser 单元测试|
|IQ-TEST-03|Service 单元测试|
|IQ-TEST-04|MockAiClient 面试题场景测试|
|IQ-TEST-05|API 集成测试|
|IQ-TEST-06|权限集成测试|
|IQ-TEST-07|前端手动测试|
|IQ-TEST-08|后端全量测试|
|IQ-TEST-09|前端构建测试|

---

## 二十一、开发优先级

### P0：必须完成

1. 生成题目返回结构化 JSON
    
2. 每道题包含分类、难度、问题、答案、追问
    
3. 生成时结合简历和岗位
    
4. 校验资源归属
    
5. 保存生成记录
    
6. 查询列表和详情正常
    
7. Mock 模式稳定返回面试题 JSON
    
8. 后端测试通过
    
9. 前端构建通过
    

### P1：重要增强

1. 结合 AI 分析短板生成题目
    
2. 支持重新生成
    
3. 支持逻辑删除
    
4. 前端支持题目分类和难度筛选
    
5. 补充权限集成测试
    
6. README 更新项目亮点
    

### P2：后续优化

1. 收藏题目
    
2. 刷题记录
    
3. 用户回答与 AI 评分
    
4. 多轮追问
    
5. 面试模式
    
6. 导出面试题
    
7. 题库沉淀
    
8. 题目推荐算法
    

---

## 二十二、分支设计

本阶段建议创建：

```text
feature/interview-question-enhancement
```

分支来源：

```text
dev
```

创建命令：

```bash
git checkout dev
git pull origin dev
git checkout -b feature/interview-question-enhancement
```

提交设计文档：

```bash
git add docs/33-interview-question-enhancement-test-design.md
git commit -m "新增 AI 面试题生成增强与测试设计文档"
```

实现完成后提交：

```bash
git add .
git commit -m "增强 AI 面试题生成与测试覆盖"
```

合并路径：

```text
feature/interview-question-enhancement
  ↓
dev
  ↓
main
```

---

## 二十三、验收标准

本阶段完成后，需要满足：

1. 可以基于简历和岗位生成面试题
    
2. 可以基于 AI 分析短板生成面试题
    
3. 每道题包含分类、难度、问题、参考答案、追问问题
    
4. 面试题记录可以保存
    
5. 用户可以查询自己的面试题列表
    
6. 用户可以查看自己的面试题详情
    
7. 用户不能查看别人的面试题
    
8. 用户可以重新生成面试题
    
9. 用户可以删除自己的面试题记录
    
10. Mock AI 面试题返回稳定
    
11. AI 返回非法内容时有明确错误
    
12. 后端单元测试覆盖 PromptBuilder、Parser、Service
    
13. 后端集成测试覆盖生成、查询、权限
    
14. 前端页面可以正常展示题目、答案、追问
    
15. 后端 `.\gradlew.bat test --no-daemon` 通过
    
16. 前端 `npm run build` 通过
    
17. README 已更新功能亮点和运行说明
    

---

## 二十四、面试表达方式

本阶段可以在面试中这样介绍：

> 我在 InternPilot 中增强了 AI 面试题生成模块。系统可以根据用户简历、目标岗位 JD 和 AI 匹配分析中的短板，生成结构化面试题。每道题包含题目分类、难度、参考答案、追问问题和关键词。后端会校验简历、岗位和分析报告是否属于当前用户，防止越权访问。同时，我为 Prompt 构建、AI 返回解析、生成服务和接口权限补充了单元测试和集成测试，保证面试题生成链路稳定。

可以强调的项目亮点：

1. 基于简历和岗位的个性化面试题生成
    
2. 基于 AI 分析短板的补强题目
    
3. 结构化 JSON 输出
    
4. 题目分类和难度分级
    
5. 参考答案和追问问题
    
6. 资源归属校验
    
7. Mock AI 稳定演示
    
8. 单元测试和集成测试覆盖
    

---

## 二十五、总结

本阶段的核心是把 AI 面试题模块从简单生成升级为结构化、个性化、可测试的业务模块。

完成本阶段后，InternPilot 的 AI 能力链路将更加完整：

```text
简历上传
  ↓
岗位 JD 管理
  ↓
AI 匹配分析
  ↓
AI 分析进度推送
  ↓
AI 分析缓存与 Mock 增强
  ↓
AI 个性化面试题生成
```

这会显著增强项目的业务闭环和面试展示价值。

