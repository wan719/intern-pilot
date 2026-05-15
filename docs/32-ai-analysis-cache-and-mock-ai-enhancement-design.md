# InternPilot AI 分析缓存与 Mock AI 增强设计文档

## 一、文档目的

本文档用于设计 InternPilot 项目中 AI 分析链路的缓存机制、Mock AI 模式、RAG 异常处理和 AI 调用失败处理的增强方案。

当前项目已经完成了：

1. 用户认证与 RBAC 权限系统
2. AI 简历匹配分析
3. WebSocket AI 分析进度推送
4. Redis 任务状态保存
5. 前端刷新后任务恢复
6. RAG 岗位知识库基础能力
7. AI 面试题生成基础能力
8. 简历版本管理基础能力

但是目前 AI 调用链路仍然存在一些工程稳定性问题，例如：

1. MockAiClient 返回内容过于固定
2. 不同 AI 场景共用同一个 Mock 返回，容易导致格式错误
3. AI 分析缓存 key 不够精确
4. 简历或岗位更新后可能命中旧缓存
5. RAG 上下文构建失败时日志不清晰
6. AI 调用失败时异常提示不统一
7. AI 调用链路测试覆盖不足

本阶段目标是先打稳 AI 功能底座，再继续扩展面试题生成、简历优化等业务功能。

---

## 二、当前问题分析

### 2.1 Mock AI 返回内容过于固定

当前项目中，MockAiClient 可能固定返回一份简历匹配分析 JSON。

这会导致一个问题：

```text
AI 简历匹配分析：需要返回匹配分析 JSON
AI 简历优化：需要返回优化后的简历文本
AI 面试题生成：需要返回面试题 JSON
````

如果三类业务都调用同一个 `aiClient.chat(prompt)`，而 MockAiClient 永远返回匹配分析 JSON，那么其他功能就可能出错。

例如：

```text
简历优化功能调用 AI
→ MockAiClient 返回匹配分析 JSON
→ 系统把 JSON 当成优化后的简历内容保存
→ 数据格式错误
```

或者：

```text
面试题生成功能调用 AI
→ MockAiClient 返回简历分析 JSON
→ 面试题解析失败
→ 前端展示异常
```

---

### 2.2 AI 分析缓存 key 不够精确

当前 AI 分析缓存可能只使用：

```text
userId + resumeId + resumeVersionId + jobId
```

作为缓存 key。

这个设计存在隐患：

```text
用户修改了简历版本内容
岗位 JD 被修改
RAG 知识库内容发生变化
AI Prompt 模板发生变化
```

这些情况下，如果缓存 key 没有变化，就可能返回旧的 AI 分析结果。

例如：

```text
用户修改了岗位 JD
→ 再次点击 AI 分析
→ 命中旧缓存
→ 页面显示的分析结果仍然基于旧 JD
```

这会影响功能可信度。

---

### 2.3 RAG 异常被静默吞掉

当前 RAG 构建上下文时，如果出现异常，可能只是返回 null 或空字符串。

问题是：

```text
用户看不到 RAG 是否生效
开发者日志中也看不到失败原因
AI 分析结果可能缺少知识库上下文
排查问题困难
```

正确做法应该是：

```text
RAG 失败不阻断主 AI 分析
但必须记录 warn 日志
必要时在分析结果中标记是否使用 RAG
```

---

### 2.4 AI 调用失败提示不统一

AI 调用可能失败，例如：

1. API Key 错误
    
2. AI 服务超时
    
3. 网络异常
    
4. 返回内容格式不合法
    
5. JSON 解析失败
    
6. AI 返回空内容
    

如果异常没有统一处理，前端可能只看到：

```text
系统异常
500
null
```

用户体验不好。

应该统一为：

```text
AI_ANALYSIS_FAILED
AI_RESPONSE_PARSE_FAILED
AI_SERVICE_TIMEOUT
AI_SERVICE_UNAVAILABLE
```

并给前端返回可理解的错误信息。

---

## 三、本阶段建设目标

### 3.1 总体目标

本阶段目标是：

> 增强 InternPilot 的 AI 调用底座，使 Mock 模式更稳定，缓存机制更准确，RAG 异常更可观测，AI 调用失败更可控，并补充对应单元测试和集成测试。

---

### 3.2 具体目标

1. MockAiClient 支持按业务场景返回不同内容
    
2. AI 分析缓存 key 加入内容版本因素
    
3. 简历或岗位更新后不会错误命中旧缓存
    
4. RAG 上下文构建失败时记录日志
    
5. AI 调用失败时返回统一错误信息
    
6. AI 返回内容解析失败时有明确异常
    
7. 补充 MockAiClient 单元测试
    
8. 补充缓存 key 生成测试
    
9. 补充 AI 分析服务测试
    
10. 补充 RAG 异常兜底测试
    
11. 保持本地开发方式，不依赖 Docker
    

---

## 四、本阶段不做什么

本阶段不继续扩展新业务功能。

暂时不做：

1. 面试题收藏
    
2. 面试题刷题记录
    
3. 面试题难度自适应
    
4. 简历优化历史对比增强
    
5. 真实向量数据库替换
    
6. 多模型路由
    
7. AI 调用计费统计
    
8. AI 请求队列
    
9. AI 并发限流
    
10. WebSocket 用户级队列升级
    

这些属于后续阶段。

本阶段只做 AI 链路稳定性增强。

---

## 五、AI 调用场景设计

### 5.1 当前 AI 业务场景

InternPilot 当前至少有以下 AI 调用场景：

|场景|说明|期望返回|
|---|---|---|
|RESUME_JOB_ANALYSIS|简历与岗位匹配分析|结构化 JSON|
|RESUME_OPTIMIZATION|简历优化|优化后的简历文本或结构化 JSON|
|INTERVIEW_QUESTION_GENERATION|面试题生成|面试题 JSON|
|JOB_RECOMMENDATION|岗位推荐|推荐结果 JSON|
|RAG_QA|RAG 知识库问答，可选|问答文本或 JSON|

---

### 5.2 新增 AI 场景枚举

建议新增：

```java
public enum AiScenarioEnum {

