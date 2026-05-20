# InternPilot AI 模型路由与 Prompt 优化设计文档

## 1. 文档背景

InternPilot 当前已经完成主要功能开发、线上部署和 Release 发布，系统已经具备以下 AI 能力：

```text
1. AI 简历岗位匹配分析
2. 岗位推荐
3. AI 面试题生成
4. RAG 岗位知识库问答
5. 全局 AI 任务中心
6. AI 分析缓存
7. MockAiClient 测试能力
8. DeepSeekAiClient 真实调用能力
```

当前阶段项目已经不适合继续盲目增加大功能，更适合围绕 AI 核心能力继续增强。

本阶段目标不是简单“接入更多模型”，而是围绕：

```text
模型路由
Prompt 版本管理
Prompt 质量优化
AI 结果格式稳定
失败重试与降级
AI 调用日志增强
AI 结果质量评估
后续多模型扩展
```

对 InternPilot 的 AI 能力做一次工程化升级。

---

## 2. 当前问题分析

### 2.1 Prompt 分散问题

当前项目中已经有多个 AI 场景：

```text
简历岗位匹配分析
岗位推荐
面试题生成
RAG 问答
简历优化建议
反馈总结，后续可选
```

如果每个模块单独写 Prompt，容易出现：

```text
1. Prompt 分散在不同 Service 中，难以维护
2. Prompt 改动后无法追踪版本
3. 不同场景输出格式不统一
4. AI 返回内容不稳定，解析容易失败
5. 缓存 key 无法准确区分 Prompt 版本
6. 后续对比 Prompt 效果困难
```

---

### 2.2 模型选择不够灵活

当前线上主要使用 DeepSeek 真接口，Mock 只用于测试。

但不同 AI 场景复杂度不同：

```text
简单任务：
    岗位标签提取
    简单推荐理由
    简单面试题生成

复杂任务：
    简历岗位深度匹配分析
    RAG 复杂问答
    综合职业建议
    多维度报告生成
```

如果所有任务都使用同一个模型，会出现：

```text
1. 简单任务成本偏高
2. 复杂任务效果可能不足
3. 无法针对场景做最优配置
4. 模型故障时缺少降级策略
```

---

### 2.3 AI 结果不稳定问题

AI 返回内容可能出现：

```text
1. 没有严格按照 JSON 格式返回
2. 缺少字段
3. 多出解释性文字
4. 分数不在合理范围内
5. 中文表达不统一
6. 面试题数量不稳定
7. RAG 回答引用上下文不足
```

因此需要增强：

```text
Prompt 约束
结果解析
格式修复
兜底策略
质量评分
```

---

### 2.4 AI 调用可观测性不足

当前虽然已经有部分日志和缓存设计，但 AI 调用仍需要继续增强可观测性。

需要记录：

```text
1. 使用了哪个场景
2. 使用了哪个模型
3. 使用了哪个 Prompt 版本
4. Prompt hash
5. Response hash
6. 是否命中缓存
7. 调用耗时
8. token 用量，如果接口返回
9. 错误码
10. 重试次数
11. 降级情况
```

这样后续才能排查：

```text
为什么这次结果差？
为什么这次调用失败？
为什么缓存没命中？
为什么线上和本地结果不同？
```

---

## 3. 本阶段目标

本阶段目标是对 AI 调用链路做工程化优化。

核心目标：

```text
1. 建立统一 AI 场景枚举
2. 建立模型路由策略
3. 建立 Prompt 版本管理机制
4. 优化 AI 输出格式约束
5. 增强 AI 结果解析鲁棒性
6. 增加失败重试与降级策略
7. 增强 AI 调用日志
8. 优化缓存 key
9. 为后续多模型 Provider 预留扩展
```

---

## 4. 本阶段不做什么

为了控制范围，本阶段暂时不做：

```text
1. 不强制接入多个真实模型供应商
2. 不引入复杂 LangChain 框架
3. 不引入向量数据库大改造
4. 不改变当前 DeepSeek 线上主模型策略
5. 不把所有 AI 结果都人工标注训练
6. 不做模型微调
7. 不引入复杂计费系统
8. 不在前端暴露 API Key
```

