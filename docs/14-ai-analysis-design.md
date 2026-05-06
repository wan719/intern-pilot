# InternPilot AI 分析模块设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的 AI 分析模块设计，包括模块目标、业务流程、Prompt 设计、AI 调用抽象、Redis 缓存设计、分析报告入库设计、接口设计、DTO/VO 设计、Entity/Mapper 设计、Service 设计、异常处理、测试流程和后续扩展方案。

AI 分析模块是 InternPilot 的核心亮点模块。它负责读取用户简历解析文本和岗位 JD 内容，调用大语言模型生成岗位匹配度分析报告，帮助用户判断自身简历与目标岗位之间的匹配程度，并给出简历优化建议和面试准备建议。

---

## 2. 模块目标

AI 分析模块需要完成以下目标：

1. 支持用户选择简历和岗位进行匹配分析；
2. 校验简历是否属于当前用户；
3. 校验岗位是否属于当前用户；
4. 读取简历解析文本 `resume.parsed_text`；
5. 读取岗位 JD 内容 `job_description.jd_content`；
6. 构造标准化 Prompt；
7. 调用大语言模型 API；
8. 要求 AI 返回结构化 JSON；
9. 解析 AI 返回结果；
10. 保存分析报告到数据库；
11. 使用 Redis 缓存相同简历和岗位的分析结果；
12. 支持 `forceRefresh` 强制重新分析；
13. 提供分析报告列表查询；
14. 提供分析报告详情查询；
15. 处理 AI 调用失败、超时、返回格式异常等问题。

---

## 3. 模块业务定位

AI 分析模块是 InternPilot 的核心业务模块，连接了简历模块、岗位模块和投递记录模块。

整体链路如下：

```text
用户上传简历
  ↓
系统解析简历文本
  ↓
用户创建岗位 JD
  ↓
用户选择简历 + 岗位
  ↓
AI 分析模块读取 parsedText + jdContent
  ↓
构造 Prompt
  ↓
调用大语言模型
  ↓
生成匹配分析报告
  ↓
保存 analysis_report
  ↓
用户根据报告决定是否投递
  ↓
创建投递记录
```

## 4. 功能范围

### 4.1 第一阶段必须实现

第一阶段需要实现：

1. 简历岗位匹配分析；
2. Prompt 模板构造；
3. AI API 调用；
4. AI JSON 结果解析；
5. 分析报告保存；
6. 分析报告列表查询；
7. 分析报告详情查询；
8. Redis 缓存分析结果；
9. `forceRefresh` 重新分析；
10. AI 异常统一处理。

### 4.2 第二阶段可扩展

第二阶段可以扩展：

1. 面试题自动生成；
2. 简历项目经历优化；
3. 岗位技能关键词提取；
4. 多模型切换；
5. AI 调用日志；
6. Prompt 模板数据库管理；
7. WebSocket 实时分析进度；
8. 异步任务分析；
9. RAG 岗位知识库；
10. 用户求职画像。

## 5. 数据库设计

### 5.1 `analysis_report` 表

AI 分析模块主要使用 `analysis_report` 表。

```sql
CREATE TABLE IF NOT EXISTS analysis_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分析报告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    job_id BIGINT NOT NULL COMMENT '岗位ID',
    match_score INT DEFAULT NULL COMMENT '匹配分数，0-100',
    match_level VARCHAR(30) DEFAULT NULL COMMENT '匹配等级',
    strengths TEXT DEFAULT NULL COMMENT '简历优势，JSON字符串',
    weaknesses TEXT DEFAULT NULL COMMENT '简历短板，JSON字符串',
    missing_skills TEXT DEFAULT NULL COMMENT '缺失技能，JSON字符串',
    suggestions TEXT DEFAULT NULL COMMENT '简历优化建议，JSON字符串',
    interview_tips TEXT DEFAULT NULL COMMENT '面试准备建议，JSON字符串',
    raw_ai_response LONGTEXT DEFAULT NULL COMMENT 'AI原始返回',
    ai_provider VARCHAR(50) DEFAULT NULL COMMENT 'AI服务商',
    ai_model VARCHAR(100) DEFAULT NULL COMMENT 'AI模型',
    cache_hit TINYINT NOT NULL DEFAULT 0 COMMENT '是否命中缓存：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_report_user_id (user_id),
    KEY idx_report_resume_id (resume_id),
    KEY idx_report_job_id (job_id),
    KEY idx_report_user_resume_job (user_id, resume_id, job_id),
    KEY idx_report_score (match_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析报告表';
```

### 5.2 字段说明

| 字段 | 说明 |
|---|---|
| id | 分析报告 ID |
| user_id | 报告所属用户 |
| resume_id | 关联简历 ID |
| job_id | 关联岗位 ID |
| match_score | 匹配分数，0-100 |
| match_level | 匹配等级 |
| strengths | 简历优势，JSON 字符串 |
| weaknesses | 简历短板，JSON 字符串 |
| missing_skills | 缺失技能，JSON 字符串 |
| suggestions | 简历优化建议，JSON 字符串 |
| interview_tips | 面试准备建议，JSON 字符串 |
| raw_ai_response | AI 原始返回内容 |
| ai_provider | AI 服务商，例如 DEEPSEEK、OPENAI、QWEN |
| ai_model | AI 模型名称 |
| cache_hit | 是否命中缓存 |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| deleted | 逻辑删除 |

### 5.3 JSON 字符串字段说明

第一阶段为了降低复杂度，以下字段直接保存 JSON 字符串：

- `strengths`
- `weaknesses`
- `missing_skills`
- `suggestions`
- `interview_tips`

示例：

```json
[
  "具备 Spring Boot 项目经验",
  "熟悉 JWT 鉴权和权限控制",
  "有 MySQL 和 Redis 使用经验"
]
```

这样做的好处：

1. 数据库表更简单；
2. 方便直接返回前端；
3. 不需要额外设计子表；
4. 适合 MVP 阶段。

后续如果需要统计“常见技能缺口”，可以再拆成结构化表。

## 6. 匹配等级设计

### 6.1 `match_score`

`match_score` 表示简历和岗位的匹配分数，范围为：

```text
0 - 100
```

### 6.2 `match_level`

匹配等级规则如下：