    RESUME_JOB_ANALYSIS("RESUME_JOB_ANALYSIS", "简历岗位匹配分析"),

    RESUME_OPTIMIZATION("RESUME_OPTIMIZATION", "简历优化"),

    INTERVIEW_QUESTION_GENERATION("INTERVIEW_QUESTION_GENERATION", "面试题生成"),

    JOB_RECOMMENDATION("JOB_RECOMMENDATION", "岗位推荐"),

    RAG_QA("RAG_QA", "RAG知识库问答"),

    UNKNOWN("UNKNOWN", "未知场景");

    private final String code;

    private final String description;
}
```

---

### 5.3 场景识别方式

有两种方案。

#### 方案一：通过 Prompt 内容识别

MockAiClient 根据 prompt 中的关键词判断业务场景。

示例：

```text
包含“简历匹配分析” → RESUME_JOB_ANALYSIS
包含“优化简历” → RESUME_OPTIMIZATION
包含“面试题” → INTERVIEW_QUESTION_GENERATION
包含“岗位推荐” → JOB_RECOMMENDATION
```

优点：

```text
改动小
不需要改 AiClient 接口
适合当前最小实现
```

缺点：

```text
依赖 prompt 文案
prompt 改动后可能识别失败
```

---

#### 方案二：AiClient 显式传入场景

修改接口：

```java
String chat(AiScenarioEnum scenario, String prompt);
```

优点：

```text
类型明确
不依赖 prompt 关键词
后续扩展更规范
```

缺点：

```text
需要修改所有调用点
改动比方案一大
```

---

### 5.4 当前阶段推荐方案

当前阶段建议采用：

```text
短期：方案一，通过 Prompt 关键词识别
后续：方案二，升级 AiClient 接口显式传入 AiScenarioEnum
```

原因：

```text
当前目标是最小增强
不要大范围重构
先保证 Mock 模式不会返回错误格式
```

---

## 六、MockAiClient 增强设计

### 6.1 设计目标

MockAiClient 需要支持不同业务场景返回不同内容。

目标：

```text
简历匹配分析 → 返回匹配分析 JSON
简历优化 → 返回优化后的简历文本
面试题生成 → 返回面试题 JSON
岗位推荐 → 返回岗位推荐 JSON
未知场景 → 返回通用 AI 文本
```

---

### 6.2 MockAiClient 伪代码

```java
@Component
@Profile("mock")
public class MockAiClient implements AiClient {