本阶段重点是：

```text
先把现有 DeepSeek 用好，把 AI 调用链路做规范。
```

---

## 5. 总体设计方案

采用分阶段方案：

```text
第一阶段：Prompt 版本管理 + 场景路由 + 日志增强
第二阶段：模型路由 + 失败重试 + 结果质量评分
第三阶段：预留多模型 Provider 扩展
```

整体架构：

```text
业务 Service
  ↓
AiScenarioEnum 场景识别
  ↓
AiPromptTemplateResolver 选择 Prompt 模板
  ↓
AiModelRouter 选择模型
  ↓
AiClient 调用模型
  ↓
AiResponseParser 解析结果
  ↓
AiQualityEvaluator 质量评估
  ↓
缓存 / 日志 / 返回业务结果
```

---

## 6. AI 场景枚举设计

### 6.1 现有场景整理

建议统一维护 AI 场景枚举：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/scenario/AiScenarioEnum.java
```

建议场景：

```java
public enum AiScenarioEnum {

    RESUME_JOB_ANALYSIS(
        "RESUME_JOB_ANALYSIS",
        "简历岗位匹配分析",
        "根据简历和岗位信息生成匹配度分析报告"
    ),

    JOB_RECOMMENDATION(
        "JOB_RECOMMENDATION",
        "岗位推荐",
        "根据用户简历和岗位库生成推荐岗位"
    ),

    INTERVIEW_QUESTION_GENERATION(
        "INTERVIEW_QUESTION_GENERATION",
        "面试题生成",
        "根据简历和岗位生成面试题"
    ),

    INTERVIEW_QUESTION_REGENERATION(
        "INTERVIEW_QUESTION_REGENERATION",
        "面试题重新生成",
        "根据用户要求重新生成面试题"
    ),

    RAG_QA(
        "RAG_QA",
        "RAG 知识库问答",
        "基于岗位知识库上下文回答用户问题"
    ),

    RESUME_OPTIMIZATION(
        "RESUME_OPTIMIZATION",
        "简历优化建议",
        "根据简历内容生成优化建议"
    );

    private final String code;
    private final String name;
    private final String description;
}
```

---

### 6.2 场景作用

场景枚举用于统一控制：

```text
1. 使用哪个 Prompt 模板
2. 使用哪个模型
3. 使用哪个解析器
4. 是否允许缓存
5. 是否允许重试
6. 是否允许降级
7. 日志如何记录
```

---

## 7. 模型路由设计

### 7.1 路由目标

模型路由不是为了炫技，而是为了：

```text
1. 简单任务使用更快模型
2. 复杂任务使用更强模型
3. 测试环境使用 Mock
4. 生产环境使用 DeepSeek
5. 模型异常时支持 fallback
6. 后续接入其他 Provider 时业务层不改
```

---

### 7.2 模型配置设计

建议新增模型配置类：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/config/AiModelProperties.java
```

配置项：

```yaml
ai:
  provider: deepseek
  default-model: deepseek-v4-flash
  fallback-model: deepseek-v4-flash
  models:
    flash: deepseek-v4-flash
    pro: deepseek-v4-pro
  scenario-model:
    RESUME_JOB_ANALYSIS: deepseek-v4-pro
    JOB_RECOMMENDATION: deepseek-v4-flash
    INTERVIEW_QUESTION_GENERATION: deepseek-v4-flash
    INTERVIEW_QUESTION_REGENERATION: deepseek-v4-flash
    RAG_QA: deepseek-v4-pro
    RESUME_OPTIMIZATION: deepseek-v4-pro
  retry:
    enabled: true
    max-attempts: 2
  timeout:
    connect-timeout-ms: 10000
    read-timeout-ms: 60000
```

注意：

```text
模型名称不要在代码里写死。
实际模型名称以当前项目配置和 DeepSeek API 可用模型为准。
```

---

### 7.3 模型路由器

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/router/AiModelRouter.java
```

核心方法：

```java
public interface AiModelRouter {

    String route(AiScenarioEnum scenario);

    String fallback(AiScenarioEnum scenario);