| 分数范围 | 等级 | 说明 |
|---|---|---|
| 85-100 | HIGH | 高匹配 |
| 70-84 | MEDIUM_HIGH | 较高匹配 |
| 60-69 | MEDIUM | 中等匹配 |
| 40-59 | LOW | 低匹配 |
| 0-39 | VERY_LOW | 很低匹配 |

### 6.3 `MatchLevelEnum`

路径：

`src/main/java/com/internpilot/enums/MatchLevelEnum.java`

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum MatchLevelEnum {

    HIGH("HIGH", "高匹配"),
    MEDIUM_HIGH("MEDIUM_HIGH", "较高匹配"),
    MEDIUM("MEDIUM", "中等匹配"),
    LOW("LOW", "低匹配"),
    VERY_LOW("VERY_LOW", "很低匹配");

    private final String code;
    private final String description;

    MatchLevelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String fromScore(Integer score) {
        if (score == null) {
            return MEDIUM.getCode();
        }

        if (score >= 85) {
            return HIGH.getCode();
        } else if (score >= 70) {
            return MEDIUM_HIGH.getCode();
        } else if (score >= 60) {
            return MEDIUM.getCode();
        } else if (score >= 40) {
            return LOW.getCode();
        } else {
            return VERY_LOW.getCode();
        }
    }
}
```

## 7. AI 服务配置设计

### 7.1 `application-dev.yml`

```yaml
ai:
  provider: deepseek
  api-key: ${AI_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  timeout: 60000
```

### 7.2 `AiProperties`

路径：

`src/main/java/com/internpilot/config/AiProperties.java`

```java
package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider;

    private String apiKey;

    private String baseUrl;

    private String model;

    private Long timeout;
}
```

### 7.3 配置安全要求

AI API Key 不能写死在代码中。

推荐使用环境变量：

```text
AI_API_KEY
```

在 Windows PowerShell 中设置临时环境变量：

```powershell
$env:AI_API_KEY="你的API Key"
```

注意：

1. 不要把真实 API Key 提交到 GitHub；
2. 不要把 API Key 打印到日志；
3. 不要把 API Key 返回给前端。

## 8. Redis 缓存设计

### 8.1 缓存目标

AI 分析接口有两个问题：

1. 调用外部模型较慢；
2. 可能产生调用成本。

因此需要对相同简历和相同岗位的分析结果进行缓存。

### 8.2 缓存 Key 设计

```text
internpilot:analysis:{userId}:{resumeId}:{jobId}
```

示例：

```text
internpilot:analysis:1:10:25
```

### 8.3 `forceRefresh` 设计

接口请求中增加：

```json
{
  "resumeId": 1,
  "jobId": 1,
  "forceRefresh": false
}
```

含义：

| forceRefresh | 说明 |
|---|---|
| false | 优先读取 Redis 缓存 |
| true | 忽略缓存，强制重新调用 AI |

### 8.4 缓存过期时间

第一阶段建议：

```text
24 小时
```

Java 中可以定义：

```java
private static final long ANALYSIS_CACHE_TTL_HOURS = 24;
```

### 8.5 缓存 Value 设计

缓存内容可以直接保存 `AnalysisResultResponse` 对象。

JSON 示例：

```json
{
  "reportId": 1,
  "resumeId": 1,
  "jobId": 1,
  "matchScore": 82,
  "matchLevel": "MEDIUM_HIGH",
  "strengths": [
    "具备 Spring Boot 项目经验",
    "熟悉 JWT 鉴权和权限控制"
  ],
  "weaknesses": [
    "缺少真实企业实习经历"
  ],
  "missingSkills": [
    "Docker",
    "Linux 部署"
  ],
  "suggestions": [
    "补充 Docker Compose 部署说明"
  ],
  "interviewTips": [
    "准备 Spring Security 过滤链执行流程"
  ],
  "cacheHit": false
}
```

### 8.6 缓存失效策略

第一阶段可以依赖 TTL 自动过期。

后续可以在以下场景主动删除缓存：

1. 简历 `parsedText` 修改；
2. 岗位 `jdContent` 修改；
3. 用户点击重新分析；
4. 用户删除简历；
5. 用户删除岗位。

## 9. Prompt 设计

### 9.1 Prompt 设计目标

Prompt 需要让 AI 输出：

1. 匹配分数；
2. 匹配等级；
3. 简历优势；
4. 简历短板；
5. 缺失技能；
6. 简历优化建议；
7. 面试准备建议；
8. 严格 JSON 格式。

### 9.2 Prompt 模板

第一阶段推荐使用固定 Prompt 模板。

```text
你是一个资深技术招聘官、Java 后端面试官和大学生实习求职导师。

请根据下面的【学生简历文本】和【目标岗位 JD】，分析该学生与岗位的匹配程度。

你的任务：
1. 给出 0-100 的匹配分数 matchScore；
2. 给出匹配等级 matchLevel；
3. 分析该学生简历中的优势 strengths；
4. 分析该学生简历中的短板 weaknesses；
5. 提取岗位要求中该学生可能缺失的技能 missingSkills；
6. 给出具体的简历优化建议 suggestions；
7. 给出面试准备建议 interviewTips。

评分要求：
- 如果简历中有明确相关项目经验、技术栈高度匹配，分数可以较高；
- 如果只是课程经历或泛泛描述，分数应适中；
- 如果缺少岗位核心技能，分数应降低；
- 不要给过于虚高的分数；
- 建议要具体，不能只写“继续努力”。

必须严格返回 JSON。
不要返回 Markdown。
不要返回多余解释。
不要使用 ```json 代码块包裹。

返回格式如下：

{
  "matchScore": 82,
  "matchLevel": "MEDIUM_HIGH",
  "strengths": [
    "优势1",
    "优势2"
  ],
  "weaknesses": [
    "短板1",
    "短板2"
  ],
  "missingSkills": [
    "缺失技能1",
    "缺失技能2"
  ],
  "suggestions": [
    "优化建议1",
    "优化建议2"
  ],
  "interviewTips": [
    "面试准备建议1",
    "面试准备建议2"
  ]
}

匹配等级规则：
- 85-100: HIGH
- 70-84: MEDIUM_HIGH
- 60-69: MEDIUM
- 40-59: LOW
- 0-39: VERY_LOW

【学生简历文本】
{resumeText}

【目标岗位 JD】
{jobDescription}
```

### 9.3 `PromptUtils`

路径：