    @Override
    public String chat(String prompt) {
        AiScenarioEnum scenario = detectScenario(prompt);

        return switch (scenario) {
            case RESUME_JOB_ANALYSIS -> mockResumeJobAnalysis();
            case RESUME_OPTIMIZATION -> mockResumeOptimization();
            case INTERVIEW_QUESTION_GENERATION -> mockInterviewQuestions();
            case JOB_RECOMMENDATION -> mockJobRecommendation();
            case RAG_QA -> mockRagQa();
            default -> mockDefaultResponse();
        };
    }

    private AiScenarioEnum detectScenario(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return AiScenarioEnum.UNKNOWN;
        }

        if (prompt.contains("简历匹配") || prompt.contains("匹配分析") || prompt.contains("matchScore")) {
            return AiScenarioEnum.RESUME_JOB_ANALYSIS;
        }

        if (prompt.contains("优化简历") || prompt.contains("简历优化")) {
            return AiScenarioEnum.RESUME_OPTIMIZATION;
        }

        if (prompt.contains("面试题") || prompt.contains("interview")) {
            return AiScenarioEnum.INTERVIEW_QUESTION_GENERATION;
        }

        if (prompt.contains("岗位推荐") || prompt.contains("推荐岗位")) {
            return AiScenarioEnum.JOB_RECOMMENDATION;
        }

        if (prompt.contains("知识库") || prompt.contains("RAG")) {
            return AiScenarioEnum.RAG_QA;
        }

        return AiScenarioEnum.UNKNOWN;
    }
}
```

---

### 6.3 简历匹配分析 Mock 返回

```json
{
  "matchScore": 82,
  "summary": "该候选人与岗位整体匹配度较高，具备 Java 后端、Spring Boot、MySQL 和项目实践基础。",
  "strengths": [
    "具备 Spring Boot 项目开发经验",
    "熟悉 MySQL、Redis、JWT 等后端常用技术",
    "有完整项目从认证到部署的实践经历"
  ],
  "weaknesses": [
    "企业级高并发经验不足",
    "缺少真实线上项目性能优化经历",
    "对分布式系统理解仍需加强"
  ],
  "suggestions": [
    "补充 Redis 缓存、接口限流和性能优化相关实践",
    "完善项目 README 和部署演示说明",
    "准备 Spring Security、MyBatis、事务等面试题"
  ]
}
```

---

### 6.4 简历优化 Mock 返回

可以返回纯文本：

```text
优化后的简历内容：

项目名称：InternPilot 面向大学生的 AI 实习投递与简历优化平台

项目描述：
基于 Spring Boot、Spring Security、MyBatis、MySQL、Redis、Vue3 和 Docker 构建的前后端分离 AI 应用平台，支持简历上传解析、岗位 JD 管理、AI 匹配分析、WebSocket 实时进度展示、RAG 岗位知识库和管理员权限系统。