    boolean allowFallback(AiScenarioEnum scenario);
}
```

实现类：

```text
DefaultAiModelRouter.java
```

示例逻辑：

```java
@Service
public class DefaultAiModelRouter implements AiModelRouter {

    private final AiModelProperties properties;

    @Override
    public String route(AiScenarioEnum scenario) {
        String model = properties.getScenarioModel().get(scenario.name());
        if (model != null && !model.isBlank()) {
            return model;
        }
        return properties.getDefaultModel();
    }

    @Override
    public String fallback(AiScenarioEnum scenario) {
        return properties.getFallbackModel();
    }

    @Override
    public boolean allowFallback(AiScenarioEnum scenario) {
        return true;
    }
}
```

---

## 8. Prompt 版本管理设计

### 8.1 为什么需要 Prompt 版本

Prompt 是 AI 功能的核心逻辑之一。Prompt 改了，输出结果就可能变化。

需要记录：

```text
1. 当前场景使用了哪个 Prompt
2. Prompt 是第几个版本
3. 缓存是否和 Prompt 版本绑定
4. 线上结果是否来自旧 Prompt
5. 后续能否做 Prompt A/B 对比
```

---

### 8.2 Prompt 版本命名规则

建议使用：

```text
场景名 + 版本号
```

例如：

```text
RESUME_JOB_ANALYSIS_v1
RESUME_JOB_ANALYSIS_v2
JOB_RECOMMENDATION_v1
INTERVIEW_QUESTION_GENERATION_v1
RAG_QA_v1
```

---

### 8.3 Prompt 模板文件方式

第一阶段建议先使用代码内模板类，不急着建数据库配置后台。

新增目录：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/prompt/template
```

新增类：

```text
AiPromptTemplate.java
AiPromptTemplateResolver.java
ResumeJobAnalysisPromptTemplateV2.java
JobRecommendationPromptTemplateV1.java
InterviewQuestionPromptTemplateV1.java
RagQaPromptTemplateV1.java
```

接口：

```java
public interface AiPromptTemplate {

    AiScenarioEnum scenario();

    String version();

    String systemPrompt();

    String buildUserPrompt(AiPromptContext context);

    AiOutputFormat outputFormat();
}
```

---

### 8.4 Prompt 上下文对象

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/prompt/AiPromptContext.java
```

建议字段：

```java
@Data
@Builder
public class AiPromptContext {

    private AiScenarioEnum scenario;

    private String userProfile;

    private String resumeContent;

    private String jobContent;

    private String ragContext;

    private String userQuestion;

    private String extraInstruction;