`src/main/java/com/internpilot/util/PromptUtils.java`

```java
package com.internpilot.util;

public class PromptUtils {

    private PromptUtils() {
    }

    public static String buildResumeJobMatchPrompt(String resumeText, String jobDescription) {
        return """
                你是一个资深技术招聘官、Java 后端面试官和大学生实习求职导师。

                请根据下面的【学生简历文本】和【目标岗位 JD】，分析该学生与岗位的匹配程度。

                你的任务：
                1. 给出 0-100 的匹配分数 matchScore；
                2. 给出匹配等级 matchLevel；
                3. 分析该学生简历中的优势 strengths；
                4. 分析该学生简历中的短板 weaknesses；
                5. 提取岗位要求中该学生可能缺失的技能 missingSkills；
                6. 给出具体的简历优化建议 suggestions；
                7. 给出面试准备建议 interviewTips。

                评分要求：
                - 如果简历中有明确相关项目经验、技术栈高度匹配，分数可以较高；
                - 如果只是课程经历或泛泛描述，分数应适中；
                - 如果缺少岗位核心技能，分数应降低；
                - 不要给过于虚高的分数；
                - 建议要具体，不能只写“继续努力”。

                必须严格返回 JSON。
                不要返回 Markdown。
                不要返回多余解释。
                不要使用 ```json 代码块包裹。

                返回格式如下：

                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": [
                    "优势1",
                    "优势2"
                  ],
                  "weaknesses": [
                    "短板1",
                    "短板2"
                  ],
                  "missingSkills": [
                    "缺失技能1",
                    "缺失技能2"
                  ],
                  "suggestions": [
                    "优化建议1",
                    "优化建议2"
                  ],
                  "interviewTips": [
                    "面试准备建议1",
                    "面试准备建议2"
                  ]
                }

                匹配等级规则：
                - 85-100: HIGH
                - 70-84: MEDIUM_HIGH
                - 60-69: MEDIUM
                - 40-59: LOW
                - 0-39: VERY_LOW

                【学生简历文本】
                %s

                【目标岗位 JD】
                %s
                """.formatted(resumeText, jobDescription);
    }
}
```

## 10. AI 调用抽象设计

### 10.1 为什么需要抽象 `AiClient`

第一阶段你可能使用 DeepSeek API。

但后续可能切换为：

1. OpenAI；
2. 通义千问；
3. 智谱；
4. 本地大模型；
5. Mock 模型。

因此不要把模型调用逻辑直接写死在 `AnalysisServiceImpl` 中。

推荐抽象接口：

```text
AiClient
```

### 10.2 `AiClient` 接口

路径：

`src/main/java/com/internpilot/service/AiClient.java`

```java
package com.internpilot.service;

public interface AiClient {

    String chat(String prompt);
}
```

### 10.3 `MockAiClient`

开发早期可以先用 Mock，避免 API Key 和费用问题。

路径：

`src/main/java/com/internpilot/service/impl/MockAiClient.java`

```java
package com.internpilot.service.impl;

import com.internpilot.service.AiClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockAiClient implements AiClient {

    @Override
    public String chat(String prompt) {
        return """
                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": [
                    "具备 Spring Boot 项目经验",
                    "熟悉 JWT 鉴权和权限控制",
                    "有 MySQL 和 Redis 使用经验"
                  ],
                  "weaknesses": [
                    "缺少真实企业实习经历",
                    "项目中高并发和部署经验体现不足"
                  ],
                  "missingSkills": [
                    "Docker",
                    "Linux 部署",
                    "消息队列"
                  ],
                  "suggestions": [
                    "在项目描述中补充 Redis 缓存使用场景",
                    "增加 Docker Compose 部署说明",
                    "补充接口文档和测试说明"
                  ],
                  "interviewTips": [
                    "准备 Spring Security 过滤链执行流程",
                    "准备 JWT 登录流程",
                    "准备 Redis 缓存穿透、击穿、雪崩问题"
                  ]
                }
                """;
    }
}
```

### 10.4 `DeepSeekAiClient` 设计

如果你使用 DeepSeek，可以封装为：

路径：

`src/main/java/com/internpilot/service/impl/DeepSeekAiClient.java`

第一阶段示例设计：

```java
package com.internpilot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.config.AiProperties;
import com.internpilot.exception.AiServiceException;
import com.internpilot.service.AiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class DeepSeekAiClient implements AiClient {

    private final AiProperties aiProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String chat(String prompt) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new AiServiceException("AI API Key 未配置");
        }

        String url = aiProperties.getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new AiServiceException("AI 服务调用失败");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("AI 服务调用异常");
        }
    }
}
```

### 10.5 `RestTemplate` Bean

路径：

`src/main/java/com/internpilot/config/WebConfig.java`

```java
package com.internpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

然后 `DeepSeekAiClient` 中可以注入：

```java
private final RestTemplate restTemplate;
```

## 11. AI 返回结果解析设计

### 11.1 `AiAnalysisResult`

路径：

`src/main/java/com/internpilot/dto/analysis/AiAnalysisResult.java`

```java
package com.internpilot.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class AiAnalysisResult {

    private Integer matchScore;

    private String matchLevel;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> missingSkills;

    private List<String> suggestions;

    private List<String> interviewTips;
}
```

### 11.2 JSON 清洗

大模型有时会返回 Markdown 代码块或在 JSON 前后添加解释文字。

第一阶段可以先做简单清洗。

路径：

`src/main/java/com/internpilot/util/JsonUtils.java`

```java
package com.internpilot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.exception.AiServiceException;

public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static <T> T parseAiJson(String rawText, Class<T> clazz) {
        try {
            String json = extractJson(rawText);
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI 返回结果解析失败");
        }
    }

    public static String toJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("JSON 序列化失败");
        }
    }

    public static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("JSON 反序列化失败");
        }
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new AiServiceException("AI 返回内容为空");
        }

        String text = rawText.trim();

        if (text.startsWith("```json")) {
            text = text.substring(7).trim();
        }

        if (text.startsWith("```")) {
            text = text.substring(3).trim();
        }

        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start < 0 || end < 0 || end <= start) {
            throw new AiServiceException("AI 返回内容不是合法 JSON");
        }

        return text.substring(start, end + 1);
    }
}
```

### 11.3 结果校验

AI 返回结果解析后需要校验：

1. `matchScore` 不为空；
2. `matchScore` 范围在 0-100；
3. `matchLevel` 如果为空，则根据分数自动计算；
4. `strengths` 为空时使用空数组；
5. `weaknesses` 为空时使用空数组；
6. `suggestions` 为空时使用空数组。

## 12. Entity 与 Mapper 设计

### 12.1 `AnalysisReport` Entity

路径：

`src/main/java/com/internpilot/entity/AnalysisReport.java`

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_report")
public class AnalysisReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jobId;

    private Integer matchScore;

    private String matchLevel;

    private String strengths;

    private String weaknesses;

    private String missingSkills;

    private String suggestions;

    private String interviewTips;

    private String rawAiResponse;

    private String aiProvider;

    private String aiModel;

    private Integer cacheHit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

### 12.2 `AnalysisReportMapper`

路径：

`src/main/java/com/internpilot/mapper/AnalysisReportMapper.java`

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.AnalysisReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisReportMapper extends BaseMapper<AnalysisReport> {
}
```

## 13. DTO 与 VO 设计

### 13.1 `AnalysisMatchRequest`

路径：

`src/main/java/com/internpilot/dto/analysis/AnalysisMatchRequest.java`

```java
package com.internpilot.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "简历岗位匹配分析请求")
public class AnalysisMatchRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位 ID 不能为空")
    private Long jobId;

    @Schema(description = "是否强制重新分析", example = "false")
    private Boolean forceRefresh = false;
}
```

### 13.2 `AnalysisResultResponse`

路径：

`src/main/java/com/internpilot/vo/analysis/AnalysisResultResponse.java`

```java
package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "AI 分析结果响应")
public class AnalysisResultResponse {

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "匹配分数")
    private Integer matchScore;

    @Schema(description = "匹配等级")
    private String matchLevel;

    @Schema(description = "简历优势")
    private List<String> strengths;

    @Schema(description = "简历短板")
    private List<String> weaknesses;

    @Schema(description = "缺失技能")
    private List<String> missingSkills;

    @Schema(description = "简历优化建议")
    private List<String> suggestions;

    @Schema(description = "面试准备建议")
    private List<String> interviewTips;

    @Schema(description = "是否命中缓存")
    private Boolean cacheHit;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 13.3 `AnalysisReportListResponse`

路径：

`src/main/java/com/internpilot/vo/analysis/AnalysisReportListResponse.java`

```java
package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "分析报告列表响应")
public class AnalysisReportListResponse {

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "匹配分数")
    private Integer matchScore;

    @Schema(description = "匹配等级")
    private String matchLevel;

    @Schema(description = "是否命中缓存")
    private Boolean cacheHit;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 13.4 `AnalysisReportDetailResponse`

路径：

`src/main/java/com/internpilot/vo/analysis/AnalysisReportDetailResponse.java`

```java
package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "分析报告详情响应")
public class AnalysisReportDetailResponse {

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "匹配分数")
    private Integer matchScore;

    @Schema(description = "匹配等级")
    private String matchLevel;

    @Schema(description = "简历优势")
    private List<String> strengths;

    @Schema(description = "简历短板")
    private List<String> weaknesses;

    @Schema(description = "缺失技能")
    private List<String> missingSkills;

    @Schema(description = "简历优化建议")
    private List<String> suggestions;

    @Schema(description = "面试准备建议")
    private List<String> interviewTips;

    @Schema(description = "AI 服务商")
    private String aiProvider;

    @Schema(description = "AI 模型")
    private String aiModel;

    @Schema(description = "是否命中缓存")
    private Boolean cacheHit;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

## 14. Service 设计

### 14.1 `AnalysisService`

路径：

`src/main/java/com/internpilot/service/AnalysisService.java`

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;

public interface AnalysisService {

    AnalysisResultResponse match(AnalysisMatchRequest request);

    PageResult<AnalysisReportListResponse> listReports(
            Long resumeId,
            Long jobId,
            Integer minScore,
            Integer pageNum,
            Integer pageSize
    );

    AnalysisReportDetailResponse getReportDetail(Long id);
}
```

### 14.2 `AnalysisServiceImpl` 核心代码

路径：

`src/main/java/com/internpilot/service/impl/AnalysisServiceImpl.java`

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.config.AiProperties;
import com.internpilot.dto.analysis.AiAnalysisResult;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.enums.MatchLevelEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.service.AiClient;
import com.internpilot.service.AnalysisService;
import com.internpilot.util.JsonUtils;
import com.internpilot.util.PromptUtils;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private static final long CACHE_TTL_HOURS = 24;

    private final ResumeMapper resumeMapper;

    private final JobDescriptionMapper jobDescriptionMapper;

    private final AnalysisReportMapper analysisReportMapper;

    private final AiClient aiClient;

    private final AiProperties aiProperties;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public AnalysisResultResponse match(AnalysisMatchRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Resume resume = getUserResumeOrThrow(request.getResumeId(), currentUserId);
        JobDescription job = getUserJobOrThrow(request.getJobId(), currentUserId);

        if (!StringUtils.hasText(resume.getParsedText())) {
            throw new BusinessException("简历解析文本为空");
        }

        if (!StringUtils.hasText(job.getJdContent())) {
            throw new BusinessException("岗位 JD 不能为空");
        }

        String cacheKey = buildCacheKey(currentUserId, resume.getId(), job.getId());

        boolean forceRefresh = Boolean.TRUE.equals(request.getForceRefresh());

        if (!forceRefresh) {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof AnalysisResultResponse cachedResponse) {
                cachedResponse.setCacheHit(true);
                return cachedResponse;
            }
        }

        String prompt = PromptUtils.buildResumeJobMatchPrompt(
                resume.getParsedText(),
                job.getJdContent()
        );

        String rawResponse = aiClient.chat(prompt);

        AiAnalysisResult aiResult = JsonUtils.parseAiJson(rawResponse, AiAnalysisResult.class);

        normalizeAiResult(aiResult);

        AnalysisReport report = new AnalysisReport();
        report.setUserId(currentUserId);
        report.setResumeId(resume.getId());
        report.setJobId(job.getId());
        report.setMatchScore(aiResult.getMatchScore());
        report.setMatchLevel(aiResult.getMatchLevel());
        report.setStrengths(JsonUtils.toJsonString(nullToEmpty(aiResult.getStrengths())));
        report.setWeaknesses(JsonUtils.toJsonString(nullToEmpty(aiResult.getWeaknesses())));
        report.setMissingSkills(JsonUtils.toJsonString(nullToEmpty(aiResult.getMissingSkills())));
        report.setSuggestions(JsonUtils.toJsonString(nullToEmpty(aiResult.getSuggestions())));
        report.setInterviewTips(JsonUtils.toJsonString(nullToEmpty(aiResult.getInterviewTips())));
        report.setRawAiResponse(rawResponse);
        report.setAiProvider(aiProperties.getProvider());
        report.setAiModel(aiProperties.getModel());
        report.setCacheHit(0);

        analysisReportMapper.insert(report);

        AnalysisResultResponse response = toResultResponse(report);
        response.setCacheHit(false);

        redisTemplate.opsForValue().set(
                cacheKey,
                response,
                Duration.ofHours(CACHE_TTL_HOURS)
        );

        return response;
    }

    @Override
    public PageResult<AnalysisReportListResponse> listReports(
            Long resumeId,
            Long jobId,
            Integer minScore,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisReport::getUserId, currentUserId)
                .eq(AnalysisReport::getDeleted, 0);

        if (resumeId != null) {
            wrapper.eq(AnalysisReport::getResumeId, resumeId);
        }

        if (jobId != null) {
            wrapper.eq(AnalysisReport::getJobId, jobId);
        }

        if (minScore != null) {
            wrapper.ge(AnalysisReport::getMatchScore, minScore);
        }

        wrapper.orderByDesc(AnalysisReport::getCreatedAt);

        Page<AnalysisReport> page = new Page<>(pageNum, pageSize);

        Page<AnalysisReport> resultPage = analysisReportMapper.selectPage(page, wrapper);

        List<AnalysisReportListResponse> records = resultPage.getRecords()
                .stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public AnalysisReportDetailResponse getReportDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        AnalysisReport report = getUserReportOrThrow(id, currentUserId);

        return toDetailResponse(report);
    }

    private Resume getUserResumeOrThrow(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (resume == null) {
            throw new BusinessException("简历不存在或无权限访问");
        }

        return resume;
    }

    private JobDescription getUserJobOrThrow(Long jobId, Long userId) {
        JobDescription job = jobDescriptionMapper.selectOne(
                new LambdaQueryWrapper<JobDescription>()
                        .eq(JobDescription::getId, jobId)
                        .eq(JobDescription::getUserId, userId)
                        .eq(JobDescription::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (job == null) {
            throw new BusinessException("岗位不存在或无权限访问");
        }

        return job;
    }

    private AnalysisReport getUserReportOrThrow(Long reportId, Long userId) {
        AnalysisReport report = analysisReportMapper.selectOne(
                new LambdaQueryWrapper<AnalysisReport>()
                        .eq(AnalysisReport::getId, reportId)
                        .eq(AnalysisReport::getUserId, userId)
                        .eq(AnalysisReport::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (report == null) {
            throw new BusinessException("分析报告不存在或无权限访问");
        }

        return report;
    }

    private String buildCacheKey(Long userId, Long resumeId, Long jobId) {
        return "internpilot:analysis:%d:%d:%d".formatted(userId, resumeId, jobId);
    }

    private void normalizeAiResult(AiAnalysisResult result) {
        if (result.getMatchScore() == null) {
            result.setMatchScore(60);
        }

        if (result.getMatchScore() < 0) {
            result.setMatchScore(0);
        }

        if (result.getMatchScore() > 100) {
            result.setMatchScore(100);
        }

        if (!StringUtils.hasText(result.getMatchLevel())) {
            result.setMatchLevel(MatchLevelEnum.fromScore(result.getMatchScore()));
        }
    }

    private List<String> nullToEmpty(List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private AnalysisResultResponse toResultResponse(AnalysisReport report) {
        AnalysisResultResponse response = new AnalysisResultResponse();
        response.setReportId(report.getId());
        response.setResumeId(report.getResumeId());
        response.setJobId(report.getJobId());
        response.setMatchScore(report.getMatchScore());
        response.setMatchLevel(report.getMatchLevel());
        response.setStrengths(JsonUtils.fromJsonString(report.getStrengths(), List.class));
        response.setWeaknesses(JsonUtils.fromJsonString(report.getWeaknesses(), List.class));
        response.setMissingSkills(JsonUtils.fromJsonString(report.getMissingSkills(), List.class));
        response.setSuggestions(JsonUtils.fromJsonString(report.getSuggestions(), List.class));
        response.setInterviewTips(JsonUtils.fromJsonString(report.getInterviewTips(), List.class));
        response.setCacheHit(report.getCacheHit() != null && report.getCacheHit() == 1);
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private AnalysisReportListResponse toListResponse(AnalysisReport report) {
        AnalysisReportListResponse response = new AnalysisReportListResponse();
        response.setReportId(report.getId());
        response.setResumeId(report.getResumeId());
        response.setJobId(report.getJobId());
        response.setMatchScore(report.getMatchScore());
        response.setMatchLevel(report.getMatchLevel());
        response.setCacheHit(report.getCacheHit() != null && report.getCacheHit() == 1);
        response.setCreatedAt(report.getCreatedAt());

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        return response;
    }

    private AnalysisReportDetailResponse toDetailResponse(AnalysisReport report) {
        AnalysisReportDetailResponse response = new AnalysisReportDetailResponse();
        response.setReportId(report.getId());
        response.setResumeId(report.getResumeId());
        response.setJobId(report.getJobId());
        response.setMatchScore(report.getMatchScore());
        response.setMatchLevel(report.getMatchLevel());
        response.setStrengths(JsonUtils.fromJsonString(report.getStrengths(), List.class));
        response.setWeaknesses(JsonUtils.fromJsonString(report.getWeaknesses(), List.class));
        response.setMissingSkills(JsonUtils.fromJsonString(report.getMissingSkills(), List.class));
        response.setSuggestions(JsonUtils.fromJsonString(report.getSuggestions(), List.class));
        response.setInterviewTips(JsonUtils.fromJsonString(report.getInterviewTips(), List.class));
        response.setAiProvider(report.getAiProvider());
        response.setAiModel(report.getAiModel());
        response.setCacheHit(report.getCacheHit() != null && report.getCacheHit() == 1);
        response.setCreatedAt(report.getCreatedAt());

        Resume resume = resumeMapper.selectById(report.getResumeId());
        if (resume != null) {
            response.setResumeName(resume.getResumeName());
        }

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        return response;
    }
}
```

## 15. Controller 设计

### 15.1 `AnalysisController`

路径：

`src/main/java/com/internpilot/controller/AnalysisController.java`

```java
package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.service.AnalysisService;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 分析接口")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "简历岗位匹配分析", description = "根据用户简历和岗位 JD 生成 AI 匹配分析报告")
    @PostMapping("/match")
    public Result<AnalysisResultResponse> match(
            @RequestBody @Valid AnalysisMatchRequest request
    ) {
        return Result.success(analysisService.match(request));
    }

    @Operation(summary = "查询分析报告列表", description = "分页查询当前用户的历史 AI 分析报告")
    @GetMapping("/reports")
    public Result<PageResult<AnalysisReportListResponse>> listReports(
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(
                analysisService.listReports(resumeId, jobId, minScore, pageNum, pageSize)
        );
    }

    @Operation(summary = "查询分析报告详情", description = "查询当前用户某份 AI 分析报告的完整内容")
    @GetMapping("/reports/{id}")
    public Result<AnalysisReportDetailResponse> getReportDetail(@PathVariable Long id) {
        return Result.success(analysisService.getReportDetail(id));
    }
}
```

## 16. 接口设计

### 16.1 简历岗位匹配分析

基本信息：

| 项目 | 内容 |
|---|---|
| URL | `/api/analysis/match` |
| Method | `POST` |
| 权限 | `USER` |
| Content-Type | `application/json` |

请求参数：

```json
{
  "resumeId": 1,
  "jobId": 1,
  "forceRefresh": false
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 1,
    "resumeId": 1,
    "jobId": 1,
    "matchScore": 82,
    "matchLevel": "MEDIUM_HIGH",
    "strengths": [
      "具备 Spring Boot 项目经验",
      "熟悉 JWT 鉴权和权限控制",
      "有 MySQL 和 Redis 使用经验"
    ],
    "weaknesses": [
      "缺少真实企业实习经历",
      "项目中高并发和部署经验体现不足"
    ],
    "missingSkills": [
      "Docker",
      "Linux 部署",
      "消息队列"
    ],
    "suggestions": [
      "在项目描述中补充 Redis 缓存使用场景",
      "增加 Docker Compose 部署说明",
      "补充接口文档和测试说明"
    ],
    "interviewTips": [
      "准备 Spring Security 过滤链执行流程",
      "准备 JWT 登录流程",
      "准备 Redis 缓存穿透、击穿、雪崩问题"
    ],
    "cacheHit": false,
    "createdAt": "2026-05-06 21:00:00"
  }
}
```

### 16.2 查询分析报告列表

基本信息：

| 项目 | 内容 |
|---|---|
| URL | `/api/analysis/reports` |
| Method | `GET` |
| 权限 | `USER` |

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| resumeId | Long | 否 | 无 | 按简历筛选 |
| jobId | Long | 否 | 无 | 按岗位筛选 |
| minScore | Integer | 否 | 无 | 最低匹配分数 |
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 10 | 每页数量 |

### 16.3 查询分析报告详情

基本信息：

| 项目 | 内容 |
|---|---|
| URL | `/api/analysis/reports/{id}` |
| Method | `GET` |
| 权限 | `USER` |

## 17. 异常处理设计

### 17.1 常见异常

| 场景 | 错误信息 |
|---|---|
| 简历 ID 为空 | 简历 ID 不能为空 |
| 岗位 ID 为空 | 岗位 ID 不能为空 |
| 简历不存在 | 简历不存在或无权限访问 |
| 岗位不存在 | 岗位不存在或无权限访问 |
| 简历文本为空 | 简历解析文本为空 |
| 岗位 JD 为空 | 岗位 JD 不能为空 |
| AI API Key 未配置 | AI API Key 未配置 |
| AI 调用失败 | AI 服务调用失败 |
| AI 返回为空 | AI 返回内容为空 |
| AI 返回非 JSON | AI 返回结果解析失败 |

### 17.2 `AiServiceException`

路径：

`src/main/java/com/internpilot/exception/AiServiceException.java`

```java
package com.internpilot.exception;

import com.internpilot.common.ResultCode;

public class AiServiceException extends BusinessException {

    public AiServiceException(String message) {
        super(ResultCode.AI_SERVICE_ERROR, message);
    }
}
```

### 17.3 全局异常响应

AI 服务异常统一返回：

```json
{
  "code": 600,
  "message": "AI 分析服务暂时不可用，请稍后重试",
  "data": null
}
```

或者返回具体错误：

```json
{
  "code": 600,
  "message": "AI 返回结果解析失败",
  "data": null
}
```

## 18. 数据权限设计

### 18.1 核心原则

AI 分析必须保证：

- `resume.user_id == 当前登录用户ID`
- `job.user_id == 当前登录用户ID`
- `analysis_report.user_id == 当前登录用户ID`

### 18.2 为什么必须校验

如果只传 `resumeId` 和 `jobId`，用户可能构造请求：

```json
{
  "resumeId": 999,
  "jobId": 888
}
```

如果后端没有校验 `user_id`，就可能拿到别人的简历和岗位 JD，造成严重隐私泄露。

### 18.3 正确做法

查询简历：

```java
Resume resume = resumeMapper.selectOne(
    new LambdaQueryWrapper<Resume>()
        .eq(Resume::getId, resumeId)
        .eq(Resume::getUserId, currentUserId)
        .eq(Resume::getDeleted, 0)
);
```

查询岗位：

```java
JobDescription job = jobDescriptionMapper.selectOne(
    new LambdaQueryWrapper<JobDescription>()
        .eq(JobDescription::getId, jobId)
        .eq(JobDescription::getUserId, currentUserId)
        .eq(JobDescription::getDeleted, 0)
);
```

## 19. 日志设计

### 19.1 建议记录

AI 分析模块可以记录：

1. 用户 ID；
2. `resumeId`；
3. `jobId`；
4. 是否命中缓存；
5. AI 调用耗时；
6. AI 调用是否成功；
7. AI 返回解析是否成功。

### 19.2 不建议记录

不要在日志中记录：

1. 完整简历文本；
2. 完整岗位 JD；
3. AI API Key；
4. 完整 JWT Token；
5. 用户隐私信息。

## 20. 测试流程

### 20.1 测试前置条件

1. 用户认证模块完成；
2. 简历上传模块完成；
3. 岗位 JD 模块完成；
4. Redis 正常运行；
5. `analysis_report` 表已创建；
6. 已经上传至少一份简历；
7. 已经创建至少一个岗位 JD；
8. 已获得 JWT Token。

### 20.2 推荐先使用 `MockAiClient`

开发初期建议先使用 `MockAiClient`。

在配置中设置：

```yaml
spring:
  profiles:
    active: mock
```

或者启动参数：

```bash
./gradlew bootRun --args='--spring.profiles.active=mock'
```

这样可以不依赖真实 AI API，先跑通业务闭环。

### 20.3 测试顺序

1. 登录获取 Token；
2. 上传简历；
3. 创建岗位 JD；
4. 调用 `/api/analysis/match`；
5. 查看是否返回匹配分数；
6. 查看 `analysis_report` 表是否插入数据；
7. 再次调用相同 `resumeId + jobId`；
8. 验证是否命中 Redis 缓存；
9. 使用 `forceRefresh=true` 再次调用；
10. 验证是否重新生成报告；
11. 查询分析报告列表；
12. 查询分析报告详情；
13. 使用不存在的 `resumeId` 测试异常；
14. 使用不存在的 `jobId` 测试异常；
15. 不带 Token 测试 401。

## 21. PowerShell 测试示例

### 21.1 登录获取 Token

```powershell
$body = @{
  username = "wan"
  password = "123456"
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

$token = $response.data.token
```

### 21.2 发起 AI 匹配分析

```powershell
$body = @{
  resumeId = 1
  jobId = 1
  forceRefresh = $false
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/analysis/match" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

### 21.3 强制重新分析

```powershell
$body = @{
  resumeId = 1
  jobId = 1
  forceRefresh = $true
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/analysis/match" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

### 21.4 查询报告列表

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/analysis/reports?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 21.5 查询报告详情

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/analysis/reports/1" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

## 22. Swagger 测试流程

访问：

```text
http://localhost:8080/doc.html
```

测试步骤：

1. 调用登录接口；
2. 复制 Token；
3. 点击 Authorize；
4. 输入 Bearer Token；
5. 打开 AI 分析接口；
6. 输入 `resumeId` 和 `jobId`；
7. 调用匹配分析接口；
8. 查看返回结果；
9. 调用报告列表接口；
10. 调用报告详情接口。

## 23. 常见问题与解决方案

### 23.1 AI API Key 未配置

错误：

```text
AI API Key 未配置
```

解决：

PowerShell 设置环境变量：

```powershell
$env:AI_API_KEY="你的API Key"
```

然后重新启动项目。

### 23.2 AI 返回结果解析失败

原因：

1. AI 返回了 Markdown；
2. AI 返回了自然语言解释；
3. AI 返回 JSON 字段缺失；
4. AI 返回数组格式不正确；
5. Prompt 约束不够强。

解决：

1. Prompt 中强调“必须返回 JSON”；
2. 不要让 AI 返回 Markdown；
3. 增加 `extractJson` 清洗；
4. 保存 `raw_ai_response` 方便排查。

### 23.3 第二次分析没有命中缓存

可能原因：

1. `forceRefresh=true`；
2. Redis 没启动；
3. 缓存 Key 不一致；
4. `response` 没有正确序列化；
5. TTL 已过期。

检查 Redis Key：

```bash
redis-cli keys "internpilot:analysis:*"
```

### 23.4 查询报告详情报错

可能原因：

1. `reportId` 不存在；
2. `reportId` 属于其他用户；
3. 报告已逻辑删除；
4. Token 对应用户不对。

### 23.5 AI 调用很慢

原因：

1. 外部 API 响应慢；
2. 网络不稳定；
3. Prompt 太长；
4. 简历解析文本太长；
5. 岗位 JD 太长。

优化：

1. 限制 `parsedText` 长度；
2. 限制 `jdContent` 长度；
3. 使用 Redis 缓存；
4. 后续改为异步任务；
5. 后续使用 WebSocket 返回进度。

## 24. 后续增强：AI 调用日志

第二阶段可以增加 `ai_call_log` 表，记录：

1. 用户 ID；
2. 模型名称；
3. 请求类型；
4. 是否成功；
5. 错误原因；
6. 调用耗时；
7. token 消耗。

用途：

1. 排查问题；
2. 控制成本；
3. 分析调用量；
4. 做管理员看板。

## 25. 后续增强：面试题生成

可以新增接口：

```text
POST /api/analysis/interview-questions
```

输入：

```json
{
  "resumeId": 1,
  "jobId": 1
}
```

输出：

```json
{
  "basicQuestions": [
    "请解释 HashMap 的底层原理",
    "请介绍 JVM 内存模型"
  ],
  "frameworkQuestions": [
    "Spring Boot 自动配置原理是什么",
    "Spring Security 过滤链怎么执行"
  ],
  "projectQuestions": [
    "你的校园搭子系统中 JWT 是怎么实现的",
    "你为什么使用 Redis"
  ],
  "databaseQuestions": [
    "MySQL 索引失效有哪些场景"
  ]
}
```

## 26. 后续增强：异步分析任务

AI 分析可能比较慢，后续可以改成异步。

流程：

```text
用户提交分析请求
  ↓
后端创建 analysis_task
  ↓
立即返回 taskId
  ↓
后台线程或消息队列执行 AI 分析
  ↓
前端轮询或 WebSocket 获取进度
  ↓
分析完成后返回报告
```

好处：

1. 不阻塞 HTTP 请求；
2. 前端体验更好；
3. 后续可以显示分析进度；
4. 可以更好地处理超时。

## 27. 后续增强：WebSocket 实时进度

可以设计分析进度：

1. 10% 正在读取简历；
2. 30% 正在读取岗位 JD；
3. 50% 正在调用 AI 分析；
4. 80% 正在解析分析结果；
5. 100% 分析完成。

这会让项目展示效果更强。

## 28. 后续增强：RAG 岗位知识库

后续可以加入岗位知识库：

```text
岗位 JD
  ↓
提取岗位技能关键词
  ↓
查询岗位知识库
  ↓
召回相关技能说明和面试重点
  ↓
与简历文本一起输入 AI
  ↓
生成更专业的分析报告
```

可选技术：

1. 向量数据库；
2. Embedding；
3. Elasticsearch；
4. 本地知识库 Markdown；
5. Spring AI。

第一阶段不建议做 RAG，先完成基础 AI 分析闭环。

## 29. 面试讲解准备

### 29.1 面试官可能问：AI 分析模块怎么实现？

回答思路：

用户选择简历和岗位后，后端会先校验这份简历和岗位是否属于当前登录用户，防止越权访问。
然后读取简历解析文本和岗位 JD，构造 Prompt，调用大语言模型 API。
Prompt 中要求模型严格返回 JSON，包括匹配分数、优势、短板、缺失技能、简历优化建议和面试准备建议。
后端解析 JSON 后，将结果保存到 `analysis_report` 表，并写入 Redis 缓存。
如果下次相同简历和岗位再次分析，就优先返回缓存结果，减少重复调用 AI。

### 29.2 面试官可能问：为什么要用 Redis 缓存？

回答思路：

AI 分析接口相比普通查询接口更慢，而且可能有调用成本。
对于同一用户、同一简历、同一岗位，在短时间内重复分析通常没有必要重新调用模型。
所以我使用 Redis 缓存分析结果，Key 由 `userId`、`resumeId`、`jobId` 组成。
如果缓存命中，直接返回结果；如果用户选择 `forceRefresh`，才强制重新调用 AI。

### 29.3 面试官可能问：AI 返回格式不稳定怎么办？

回答思路：

我从三个层面处理。
第一，在 Prompt 中明确要求必须返回 JSON，不返回 Markdown 和额外解释。
第二，后端对 AI 返回文本做简单清洗，比如去掉代码块，只截取第一个大括号到最后一个大括号之间的内容。
第三，使用 Jackson 解析 JSON，如果解析失败，会抛出 AI 服务异常，同时保存 `raw_ai_response` 方便排查。

### 29.4 面试官可能问：如何防止用户分析别人的简历？

回答思路：

AI 分析接口不能只相信前端传来的 `resumeId` 和 `jobId`。
后端会根据当前 JWT 解析出的 `userId`，同时查询 `resumeId + userId` 和 `jobId + userId`。
只有简历和岗位都属于当前用户，才允许进入 AI 分析流程。
否则返回“资源不存在或无权限访问”。

### 29.5 面试官可能问：如果 AI 调用超时怎么办？

回答思路：

第一阶段会捕获 AI 调用异常，返回统一的 AI 服务异常响应。
同时使用 Redis 缓存降低重复调用次数。
后续可以进一步优化为异步任务模式，请求先返回 `taskId`，后台执行 AI 分析，前端通过轮询或 WebSocket 获取分析进度和结果。

## 30. 开发顺序建议

AI 分析模块建议按以下顺序开发：

1. 创建 `analysis_report` 表；
2. 创建 `AnalysisReport` Entity；
3. 创建 `AnalysisReportMapper`；
4. 创建 `MatchLevelEnum`；
5. 创建 `AiProperties`；
6. 创建 `AiClient` 接口；
7. 创建 `MockAiClient`；
8. 创建 `AiAnalysisResult`；
9. 创建 `JsonUtils`；
10. 创建 `PromptUtils`；
11. 创建 `AnalysisMatchRequest`；
12. 创建 `AnalysisResultResponse`；
13. 创建 `AnalysisReportListResponse`；
14. 创建 `AnalysisReportDetailResponse`；
15. 创建 `AnalysisService`；
16. 实现 `AnalysisServiceImpl`；
17. 创建 `AnalysisController`；
18. 使用 `MockAiClient` 跑通分析接口；
19. 测试分析报告入库；
20. 测试 Redis 缓存；
21. 测试 `forceRefresh`；
22. 再接入真实 DeepSeek / OpenAI / Qwen API；
23. 测试 AI 返回解析；
24. 测试异常场景。

## 31. 验收标准

### 31.1 分析接口验收

- 登录用户可以发起 AI 分析；
- 未登录用户不能发起 AI 分析；
- `resumeId` 不能为空；
- `jobId` 不能为空；
- 简历必须属于当前用户；
- 岗位必须属于当前用户；
- 简历 `parsedText` 不能为空；
- 岗位 `jdContent` 不能为空；
- 系统可以返回匹配分数；
- 系统可以返回优势、短板、缺失技能、建议；
- 分析报告可以保存到数据库。

### 31.2 缓存验收

- 第一次分析调用 AI；
- 第二次相同简历和岗位分析命中缓存；
- `cacheHit` 能正确返回；
- `forceRefresh=true` 可以强制重新分析；
- Redis 中能看到分析缓存 Key。

### 31.3 报告查询验收

- 用户可以查询自己的分析报告列表；
- 用户可以查询自己的分析报告详情；
- 用户不能查询他人的报告；
- 列表支持 `resumeId` 筛选；
- 列表支持 `jobId` 筛选；
- 列表支持 `minScore` 筛选。

### 31.4 异常验收

- AI API Key 未配置时返回友好错误；
- AI 返回非 JSON 时返回友好错误；
- AI 调用失败时返回统一错误；
- 不打印完整简历和 API Key；
- 资源不存在时返回明确错误。

## 32. 模块设计结论

AI 分析模块是 InternPilot 的核心亮点模块，负责将用户简历和岗位 JD 转化为结构化的匹配分析报告。

第一阶段采用：

```text
简历 parsedText + 岗位 jdContent + Prompt 模板 + 大模型 API + JSON 结构化输出 + MySQL 报告存储 + Redis 缓存
```

核心流程为：

```text
用户选择简历和岗位
  ↓
校验数据权限
  ↓
读取 parsedText 和 jdContent
  ↓
检查 Redis 缓存
  ↓
构造 Prompt
  ↓
调用 AI
  ↓
解析 JSON
  ↓
保存 analysis_report
  ↓
返回分析结果
```

该设计既能满足 MVP 阶段的核心业务需求，也能展示 Java 后端项目中 AI 应用集成、Prompt Engineering、Redis 缓存、异常处理、数据权限控制和工程化设计能力。

后续可以继续扩展为异步 AI 分析任务、WebSocket 实时进度、AI 调用日志、多模型切换、面试题生成和 RAG 岗位知识库。