个人职责：
1. 负责用户认证与 JWT 鉴权模块设计。
2. 负责 RBAC 权限系统设计，实现用户、角色、权限动态管理。
3. 负责 AI 简历匹配分析链路，实现 Redis 缓存和 WebSocket 进度推送。
4. 负责前后端联调、接口测试和 Docker Compose 本地部署。
```

---

### 6.5 面试题生成 Mock 返回

```json
{
  "questions": [
    {
      "category": "Java基础",
      "difficulty": "基础",
      "question": "HashMap 的底层数据结构是什么？",
      "answer": "JDK 8 中 HashMap 底层由数组、链表和红黑树组成。当链表长度超过阈值且数组容量足够时，链表会转换为红黑树以提高查询效率。"
    },
    {
      "category": "Spring Boot",
      "difficulty": "中等",
      "question": "Spring Boot 自动配置的原理是什么？",
      "answer": "Spring Boot 通过自动配置类、条件注解和配置元数据，在应用启动时根据类路径、配置文件和 Bean 条件自动装配所需组件。"
    },
    {
      "category": "项目经验",
      "difficulty": "中等",
      "question": "你的项目中为什么要使用 WebSocket 展示 AI 分析进度？",
      "answer": "因为 AI 分析属于耗时任务，如果只使用同步接口，用户体验较差。WebSocket 可以在任务执行过程中实时推送阶段进度，提升交互体验。"
    }
  ]
}
```

---

## 七、AI 分析缓存增强设计

### 7.1 当前缓存问题

如果缓存 key 只包含：

```text
userId
resumeId
resumeVersionId
jobId
```

可能出现旧缓存问题。

例如：

```text
resumeVersionId 不变，但 content 被更新
jobId 不变，但 jdContent 被更新
RAG 知识库更新，但缓存仍然命中旧分析
Prompt 模板更新，但缓存仍然使用旧结果
```

---

### 7.2 缓存 key 设计目标

缓存 key 需要体现：

1. 用户 ID
    
2. 简历 ID
    
3. 简历版本 ID
    
4. 岗位 ID
    
5. 简历版本更新时间
    
6. 岗位更新时间
    
7. RAG 是否启用
    
8. Prompt 模板版本
    
9. AI 模型名称，可选
    

---

### 7.3 推荐缓存 key

建议格式：

```text
ai:analysis:result:{userId}:{resumeId}:{resumeVersionId}:{resumeUpdatedAt}:{jobId}:{jobUpdatedAt}:{ragEnabled}:{promptVersion}
```

示例：

```text
ai:analysis:result:1:10:3:20260514103000:8:20260514104000:true:v1
```

---

### 7.4 更安全的 Hash Key 方案

如果 key 太长，可以先拼接原始 key，再做 hash。

```java
String rawKey = userId + ":" +
        resumeId + ":" +
        resumeVersionId + ":" +
        resumeUpdatedAt + ":" +
        jobId + ":" +
        jobUpdatedAt + ":" +
        ragEnabled + ":" +
        promptVersion;

String hash = DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));

String cacheKey = "ai:analysis:result:" + hash;
```

优点：

```text
Redis key 更短
字段变化仍然可以影响缓存
后续扩展字段方便
```

---

### 7.5 当前阶段推荐方案

推荐当前使用 Hash Key 方案。

因为它：

1. 避免 Redis key 过长
    
2. 可读性通过日志补充
    
3. 变更因素可控
    
4. 后续容易加入模型版本、RAG 版本等字段
    

---

### 7.6 缓存 TTL

建议：

```text
AI 分析结果缓存：24 小时
```

原因：

```text
AI 分析结果不是永久不变
简历和岗位可能会改
24 小时可以减少重复调用
同时避免长期旧缓存污染
```

---

### 7.7 缓存命中策略

流程：

```text
用户发起 AI 分析
  ↓
根据用户、简历版本、岗位、更新时间、RAG配置、Prompt版本生成缓存 key
  ↓
查询 Redis 是否存在缓存结果
  ↓
如果存在，直接返回缓存分析报告
  ↓
如果不存在，调用 AI
  ↓