    private Map<String, Object> metadata;
}
```

注意：

```text
不要在日志中直接打印完整 resumeContent、jobContent、ragContext。
可以打印 hash、长度、是否为空。
```

---

## 9. Prompt 优化原则

### 9.1 通用系统 Prompt 要求

所有业务 Prompt 都应遵守：

```text
1. 必须使用简体中文回答
2. 不要编造不存在的信息
3. 不要输出与求职无关内容
4. 如果信息不足，要明确说明
5. 输出结构必须稳定
6. 分数必须在指定范围内
7. 字段名称必须固定
8. 不要在 JSON 外输出额外解释
```

---

### 9.2 简历岗位分析 Prompt 优化目标

输出应包含：

```text
1. 匹配总分
2. 技能匹配度
3. 经验匹配度
4. 学历/背景匹配度
5. 项目经历匹配度
6. 优势分析
7. 不足分析
8. 简历优化建议
9. 面试准备建议
10. 风险提示
```

建议输出 JSON：

```json
{
  "overallScore": 82,
  "skillScore": 85,
  "experienceScore": 78,
  "educationScore": 80,
  "projectScore": 88,
  "summary": "整体匹配度较高，适合投递该岗位。",
  "strengths": [
    "具备 Java 后端开发基础",
    "有 Spring Boot 项目经验"
  ],
  "weaknesses": [
    "缺少企业实习经历",
    "分布式系统经验较少"
  ],
  "resumeSuggestions": [
    "突出项目中的 RBAC、WebSocket、AI 接入经验",
    "补充接口性能优化和部署经验"
  ],
  "interviewSuggestions": [
    "准备 Spring Security 和 JWT 登录流程",
    "准备 MySQL 索引和 Redis 缓存相关问题"
  ],
  "riskTips": [
    "岗位可能要求更强的实习经验，需要用项目经历弥补"
  ]
}
```

---

### 9.3 岗位推荐 Prompt 优化目标

输出应包含：

```text
1. 推荐岗位名称
2. 推荐理由
3. 匹配标签
4. 匹配分数
5. 学习建议
6. 投递建议
```

建议结构：

```json
{
  "recommendations": [
    {
      "jobId": 1,
      "matchScore": 86,
      "reason": "该岗位技术栈与用户项目经历匹配度较高。",
      "matchedTags": ["Java", "Spring Boot", "MySQL"],
      "learningSuggestions": ["补充 Redis 缓存和接口优化经验"],
      "applySuggestion": "建议优先投递"
    }
  ]
}
```

---

### 9.4 面试题生成 Prompt 优化目标

输出应包含：

```text
1. 基础题
2. 项目题
3. 场景题
4. 八股题
5. 追问
6. 答题关键词
7. 参考回答
```

建议结构：

```json
{
  "questions": [
    {
      "type": "PROJECT",
      "difficulty": "MEDIUM",
      "question": "请介绍你在 InternPilot 中如何实现 RBAC 权限控制？",
      "answerHint": "可以从用户、角色、权限表设计，以及后端 @PreAuthorize 和前端菜单权限控制讲起。",
      "keywords": ["RBAC", "Spring Security", "JWT", "权限表"],
      "followUps": [
        "如果普通用户伪造请求访问管理员接口，你如何防止？"
      ]
    }
  ]
}
```

---

### 9.5 RAG 问答 Prompt 优化目标

RAG 问答必须强调：

```text
1. 优先基于知识库上下文回答
2. 如果上下文没有答案，要说明无法从知识库确认
3. 不要编造岗位信息
4. 回答要给出依据
5. 适合大学生求职场景
```

建议格式：

```json
{
  "answer": "根据知识库内容，Java 后端实习通常要求掌握 Spring Boot、MySQL、Redis 等技术。",
  "evidence": [
    "知识片段 1 提到 Spring Boot 和 MySQL 是岗位要求",
    "知识片段 2 提到 Redis 属于加分项"
  ],
  "confidence": "HIGH",
  "suggestions": [
    "建议优先准备 Spring Boot 项目介绍",
    "建议补充 Redis 缓存应用场景"
  ]
}
```

---

## 10. AI 输出格式与解析增强

### 10.1 统一输出格式枚举

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/prompt/AiOutputFormat.java
```

```java
public enum AiOutputFormat {
    JSON_OBJECT,
    JSON_ARRAY,
    MARKDOWN,
    PLAIN_TEXT
}
```

---

### 10.2 解析器设计

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/parser/AiResponseParser.java
```

接口：

```java
public interface AiResponseParser<T> {

    AiScenarioEnum scenario();

    T parse(String rawResponse);

    T fallback(String rawResponse, Exception exception);
}
```

不同场景实现不同解析器：

```text
ResumeJobAnalysisParser
JobRecommendationParser
InterviewQuestionParser
RagQaParser
```

---

### 10.3 JSON 清洗策略

对于 AI 返回 JSON，解析前做清洗：

```text
1. 去除 ```json 代码块包裹
2. 去除 JSON 前后的解释性文字
3. 尝试截取第一个 { 到最后一个 }
4. 校验字段是否存在
5. 分数超出范围时修正到 0-100
6. 列表为空时填充空数组
7. 解析失败时返回 fallback 结果
```

工具类：

```text
AiJsonSanitizer.java
```

---

## 11. AI 质量评分设计

### 11.1 为什么需要质量评分

AI 返回结果不一定每次都好，需要一个基础质量检查机制。

质量评分不是评价模型聪明程度，而是检查结果是否满足业务要求。

---

### 11.2 质量检查维度

```text
1. 是否为空
2. 是否可解析
3. 必填字段是否存在
4. 分数是否在 0-100
5. 列表数量是否达标
6. 是否使用中文
7. 是否出现明显格式错误
8. 是否出现“作为 AI 模型”等无关表达
9. RAG 回答是否引用上下文
```

---

### 11.3 新增质量评估器

新增：

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/quality/AiQualityEvaluator.java
```

```java
public interface AiQualityEvaluator<T> {

    AiScenarioEnum scenario();

    AiQualityResult evaluate(T parsedResult, String rawResponse);
}
```

结果对象：

```java
@Data
@Builder
public class AiQualityResult {

    private int score;

    private boolean passed;

    private List<String> warnings;

    private List<String> errors;
}
```

---

## 12. 失败重试与降级设计

### 12.1 重试场景

允许重试：

```text
1. 网络超时
2. 连接失败
3. 临时 5xx 错误
4. 响应为空
5. JSON 解析失败但原始内容疑似可修复
```

不建议重试：

```text
1. API Key 错误
2. 权限错误
3. 余额不足
4. 参数非法
5. 用户主动取消任务
```

---

### 12.2 降级策略

降级策略：

```text
第一层：同模型重试
第二层：fallback 模型
第三层：返回结构化兜底结果
第四层：提示用户稍后重试
```

示例：

```text
RESUME_JOB_ANALYSIS:
    deepseek-v4-pro 失败
      ↓
    重试 1 次
      ↓
    fallback 到 deepseek-v4-flash
      ↓
    仍失败则返回“分析失败，请稍后重试”

JOB_RECOMMENDATION:
    deepseek-v4-flash 失败
      ↓
    返回基于规则的简单推荐，后续可选

INTERVIEW_QUESTION_GENERATION:
    deepseek-v4-flash 失败
      ↓
    返回基础通用面试题模板，后续可选
```

---

## 13. AI 缓存 Key 优化

### 13.1 当前问题

如果缓存 key 只包含简历和岗位信息，Prompt 或模型变化后，可能命中旧缓存。

---

### 13.2 新缓存 Key 组成

建议缓存 key 包含：

```text
scenario
model
promptVersion
resumeId
resumeVersionId
resumeUpdatedAt
jobId
jobUpdatedAt
ragKnowledgeVersion
promptHash
```

示例：

```text
ai:cache:{scenario}:{model}:{promptVersion}:{businessHash}
```

---

### 13.3 修改缓存构建器

修改：

```text
AiAnalysisCacheKeyBuilder.java
```

或新增统一：

```text
AiCacheKeyBuilder.java
```

接口：

```java
public String build(AiCacheKeyContext context);
```

上下文：

```java
@Data
@Builder
public class AiCacheKeyContext {

    private AiScenarioEnum scenario;

    private String model;

    private String promptVersion;

    private Long resumeId;

    private Long resumeVersionId;

    private LocalDateTime resumeUpdatedAt;

    private Long jobId;

    private LocalDateTime jobUpdatedAt;

    private String promptHash;

    private Map<String, Object> extra;
}
```

---

## 14. AI 调用日志增强

### 14.1 日志目标

AI 调用日志要能回答：

```text
1. 谁调用了 AI？
2. 调用了什么场景？
3. 用了什么模型？
4. 用了哪个 Prompt 版本？
5. 是否命中缓存？
6. 调用花了多久？
7. 是否重试？
8. 是否降级？
9. 是否成功？
10. 失败原因是什么？
```

---

### 14.2 日志字段

建议新增或增强日志：

```text
userId
scenario
provider
model
promptVersion
promptHash
responseHash
cacheKey
cacheHit
durationMs
retryCount
fallbackUsed
success
errorCode
errorMessage
createdAt
```

---

### 14.3 安全注意事项

日志中不要打印：

```text
1. 完整简历内容
2. 完整岗位内容
3. 完整 Prompt
4. 完整 DeepSeek API Key
5. 邮箱授权码
6. JWT token
```

可以打印：

```text
1. 内容长度
2. hash
3. 场景
4. 模型
5. 版本
6. 耗时
7. 错误码
```

---

## 15. 数据库设计，可选

第一阶段可以不新增表，先用代码配置和日志完成。

如果要增强可观测性，可以新增：

```text
ai_call_log
```

### 15.1 ai_call_log 表

```sql
CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI调用日志ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    scenario VARCHAR(64) NOT NULL COMMENT 'AI场景',
    provider VARCHAR(64) NOT NULL COMMENT '模型供应商',
    model VARCHAR(128) NOT NULL COMMENT '模型名称',
    prompt_version VARCHAR(64) NOT NULL COMMENT 'Prompt版本',
    prompt_hash VARCHAR(128) DEFAULT NULL COMMENT 'Prompt哈希',
    response_hash VARCHAR(128) DEFAULT NULL COMMENT '响应哈希',
    cache_key VARCHAR(255) DEFAULT NULL COMMENT '缓存Key',
    cache_hit TINYINT DEFAULT 0 COMMENT '是否命中缓存',
    duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    fallback_used TINYINT DEFAULT 0 COMMENT '是否使用降级模型',
    success TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='AI调用日志表';