AI 返回成功后写入缓存
```

---

### 7.8 缓存命中时是否推送 WebSocket

需要推送。

即使命中缓存，也应该给前端完整反馈：

```text
PENDING 0%
BUILDING_CONTEXT 35%
GENERATING_REPORT 85%
COMPLETED 100%
```

或者直接：

```text
PENDING 0%
COMPLETED 100%
```

当前建议：

```text
缓存命中时直接推送 COMPLETED，并提示“已命中缓存分析结果”
```

这样用户体验更快。

---

## 八、Prompt 模板版本设计

### 8.1 为什么需要 Prompt 版本

AI 分析结果依赖 Prompt。

如果你修改了 Prompt 模板，例如：

```text
增加分析维度
改变 JSON 格式
增加 RAG 上下文
调整评分标准
```

即使简历和岗位没有变化，旧缓存也不应该继续使用。

因此缓存 key 中需要包含：

```text
promptVersion
```

---

### 8.2 Prompt 版本常量

建议在分析服务中定义：

```java
private static final String ANALYSIS_PROMPT_VERSION = "v1";
```

后续 Prompt 改动时升级：

```java
private static final String ANALYSIS_PROMPT_VERSION = "v2";
```

---

## 九、RAG 异常处理增强设计

### 9.1 当前问题

当前 RAG 上下文构建异常时，可能直接忽略。

这会导致：

```text
RAG 实际没有生效
开发者不知道失败原因
AI 分析质量下降
问题排查困难
```

---

### 9.2 设计原则

RAG 是增强能力，不应该阻断主 AI 分析。

因此：

```text
RAG 成功 → 带上 RAG 上下文
RAG 失败 → 记录日志，继续执行普通 AI 分析
```

---

### 9.3 推荐实现

```java
private String buildRagContextSafely(Long userId, JobDescription job) {
    try {
        return ragService.buildContext(userId, job);
    } catch (Exception e) {
        log.warn("构建 RAG 上下文失败，userId={}, jobId={}", userId, job.getId(), e);
        return "";
    }
}
```

---

### 9.4 是否在前端展示 RAG 失败

当前阶段不建议直接提示用户：

```text
RAG 构建失败
```

因为用户可能不理解。

更推荐：

```text
后端日志记录
AI 分析继续完成
```

后续可以在管理员日志中展示。

---

## 十、AI 调用异常设计

### 10.1 异常分类

建议定义统一异常类型：

```java
public class AiServiceException extends RuntimeException {

    private final String errorCode;