```

说明：

```text
如果时间紧，可以先不建表，只增强日志输出。
如果想让管理员后台查看 AI 调用情况，再新增 ai_call_log 表。
```

---

## 16. 后端新增/修改文件建议

### 16.1 新增文件

```text
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/router/AiModelRouter.java
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/router/DefaultAiModelRouter.java

backend/intern-pilot-backend/src/main/java/com/internpilot/ai/config/AiModelProperties.java

backend/intern-pilot-backend/src/main/java/com/internpilot/ai/prompt/AiPromptContext.java
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/prompt/AiOutputFormat.java
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/prompt/template/AiPromptTemplate.java
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/prompt/template/AiPromptTemplateResolver.java

backend/intern-pilot-backend/src/main/java/com/internpilot/ai/parser/AiResponseParser.java
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/parser/AiJsonSanitizer.java

backend/intern-pilot-backend/src/main/java/com/internpilot/ai/quality/AiQualityEvaluator.java
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/quality/AiQualityResult.java

backend/intern-pilot-backend/src/main/java/com/internpilot/ai/cache/AiCacheKeyContext.java
backend/intern-pilot-backend/src/main/java/com/internpilot/ai/cache/AiCacheKeyBuilder.java
```

---

### 16.2 修改文件

根据实际项目结构修改：

```text
AiScenarioEnum.java
DeepSeekAiClient.java
MockAiClient.java
AnalysisServiceImpl.java
JobRecommendationServiceImpl.java
InterviewQuestionServiceImpl.java
RagQaServiceImpl.java
AiAnalysisCacheKeyBuilder.java
application.yml
application-prod.yml
application-test.yml
```

---

## 17. 前端改造设计

本阶段前端改动较少，重点是展示 AI 调用信息和优化用户提示。

### 17.1 AI 任务中心展示模型信息，可选

在任务详情中显示：

```text
AI 场景：简历岗位匹配分析
模型：DeepSeek Pro
Prompt 版本：v2
状态：已完成
```

注意：

```text
普通用户不一定需要看到技术细节。
可以只在管理员模式或调试模式显示。
```

---

### 17.2 管理员后台 AI 调用日志，可选

如果新增 `ai_call_log` 表，可以在管理员后台新增：

```text
AI 调用日志
```

功能：

```text
1. 按场景筛选
2. 按模型筛选
3. 按成功/失败筛选
4. 查看耗时
5. 查看错误码
6. 查看是否命中缓存
```

此功能可以作为后续迭代，不作为本阶段必须完成项。

---

## 18. 开发步骤建议

### 第 1 步：创建分支

```bash
git checkout dev
git pull origin dev
git checkout -b feature/ai-model-router-prompt-optimization
```

---

### 第 2 步：保存设计文档

```bash
touch docs/42-ai-model-router-and-prompt-optimization-design.md
```

将本文档复制进去。

---

### 第 3 步：统一 AI 场景

```text
1. 检查 AiScenarioEnum
2. 补齐所有 AI 场景
3. 替换代码中的硬编码场景字符串
4. 确保测试通过
```

---

### 第 4 步：实现模型路由

```text
1. 新增 AiModelProperties
2. 新增 AiModelRouter
3. DeepSeekAiClient 支持传入 model
4. 各业务场景通过 router 获取 model
5. 日志输出 scenario + model
```

---

### 第 5 步：实现 Prompt 模板版本管理

```text
1. 新增 AiPromptTemplate 接口
2. 为简历分析、岗位推荐、面试题、RAG 建立模板类
3. 每个模板明确 version
4. 缓存 key 加入 promptVersion
5. 日志输出 promptVersion
```

---

### 第 6 步：增强解析与格式修复

```text
1. 新增 AiJsonSanitizer
2. 改造现有 Parser
3. 支持去除 ```json 代码块
4. 支持字段兜底
5. 支持解析失败 fallback
```