    public AiServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

---

### 10.2 错误码设计

|错误码|说明|
|---|---|
|AI_SERVICE_UNAVAILABLE|AI 服务不可用|
|AI_SERVICE_TIMEOUT|AI 服务调用超时|
|AI_RESPONSE_EMPTY|AI 返回内容为空|
|AI_RESPONSE_PARSE_FAILED|AI 返回内容解析失败|
|AI_ANALYSIS_FAILED|AI 分析失败|
|AI_MOCK_SCENARIO_UNKNOWN|Mock AI 未识别场景|

---

### 10.3 前端错误文案

|错误码|前端文案|
|---|---|
|AI_SERVICE_UNAVAILABLE|AI 服务暂时不可用，请稍后重试|
|AI_SERVICE_TIMEOUT|AI 响应超时，请稍后重试|
|AI_RESPONSE_EMPTY|AI 返回内容为空，请重新尝试|
|AI_RESPONSE_PARSE_FAILED|AI 返回格式异常，请稍后重试|
|AI_ANALYSIS_FAILED|AI 分析失败，请稍后重试|

---

## 十一、AI 返回内容解析增强

### 11.1 当前问题

AI 返回 JSON 时，可能出现：

1. 包裹 Markdown 代码块
    
2. JSON 前后有解释文字
    
3. 字段缺失
    
4. 字段类型错误
    
5. 返回空字符串
    

例如：

````text
```json
{
  "matchScore": 82
}
````

````

或者：

```text
下面是分析结果：
{
  "matchScore": 82
}
````

---

### 11.2 解析工具设计

建议新增：

```java
public class AiResponseParser {

    public static String extractJson(String response) {
        // 去除 markdown json code block
        // 截取第一个 { 到最后一个 }
        // 返回 JSON 字符串
    }
}
```

---

### 11.3 解析失败处理

如果无法解析，应抛出：

```java
throw new AiServiceException(
    "AI_RESPONSE_PARSE_FAILED",
    "AI 返回格式异常，无法解析为 JSON"
);
```

---

## 十二、后端改动清单

### 12.1 建议新增文件

|文件|作用|
|---|---|
|`AiScenarioEnum.java`|AI 调用场景枚举|
|`AiServiceException.java`|AI 统一异常|
|`AiResponseParser.java`|AI 返回内容解析工具|
|`AiAnalysisCacheKeyBuilder.java`|AI 分析缓存 key 构造器|

---

### 12.2 建议修改文件

|文件|修改内容|
|---|---|
|`MockAiClient.java`|按业务场景返回不同 Mock 内容|
|`AnalysisServiceImpl.java`|使用新版缓存 key、RAG 异常日志、AI 解析异常|
|`InterviewQuestionServiceImpl.java`|适配 Mock 面试题 JSON|
|`ResumeVersionServiceImpl.java`|适配 Mock 简历优化文本|
|`JobRecommendationServiceImpl.java`|适配 Mock 岗位推荐 JSON|
|`AnalysisTaskServiceImpl.java`|缓存命中时推送 COMPLETED|
|`GlobalExceptionHandler.java`|处理 AiServiceException|

---

## 十三、后端实现优先级

### P0：必须完成

1. MockAiClient 按场景返回不同内容
    
2. 分析缓存 key 加入更新时间和 promptVersion
    
3. RAG 失败记录 warn 日志
    
4. AI 返回空内容时抛明确异常
    
5. JSON 解析失败时抛明确异常
    
6. 补 MockAiClient 单元测试
    
7. 补缓存 key 构造测试
    
8. 后端全量测试通过
    

---

### P1：重要增强

1. 新增 AiScenarioEnum
    
2. 新增 AiServiceException
    
3. 新增 AiResponseParser
    
4. GlobalExceptionHandler 处理 AI 异常
    
5. 缓存命中时推送 COMPLETED
    
6. 面试题生成适配 Mock 返回
    
7. 简历优化适配 Mock 返回
    
8. RAG 异常兜底测试
    

---

### P2：后续优化

1. AiClient 显式传入 AiScenarioEnum
    
2. Prompt 模板统一管理
    
3. Prompt 版本号配置化
    
4. AI 调用耗时统计
    
5. AI 调用日志入库
    
6. AI 失败重试机制
    
7. AI 请求限流
    
8. 多模型路由
    

---

## 十四、单元测试设计

### 14.1 MockAiClientTest

测试目标：

1. 简历匹配分析 prompt 返回匹配分析 JSON
    
2. 简历优化 prompt 返回优化简历文本
    
3. 面试题 prompt 返回面试题 JSON
    
4. 岗位推荐 prompt 返回推荐 JSON
    
5. 未知 prompt 返回默认文本
    
6. 空 prompt 不报错
    

测试用例：

|用例|输入|预期|
|---|---|---|
|chat_resumeJobAnalysisPrompt_returnAnalysisJson|包含“匹配分析”|返回 matchScore|
|chat_resumeOptimizationPrompt_returnResumeText|包含“优化简历”|返回优化后的简历|
|chat_interviewQuestionPrompt_returnQuestionJson|包含“面试题”|返回 questions|
|chat_jobRecommendationPrompt_returnRecommendationJson|包含“岗位推荐”|返回 recommendations|
|chat_unknownPrompt_returnDefaultText|随机文本|返回默认内容|
|chat_blankPrompt_returnDefaultText|空字符串|返回默认内容|

---

### 14.2 AiAnalysisCacheKeyBuilderTest

测试目标：

1. 相同输入生成相同 key
    
2. 简历更新时间变化时 key 变化
    
3. 岗位更新时间变化时 key 变化
    
4. promptVersion 变化时 key 变化
    
5. ragEnabled 变化时 key 变化
    
6. key 以 `ai:analysis:result:` 开头
    

---

### 14.3 AiResponseParserTest

测试目标：

1. 纯 JSON 可以解析
    
2. Markdown JSON 代码块可以解析
    
3. JSON 前后有文字可以解析
    
4. 空字符串抛异常
    
5. 无 JSON 内容抛异常
    
6. 非法 JSON 抛异常
    

---

### 14.4 AnalysisServiceImplTest

测试目标：

1. 缓存未命中时调用 AI
    
2. 缓存命中时不调用 AI
    
3. 简历版本更新时间变化后不命中旧缓存
    
4. 岗位更新时间变化后不命中旧缓存
    
5. RAG 构建失败时仍继续 AI 分析
    
6. AI 返回空内容时抛 AiServiceException
    
7. AI 返回非法 JSON 时抛 AiServiceException
    

---

### 14.5 GlobalExceptionHandlerTest

测试目标：

1. AiServiceException 返回统一错误结构
    
2. AI_RESPONSE_PARSE_FAILED 返回可读错误信息
    
3. AI_SERVICE_TIMEOUT 返回可读错误信息
    

---

## 十五、集成测试设计

### 15.1 AI 分析缓存集成测试

测试流程：

```text
1. 登录测试用户
2. 创建测试简历
3. 创建测试岗位
4. 第一次发起 AI 分析
5. 断言调用 AI 并生成结果
6. 第二次使用相同参数发起 AI 分析
7. 断言命中缓存
8. 修改岗位更新时间或内容
9. 再次发起 AI 分析
10. 断言不再命中旧缓存
```

---

### 15.2 Mock AI 场景集成测试

测试流程：

```text
1. 使用 mock profile 启动测试
2. 调用简历匹配分析
3. 断言返回 matchScore
4. 调用面试题生成
5. 断言返回 questions
6. 调用简历优化
7. 断言返回优化简历文本
```

---

### 15.3 RAG 异常兜底集成测试

测试流程：

```text
1. Mock RAG 服务抛出异常
2. 发起 AI 分析
3. 断言主流程没有失败
4. 断言最终仍生成 AI 分析结果
5. 断言日志或状态中能观察到 RAG 失败
```

---

### 15.4 AI 异常处理集成测试

测试流程：

```text
1. Mock AI 返回空字符串
2. 发起 AI 分析
3. 断言返回 AI_RESPONSE_EMPTY
4. Mock AI 返回非法 JSON
5. 发起 AI 分析
6. 断言返回 AI_RESPONSE_PARSE_FAILED
```

---

## 十六、前端影响分析

### 16.1 前端需要关注的变化

本阶段主要是后端增强，前端改动较少。

可能涉及：

1. AI 分析失败时展示更明确错误信息
    
2. 缓存命中时展示“已使用缓存结果”
    
3. 面试题生成 Mock 返回格式更稳定
    
4. 简历优化 Mock 返回内容更合理
    

---

### 16.2 前端错误展示建议

如果后端返回：

```json
{
  "code": "AI_RESPONSE_PARSE_FAILED",
  "message": "AI 返回格式异常，请稍后重试"
}
```

前端可以展示：

```text
AI 返回格式异常，请稍后重试
```

不要直接展示后端异常堆栈。

---

## 十七、本地开发方式

本阶段继续使用本地开发模式。

|模块|方式|
|---|---|
|后端|IDEA 或 `.\gradlew.bat bootRun`|
|前端|`npm run dev`|
|MySQL|本机 MySQL|
|Redis|本机 Redis|
|Docker|不参与本阶段开发|

---

### 17.1 后端测试命令

```powershell
cd backend/intern-pilot-backend
.\gradlew.bat test --no-daemon
```

通过标准：

```text
BUILD SUCCESSFUL
```

---

### 17.2 前端构建命令

```powershell
cd frontend/intern-pilot-frontend
npm run build
```

通过标准：

```text
vite build 成功
```

---

## 十八、分支设计

### 18.1 分支名称

本阶段建议创建：

```text
feature/ai-analysis-cache-mock
```

---

### 18.2 分支来源

从 `dev` 创建：

```bash
git checkout dev
git pull origin dev
git checkout -b feature/ai-analysis-cache-mock
```

---

### 18.3 合并路径

```text
feature/ai-analysis-cache-mock
  ↓
dev
  ↓
main
```

---

### 18.4 提交建议

先提交设计文档：

```bash
git add docs/32-ai-analysis-cache-and-mock-ai-enhancement-design.md
git commit -m "新增 AI 分析缓存与 Mock AI 增强设计文档"
```

实现完成后提交：

```bash
git add .
git commit -m "增强 AI 分析缓存与 Mock AI 场景返回"
```

---

## 十九、验收标准

本阶段完成后，需要满足：

1. MockAiClient 能区分简历分析、简历优化、面试题生成、岗位推荐场景
    
2. 简历分析 Mock 返回合法分析 JSON
    
3. 面试题 Mock 返回合法题目 JSON
    
4. 简历优化 Mock 返回合理文本
    
5. AI 分析缓存 key 包含简历版本更新时间
    
6. AI 分析缓存 key 包含岗位更新时间
    
7. Prompt 版本变化会影响缓存 key
    
8. RAG 构建失败时记录 warn 日志
    
9. RAG 构建失败不阻断 AI 分析主流程
    
10. AI 返回空内容时有明确错误
    
11. AI 返回非法 JSON 时有明确错误
    
12. 缓存命中时不会重复调用 AI
    
13. 缓存命中时仍能推送分析完成状态
    
14. 单元测试覆盖 MockAiClient
    
15. 单元测试覆盖缓存 key 构造器
    
16. 单元测试覆盖 AI 返回解析工具
    
17. 后端 `.\gradlew.bat test --no-daemon` 通过
    
18. 前端 `npm run build` 通过
    

---

## 二十、开发任务拆分

### 20.1 后端任务

|编号|任务|
|---|---|
|AI-BE-01|新增 AiScenarioEnum|
|AI-BE-02|增强 MockAiClient 场景识别|
|AI-BE-03|MockAiClient 返回多场景 Mock 数据|
|AI-BE-04|新增 AiAnalysisCacheKeyBuilder|
|AI-BE-05|AnalysisServiceImpl 使用新版缓存 key|
|AI-BE-06|新增 Prompt Version 常量|
|AI-BE-07|RAG 构建异常增加 warn 日志|
|AI-BE-08|新增 AiServiceException|
|AI-BE-09|新增 AiResponseParser|
|AI-BE-10|GlobalExceptionHandler 处理 AI 异常|
|AI-BE-11|缓存命中时推送 COMPLETED|
|AI-BE-12|增加 MockAiClientTest|
|AI-BE-13|增加 AiAnalysisCacheKeyBuilderTest|
|AI-BE-14|增加 AiResponseParserTest|
|AI-BE-15|增加 AnalysisServiceImpl 相关测试|

---

### 20.2 前端任务

|编号|任务|
|---|---|
|AI-FE-01|检查 AI 分析失败错误展示|
|AI-FE-02|检查面试题 Mock 返回展示|
|AI-FE-03|检查简历优化 Mock 返回展示|
|AI-FE-04|前端构建验证|

---

### 20.3 测试任务

|编号|任务|
|---|---|
|AI-TEST-01|MockAiClient 多场景测试|
|AI-TEST-02|缓存 key 稳定性测试|
|AI-TEST-03|缓存 key 变更因素测试|
|AI-TEST-04|AI JSON 解析测试|
|AI-TEST-05|RAG 异常兜底测试|
|AI-TEST-06|AI 异常统一处理测试|
|AI-TEST-07|后端全量测试|
|AI-TEST-08|前端构建测试|

---

## 二十一、面试表达方式

本阶段可以在面试中这样介绍：

> 在 InternPilot 项目中，我对 AI 分析链路做了工程稳定性增强。首先，我改造了 MockAiClient，使其能够根据简历分析、简历优化、面试题生成、岗位推荐等不同场景返回不同格式的数据，保证本地演示和测试稳定。其次，我优化了 AI 分析缓存 key，将简历版本更新时间、岗位更新时间、RAG 开关和 Prompt 版本纳入缓存因素，避免用户修改内容后仍命中旧缓存。同时，我增强了 RAG 异常兜底处理，使知识库上下文构建失败时不会阻断主分析流程，并通过统一 AI 异常类型和返回解析工具提升了错误可观测性。最后，我补充了 Mock AI、缓存 key、AI 返回解析和分析服务的单元测试，保证 AI 链路可靠。

可以强调的项目亮点：

1. Mock AI 多场景适配
    
2. AI 缓存 key 版本化
    
3. Redis 缓存避免重复 AI 调用
    
4. Prompt Version 控制缓存失效
    
5. RAG 异常不阻断主流程
    
6. AI 返回解析工具
    
7. 统一 AI 异常处理
    
8. 单元测试覆盖 AI 底座能力
    

---

## 二十二、总结

本阶段的重点不是继续增加新 AI 功能，而是提升现有 AI 功能的稳定性。

完成本阶段后，InternPilot 的 AI 模块将从：

```text
能调用 AI
```

升级为：

```text
AI 调用稳定
Mock 演示可靠
缓存不会误命中旧数据
RAG 异常可观测
AI 解析失败可定位
测试覆盖更完整
```