---

### 第 7 步：增强重试与降级

```text
1. 对网络异常和 5xx 错误启用重试
2. API Key 错误不重试
3. 用户取消任务不重试
4. fallback 模型只在允许场景使用
5. 日志记录 retryCount 和 fallbackUsed
```

---

### 第 8 步：测试与验收

```text
1. 单元测试 PromptTemplateResolver
2. 单元测试 AiModelRouter
3. 单元测试 AiJsonSanitizer
4. 单元测试 CacheKeyBuilder
5. MockAiClient 测试不同场景
6. DeepSeekAiClient 测试配置读取
7. 前端构建测试
8. 线上环境确认仍然使用 DeepSeek
```

---

## 19. 测试清单

### 19.1 后端单元测试

建议新增：

```text
AiModelRouterTest
AiPromptTemplateResolverTest
AiJsonSanitizerTest
AiCacheKeyBuilderTest
AiQualityEvaluatorTest
```

---

### 19.2 功能测试

必须测试：

```text
1. 简历岗位分析可以正常调用
2. 岗位推荐可以正常调用
3. 面试题生成可以正常调用
4. RAG 问答可以正常调用
5. Mock 环境不调用真实 DeepSeek
6. 生产环境不使用 Mock
7. 不同场景能路由到不同模型
8. Prompt 版本能正确进入缓存 key
9. AI 返回 ```json 包裹内容时能解析
10. AI 返回字段缺失时系统不崩溃
11. DeepSeek 临时失败时能重试
12. 不可重试错误不会无限重试
```

---

### 19.3 安全测试

```text
1. 日志不输出 API Key
2. 日志不输出完整简历
3. 日志不输出完整 Prompt
4. README 不公开模型密钥
5. 前端不暴露后端模型配置密钥
```

---

## 20. 验收标准

本阶段完成后，应满足：

```text
1. AI 场景统一管理
2. 不同 AI 场景可配置不同模型
3. Prompt 具有明确版本号
4. 缓存 key 包含 model + promptVersion
5. AI JSON 解析更稳定
6. DeepSeek 调用日志更完整
7. Mock 仍然只用于测试
8. 线上仍然使用 DeepSeek 真接口
9. 后端测试通过
10. 前端构建通过
11. 不破坏已有 AI 分析、岗位推荐、面试题、RAG 功能
```

---

## 21. 项目面试讲法

如果面试官问：

```text
你是怎么优化 AI 调用链路的？
```

可以回答：

```text
项目最初只是直接在业务 Service 中构造 Prompt 并调用 DeepSeek。后续我对 AI 调用链路做了工程化优化：首先把不同 AI 功能抽象成 AiScenarioEnum，例如简历分析、岗位推荐、面试题生成和 RAG 问答。然后设计 AiModelRouter，根据不同场景选择不同模型，简单任务使用更快模型，复杂任务使用更强模型。同时我为 Prompt 增加版本管理，缓存 key 中加入 model 和 promptVersion，避免 Prompt 更新后误命中旧缓存。最后还增强了 AI 返回结果解析、JSON 清洗、失败重试、日志记录和降级策略，提高了 AI 功能的稳定性和可维护性。
```

---

## 22. 推荐提交信息

```bash
git add .
git commit -m "Optimize AI model routing and prompt management"
git push origin feature/ai-model-router-prompt-optimization
```

合并 dev：

```bash
git checkout dev
git pull origin dev
git merge feature/ai-model-router-prompt-optimization
git push origin dev
```
