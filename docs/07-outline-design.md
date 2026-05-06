# InternPilot 概要设计文档

## 1. 文档说明

本文档用于描述 InternPilot 系统的总体设计方案，包括系统总体架构、后端分层架构、功能模块设计、核心业务流程设计、数据库概要设计、认证授权设计、AI 分析设计、文件上传解析设计、Redis 缓存设计、异常处理设计和项目包结构设计。

InternPilot 是一个面向大学生实习求职场景的 AI 实习投递与简历优化平台，系统核心目标是实现：

1. 用户注册登录；
2. 简历上传与解析；
3. 岗位 JD 管理；
4. AI 简历岗位匹配分析；
5. 分析报告管理；
6. 投递记录管理；
7. Redis 缓存优化；
8. Swagger 接口文档；
9. 后续 Docker 部署。

---

## 2. 系统总体架构设计

### 2.1 总体架构说明

InternPilot 采用典型的前后端分离架构。

系统主要由以下部分组成：

1. 前端应用；
2. 后端服务；
3. MySQL 数据库；
4. Redis 缓存；
5. 文件存储；
6. 大语言模型 API；
7. Swagger 接口文档；
8. 后续 Docker 部署环境。

系统第一阶段以后端为主，前端可以先使用简单页面或接口工具进行联调，待后端 MVP 完成后再补充 Vue 前端。

### 2.2 系统总体架构图

```text
┌────────────────────────────────────┐
│              前端应用               │
│       Vue 3 / Element Plus          │
└─────────────────┬──────────────────┘
                  │ HTTP / HTTPS
                  │ Authorization: Bearer Token
                  ↓
┌────────────────────────────────────┐
│           Spring Boot 后端           │
│                                    │
│  Controller 层                      │
│  Service 层                         │
│  Mapper 层                          │
│  Security 认证鉴权层                 │
│  AI Analysis Service                │
│  File Parse Service                 │
└───────┬──────────┬──────────┬───────┘
        │          │          │
        ↓          ↓          ↓
┌────────────┐ ┌────────────┐ ┌───────────────┐
│   MySQL    │ │   Redis    │ │  File Storage │
│ 业务数据    │ │ 缓存数据    │ │  简历文件       │
└────────────┘ └────────────┘ └───────────────┘
        │
        ↓
┌────────────────────────────────────┐
│        Large Language Model API     │
│   DeepSeek / OpenAI / Qwen API      │
└────────────────────────────────────┘
```

### 2.3 系统调用关系

系统核心调用链如下：

```text
前端页面
  ↓
Controller 接收请求
  ↓
Spring Security 校验 Token
  ↓
Service 执行业务逻辑
  ↓
Mapper 访问 MySQL
  ↓
必要时访问 Redis
  ↓
必要时访问文件存储
  ↓
必要时调用 AI API
  ↓
返回统一响应结果
```

---

## 3. 技术架构设计

### 3.1 后端技术栈

| 技术 | 用途 |
|---|---|
| Java 17 | 后端开发语言 |
| Spring Boot 3.x | 后端主框架 |
| Spring Security 6.x | 用户认证与接口鉴权 |
| JWT | 无状态登录认证 |
| MyBatis Plus / MyBatis | 数据持久层 |
| MySQL 8.x | 关系型数据库 |
| Redis 7.x | AI 分析结果缓存 |
| Swagger / Knife4j | 接口文档 |
| Lombok | 简化实体类代码 |
| Hibernate Validator | 参数校验 |
| Apache PDFBox | PDF 简历解析 |
| Apache POI | DOCX 简历解析 |
| Docker Compose | 后续部署 |

### 3.2 前端技术栈

| 技术 | 用途 |
|---|---|
| Vue 3 | 前端框架 |
| Vite | 构建工具 |
| Element Plus | UI 组件库 |
| Axios | 请求后端接口 |
| Pinia | 状态管理 |
| Vue Router | 路由管理 |

### 3.3 AI 技术栈

| 技术 | 用途 |
|---|---|
| DeepSeek API / OpenAI API / Qwen API | 大模型分析能力 |
| Prompt Template | 规范 AI 输入 |
| JSON Structured Output | 规范 AI 输出 |
| Redis Cache | 缓存分析结果 |
| AI Call Log | 记录调用情况，后续扩展 |

---

## 4. 后端分层架构设计

### 4.1 分层架构

后端采用经典三层架构，并结合安全层、通用层、AI 服务层和文件服务层。

```text
Controller 层
  ↓
Service 层
  ↓
Mapper 层
  ↓
Database
```

扩展层：

1. Security 层：负责登录认证、Token 校验、权限控制；
2. Common 层：统一响应、异常处理、常量、枚举；
3. AI 层：负责 Prompt 构造、AI API 调用、AI 结果解析；
4. File 层：负责简历文件存储和文本解析；
5. Config 层：负责系统配置。

### 4.2 各层职责

| 层 | 职责 |
|---|---|
| Controller | 接收 HTTP 请求、参数校验、返回响应 |
| Service | 处理核心业务逻辑 |
| Mapper | 操作数据库 |
| Entity | 数据库实体映射 |
| DTO | 接收前端请求参数 |
| VO | 返回前端展示数据 |
| Security | JWT 认证、鉴权、用户上下文 |
| Common | 统一响应、异常、常量、枚举 |
| AI Service | 构建 Prompt、调用模型、解析结果 |
| File Service | 文件保存、文件解析、文件校验 |
| Config | Redis、Swagger、安全、文件上传等配置 |

---

## 5. 系统功能模块设计

### 5.1 模块总览

系统第一阶段主要包含以下模块：

| 模块编号 | 模块名称 | 优先级 | 说明 |
|---|---|---|---|
| M-01 | 用户认证模块 | P0 | 注册、登录、JWT 鉴权 |
| M-02 | 用户信息模块 | P0 | 查询和修改当前用户信息 |
| M-03 | 简历管理模块 | P0 | 上传、解析、查询、删除、默认简历 |
| M-04 | 岗位 JD 管理模块 | P0 | 创建、查询、修改、删除岗位 |
| M-05 | AI 分析模块 | P0 | 简历与岗位匹配分析 |
| M-06 | 分析报告模块 | P0 | 查询历史分析报告 |
| M-07 | 投递记录模块 | P0 | 创建、查询、修改状态、备注 |
| M-08 | Redis 缓存模块 | P1 | 缓存 AI 分析结果 |
| M-09 | 数据看板模块 | P1 | 投递数据统计 |
| M-10 | 管理员模块 | P2 | 用户管理、模型配置、Prompt 管理 |

---

## 6. 用户认证模块概要设计

### 6.1 模块职责

用户认证模块负责：

1. 用户注册；
2. 用户登录；
3. 密码加密；
4. JWT 生成；
5. JWT 校验；
6. 当前用户上下文获取；
7. 接口访问权限控制。

### 6.2 登录认证流程

```text
用户提交用户名和密码
  ↓
AuthController 接收请求
  ↓
AuthService 查询用户
  ↓
PasswordEncoder 校验密码
  ↓
校验成功
  ↓
JwtTokenProvider 生成 Token
  ↓
返回 LoginResponse
```

### 6.3 JWT 鉴权流程

```text
用户访问业务接口
  ↓
请求头携带 Authorization: Bearer Token
  ↓
JwtAuthenticationFilter 拦截请求
  ↓
解析 Token
  ↓
校验 Token 是否有效
  ↓
从 Token 中获取 userId / username / role
  ↓
构造 Authentication 对象
  ↓
放入 SecurityContext
  ↓
请求进入 Controller
```

### 6.4 安全设计要点

1. 密码使用 BCrypt 加密；
2. Token 设置过期时间；
3. 注册和登录接口放行；
4. Swagger 接口文档放行；
5. 业务接口必须认证；
6. 管理员接口必须具备 ADMIN 角色；
7. 通过当前登录用户 ID 限制数据访问范围。

---

## 7. 简历管理模块概要设计

### 7.1 模块职责

简历管理模块负责：

1. 简历文件上传；
2. 文件类型校验；
3. 文件大小校验；
4. 文件保存；
5. 简历文本解析；
6. 简历信息入库；
7. 简历列表查询；
8. 简历详情查询；
9. 简历删除；
10. 设置默认简历。

### 7.2 简历上传解析流程

```text
用户上传简历
  ↓
ResumeController 接收 MultipartFile
  ↓
校验用户是否登录
  ↓
校验文件是否为空
  ↓
校验文件类型 PDF / DOCX
  ↓
校验文件大小
  ↓
FileStorageService 保存文件
  ↓
ResumeParseService 解析文本
  ↓
ResumeService 保存简历元数据和 parsedText
  ↓
返回上传结果
```

### 7.3 文件解析策略

| 文件类型 | 解析工具 | 说明 |
|---|---|---|
| PDF | Apache PDFBox | 提取 PDF 文本 |
| DOCX | Apache POI | 提取 Word 文本 |
| TXT | Java IO | 后续扩展 |
| Markdown | Java IO | 后续扩展 |

### 7.4 文件存储策略

第一阶段建议使用本地文件存储。

目录示例：

```text
uploads/
  resumes/
    user-1/
      20260505_resume.pdf
    user-2/
      20260505_resume.docx
```

后续可扩展为：

```text
MinIO / 阿里云 OSS / 腾讯云 COS
```

### 7.5 简历数据权限

所有简历操作必须满足：

```text
resume.user_id == currentUserId
```

用户不能查询、修改、删除其他用户的简历。

---

## 8. 岗位 JD 管理模块概要设计

### 8.1 模块职责

岗位 JD 管理模块负责：

1. 创建岗位；
2. 查询岗位列表；
3. 查询岗位详情；
4. 修改岗位；
5. 删除岗位；
6. 支持关键词搜索；
7. 支持岗位类型筛选。

### 8.2 岗位创建流程

```text
用户填写岗位信息
  ↓
JobController 接收请求
  ↓
校验用户登录状态
  ↓
校验 companyName / jobTitle / jdContent
  ↓
绑定当前用户 ID
  ↓
JobService 保存岗位
  ↓
返回岗位 ID
```

### 8.3 岗位数据权限

所有岗位操作必须满足：

```text
job.user_id == currentUserId
```

用户不能访问其他用户保存的岗位 JD。

### 8.4 岗位删除策略

第一阶段建议采用逻辑删除，避免影响历史分析报告和投递记录。

字段设计：

```text
deleted = 0 / 1
```

查询岗位列表时只查询：

```text
deleted = 0
```

---

## 9. AI 分析模块概要设计

### 9.1 模块职责

AI 分析模块是系统核心模块，负责：

1. 获取简历解析文本；
2. 获取岗位 JD；
3. 校验数据权限；
4. 检查 Redis 缓存；
5. 构造 Prompt；
6. 调用大语言模型 API；
7. 解析 AI 返回内容；
8. 保存分析报告；
9. 返回结构化分析结果。

### 9.2 AI 分析整体流程

```text
用户选择 resumeId 和 jobId
  ↓
AnalysisController 接收请求
  ↓
AnalysisService 校验简历是否存在且属于当前用户
  ↓
AnalysisService 校验岗位是否存在且属于当前用户
  ↓
检查 Redis 缓存
  ↓
缓存命中：返回缓存报告
  ↓
缓存未命中：
      ↓
  构造 Prompt
      ↓
  调用 AI API
      ↓
  解析 AI 返回 JSON
      ↓
  保存 analysis_report
      ↓
  写入 Redis
      ↓
  返回分析结果
```

### 9.3 Prompt 构造设计

Prompt 由四部分组成：

1. 系统角色说明；
2. 用户简历文本；
3. 岗位 JD 文本；
4. 输出格式约束。

示例结构：

```text
你是一个资深技术招聘官和简历优化专家。
请根据下面的学生简历和岗位 JD，分析匹配程度。

要求：
1. 给出 0-100 的匹配分数；
2. 给出匹配等级；
3. 分析简历优势；
4. 分析简历短板；
5. 提取缺失技能；
6. 给出简历优化建议；
7. 给出面试准备建议；
8. 必须返回 JSON，不要返回多余解释。

【简历文本】
{resumeText}

【岗位 JD】
{jobDescription}

【返回 JSON 格式】
{
  "matchScore": 85,
  "matchLevel": "HIGH",
  "strengths": [],
  "weaknesses": [],
  "missingSkills": [],
  "suggestions": [],
  "interviewTips": []
}
```

### 9.4 AI 返回结果设计

AI 分析结果需要转换为系统内部对象：`AnalysisResultDTO`。

核心字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| matchScore | Integer | 匹配分数 |
| matchLevel | String | 匹配等级 |
| strengths | List<String> | 优势 |
| weaknesses | List<String> | 短板 |
| missingSkills | List<String> | 缺失技能 |
| suggestions | List<String> | 优化建议 |
| interviewTips | List<String> | 面试建议 |
| rawResponse | String | AI 原始返回 |

### 9.5 AI 调用异常处理

可能异常包括：

| 异常 | 处理方式 |
|---|---|
| API Key 未配置 | 返回系统配置错误 |
| AI 服务超时 | 返回 AI 服务暂时不可用 |
| AI 返回空 | 返回分析失败 |
| AI 返回非 JSON | 保存 rawResponse，返回解析失败 |
| 网络异常 | 返回外部服务调用失败 |
| 调用次数超限 | 返回 AI 服务额度不足 |

### 9.6 AI 模块扩展设计

后续可以将 AI 模块拆成统一接口：

```java
public interface AiClient {
    String chat(String prompt);
}
```

不同模型实现：

```text
DeepSeekAiClient
OpenAiClient
QwenAiClient
MockAiClient
```

这样后续可以切换模型，不影响业务层。

---

## 10. 分析报告模块概要设计

### 10.1 模块职责

分析报告模块负责：

1. 保存 AI 分析结果；
2. 查询历史分析报告；
3. 查询分析报告详情；
4. 按简历筛选报告；
5. 按岗位筛选报告；
6. 按匹配分数筛选报告。

### 10.2 分析报告保存策略

每次 AI 分析成功后，都保存一条报告记录。

保存字段包括：

1. userId；
2. resumeId；
3. jobId；
4. matchScore；
5. matchLevel；
6. strengths；
7. weaknesses；
8. missingSkills；
9. suggestions；
10. interviewTips；
11. rawAiResponse；
12. createdAt。

### 10.3 报告数据权限

所有报告操作必须满足：

```text
analysis_report.user_id == currentUserId
```

用户不能查看他人的分析报告。

---

## 11. 投递记录模块概要设计

### 11.1 模块职责

投递记录模块负责：

1. 创建投递记录；
2. 查询投递记录列表；
3. 查询投递记录详情；
4. 修改投递状态；
5. 修改备注；
6. 删除投递记录；
7. 后续统计投递数据。

### 11.2 投递记录状态设计

投递状态使用枚举管理。

```text
TO_APPLY          待投递
APPLIED           已投递
WRITTEN_TEST      笔试中
FIRST_INTERVIEW   一面
SECOND_INTERVIEW  二面
HR_INTERVIEW      HR 面
OFFER             已 Offer
REJECTED          被拒
GIVEN_UP          放弃
```

### 11.3 投递记录创建流程

```text
用户选择岗位
  ↓
可选选择简历
  ↓
填写投递状态、日期、备注
  ↓
ApplicationController 接收请求
  ↓
校验岗位属于当前用户
  ↓
如果填写 resumeId，校验简历属于当前用户
  ↓
保存投递记录
  ↓
返回创建结果
```

### 11.4 状态修改流程

```text
用户修改状态
  ↓
校验投递记录存在
  ↓
校验投递记录属于当前用户
  ↓
校验状态是否合法
  ↓
更新状态和更新时间
  ↓
返回修改成功
```

### 11.5 数据权限

所有投递记录操作必须满足：

```text
application_record.user_id == currentUserId
```

---

## 12. Redis 缓存概要设计

### 12.1 缓存目标

Redis 第一阶段主要用于缓存 AI 分析结果，解决两个问题：

1. 避免相同简历和岗位重复调用 AI；
2. 提高重复分析接口响应速度；
3. 降低 AI API 调用成本。

### 12.2 缓存 Key 设计

```text
internpilot:analysis:{userId}:{resumeId}:{jobId}
```

示例：

```text
internpilot:analysis:1:10:25
```

### 12.3 缓存 Value 设计

缓存内容为分析结果 JSON。

示例：

```json
{
  "reportId": 1001,
  "matchScore": 82,
  "matchLevel": "MEDIUM_HIGH",
  "strengths": ["有 Spring Boot 项目经验", "熟悉 JWT"],
  "weaknesses": ["缺少实习经历"],
  "missingSkills": ["Docker", "消息队列"],
  "suggestions": ["补充部署经验", "强化 Redis 使用场景"],
  "interviewTips": ["准备 Spring Security 过滤链", "准备 Redis 缓存一致性问题"]
}
```

### 12.4 缓存过期时间

第一阶段建议：24 小时。

后续可以调整为：7 天。

### 12.5 缓存失效策略

以下情况建议清除缓存：

1. 简历 parsedText 修改；
2. 岗位 jdContent 修改；
3. 用户主动重新分析；
4. 缓存自然过期。

第一阶段可以先依赖过期时间，后续再实现主动删除。

---

## 13. 数据库概要设计

### 13.1 核心数据表

第一阶段核心数据表：

| 表名 | 说明 |
|---|---|
| user | 用户表 |
| resume | 简历表 |
| job_description | 岗位 JD 表 |
| analysis_report | AI 分析报告表 |
| application_record | 投递记录表 |

### 13.2 表关系概要

```text
User 1 —— N Resume
User 1 —— N JobDescription
User 1 —— N AnalysisReport
User 1 —— N ApplicationRecord

Resume 1 —— N AnalysisReport
JobDescription 1 —— N AnalysisReport

Resume 1 —— N ApplicationRecord
JobDescription 1 —— N ApplicationRecord
```

### 13.3 数据库 ER 图文本版

```text
┌────────────┐
│    user    │
└─────┬──────┘
      │ 1
      │
      │ N
┌─────▼──────┐
│   resume   │
└─────┬──────┘
      │ 1
      │
      │ N
┌─────▼────────────┐
│ analysis_report  │
└─────▲────────────┘
      │ N
      │
      │ 1
┌─────┴────────────┐
│ job_description  │
└─────▲────────────┘
      │ 1
      │
      │ N
┌─────┴──────────────┐
│ application_record │
└────────────────────┘
```

说明：

1. 一个用户可以上传多份简历；
2. 一个用户可以创建多个岗位 JD；
3. 一份简历可以生成多份分析报告；
4. 一个岗位可以生成多份分析报告；
5. 一个岗位可以关联多条投递记录；
6. 一条投递记录可以关联一份简历。

---

## 14. 接口概要设计

### 14.1 接口设计原则

1. 使用 RESTful 风格；
2. URL 使用名词复数；
3. 使用统一响应结构；
4. 业务接口需要 JWT；
5. 管理员接口使用 `/api/admin` 前缀；
6. 参数使用 DTO 接收；
7. 响应使用 VO 返回；
8. 接口文档使用 Swagger / Knife4j 生成。

### 14.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

异常响应：

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

### 14.3 核心接口概览

#### 用户认证接口

| 方法 | URL | 说明 |
|---|---|---|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| GET | /api/user/me | 当前用户信息 |
| PUT | /api/user/me | 修改当前用户信息 |

#### 简历接口

| 方法 | URL | 说明 |
|---|---|---|
| POST | /api/resumes/upload | 上传简历 |
| GET | /api/resumes | 查询简历列表 |
| GET | /api/resumes/{id} | 查询简历详情 |
| DELETE | /api/resumes/{id} | 删除简历 |
| PUT | /api/resumes/{id}/default | 设置默认简历 |

#### 岗位接口

| 方法 | URL | 说明 |
|---|---|---|
| POST | /api/jobs | 创建岗位 |
| GET | /api/jobs | 查询岗位列表 |
| GET | /api/jobs/{id} | 查询岗位详情 |
| PUT | /api/jobs/{id} | 修改岗位 |
| DELETE | /api/jobs/{id} | 删除岗位 |

#### AI 分析接口

| 方法 | URL | 说明 |
|---|---|---|
| POST | /api/analysis/match | 简历岗位匹配分析 |
| GET | /api/analysis/reports | 查询报告列表 |
| GET | /api/analysis/reports/{id} | 查询报告详情 |
| POST | /api/analysis/interview-questions | 生成面试题，后续扩展 |

#### 投递记录接口

| 方法 | URL | 说明 |
|---|---|---|
| POST | /api/applications | 创建投递记录 |
| GET | /api/applications | 查询投递记录列表 |
| GET | /api/applications/{id} | 查询投递详情 |
| PUT | /api/applications/{id}/status | 修改投递状态 |
| PUT | /api/applications/{id}/note | 修改备注 |
| DELETE | /api/applications/{id} | 删除投递记录 |

---

## 15. 异常处理概要设计

### 15.1 统一异常处理

系统使用全局异常处理器：`GlobalExceptionHandler`。

统一处理：

1. 参数校验异常；
2. 业务异常；
3. 登录认证异常；
4. 权限不足异常；
5. 文件上传异常；
6. AI 调用异常；
7. 系统未知异常。

### 15.2 异常类型设计

| 异常类 | 说明 |
|---|---|
| BusinessException | 业务异常 |
| UnauthorizedException | 未登录或 Token 无效 |
| ForbiddenException | 无权限访问 |
| ResourceNotFoundException | 资源不存在 |
| FileParseException | 文件解析失败 |
| AiServiceException | AI 服务异常 |

### 15.3 错误码设计

| 错误码 | 说明 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或 Token 无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 数据冲突 |
| 500 | 系统异常 |
| 600 | AI 服务异常 |
| 700 | 文件处理异常 |

---

## 16. 参数校验设计

### 16.1 校验方式

使用 Hibernate Validator。

常见注解：

```text
@NotBlank
@NotNull
@Email
@Size
@Min
@Max
@Pattern
```

### 16.2 DTO 校验示例

注册请求：

```java
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6 到 20 位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

---

## 17. 日志设计

### 17.1 日志目标

系统日志用于：

1. 排查接口异常；
2. 记录 AI 调用情况；
3. 分析系统运行状态；
4. 辅助定位文件解析失败问题。

### 17.2 日志内容

建议记录：

1. 用户登录失败原因；
2. 文件上传失败原因；
3. AI 调用耗时；
4. AI 调用失败原因；
5. 关键业务异常；
6. 系统启动和配置加载状态。

### 17.3 日志注意事项

不得打印：

1. 用户密码；
2. JWT 完整 Token；
3. AI API Key；
4. 用户完整简历隐私内容；
5. 其他敏感信息。

---

## 18. 项目包结构设计

### 18.1 推荐包结构

```text
com.internpilot
├── InternPilotApplication.java
│
├── common
│   ├── Result.java
│   ├── ResultCode.java
│   ├── PageResult.java
│   └── constants
│
├── config
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── RedisConfig.java
│   ├── WebConfig.java
│   └── FileStorageConfig.java
│
├── security
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtAccessDeniedHandler.java
│
├── controller
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ResumeController.java
│   ├── JobController.java
│   ├── AnalysisController.java
│   └── ApplicationController.java
│
├── service
│   ├── AuthService.java
│   ├── UserService.java
│   ├── ResumeService.java
│   ├── JobService.java
│   ├── AnalysisService.java
│   ├── ApplicationService.java
│   ├── FileStorageService.java
│   ├── ResumeParseService.java
│   └── AiService.java
│
├── service.impl
│   ├── AuthServiceImpl.java
│   ├── UserServiceImpl.java
│   ├── ResumeServiceImpl.java
│   ├── JobServiceImpl.java
│   ├── AnalysisServiceImpl.java
│   ├── ApplicationServiceImpl.java
│   ├── LocalFileStorageServiceImpl.java
│   ├── ResumeParseServiceImpl.java
│   └── AiServiceImpl.java
│
├── mapper
│   ├── UserMapper.java
│   ├── ResumeMapper.java
│   ├── JobDescriptionMapper.java
│   ├── AnalysisReportMapper.java
│   └── ApplicationRecordMapper.java
│
├── entity
│   ├── User.java
│   ├── Resume.java
│   ├── JobDescription.java
│   ├── AnalysisReport.java
│   └── ApplicationRecord.java
│
├── dto
│   ├── auth
│   ├── user
│   ├── resume
│   ├── job
│   ├── analysis
│   └── application
│
├── vo
│   ├── auth
│   ├── user
│   ├── resume
│   ├── job
│   ├── analysis
│   └── application
│
├── enums
│   ├── UserRoleEnum.java
│   ├── ApplicationStatusEnum.java
│   ├── MatchLevelEnum.java
│   └── FileTypeEnum.java
│
├── exception
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   ├── FileParseException.java
│   └── AiServiceException.java
│
└── util
    ├── SecurityUtils.java
    ├── JsonUtils.java
    ├── FileUtils.java
    └── PromptUtils.java
```

### 18.2 目录设计说明

| 目录 | 说明 |
|---|---|
| common | 通用响应、错误码、分页对象 |
| config | 配置类 |
| security | Spring Security 和 JWT 相关 |
| controller | 接口层 |
| service | 业务接口 |
| service.impl | 业务实现 |
| mapper | MyBatis Mapper |
| entity | 数据库实体 |
| dto | 请求参数对象 |
| vo | 响应视图对象 |
| enums | 枚举 |
| exception | 异常处理 |
| util | 工具类 |

---

## 19. 配置文件概要设计

### 19.1 application.yml 结构

```yaml
server:
  port: 8080

spring:
  application:
    name: intern-pilot

  datasource:
    url: jdbc:mysql://localhost:3306/intern_pilot?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      database: 0

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mybatis-plus:
  mapper-locations: classpath:/mapper/*.xml
  type-aliases-package: com.internpilot.entity
  configuration:
    map-underscore-to-camel-case: true

jwt:
  secret: your_jwt_secret
  expiration: 86400000

file:
  upload-dir: uploads/resumes

ai:
  provider: deepseek
  api-key: ${AI_API_KEY}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  timeout: 60000
```

### 19.2 配置安全要求

1. AI API Key 不要写死在代码中；
2. 生产环境数据库密码不要提交到 GitHub；
3. JWT Secret 需要使用较长随机字符串；
4. 可以提供 `application-example.yml`；
5. `.gitignore` 应排除真实配置文件。

---

## 20. 部署概要设计

### 20.1 第一阶段本地运行

第一阶段优先本地运行：

```text
Windows / Linux
  ↓
JDK 17
  ↓
MySQL
  ↓
Redis
  ↓
Spring Boot
```

### 20.2 第二阶段 Docker Compose

后续使用 Docker Compose 启动：

1. MySQL；
2. Redis；
3. Spring Boot Backend；
4. Frontend Nginx。

### 20.3 部署结构

```text
deploy/
├── docker-compose.yml
├── mysql/
├── redis/
└── nginx/
```

---

## 21. 系统扩展设计

### 21.1 WebSocket 扩展

AI 分析耗时较长，后续可以使用 WebSocket 返回进度。

示例进度：

```text
10% 正在读取简历
30% 正在分析岗位 JD
50% 正在调用 AI
80% 正在解析结果
100% 分析完成
```

### 21.2 异步任务扩展

后续可以将 AI 分析改为异步任务：

```text
用户提交分析任务
  ↓
返回 taskId
  ↓
后台线程 / 消息队列处理
  ↓
前端轮询或 WebSocket 获取结果
```

### 21.3 RAG 扩展

后续可以加入岗位知识库：

```text
岗位 JD
  ↓
提取关键词
  ↓
查询岗位知识库
  ↓
召回相关技能要求
  ↓
结合简历进行分析
```

### 21.4 多模型扩展

通过统一接口支持多个模型：

```text
AiClient
  ├── DeepSeekAiClient
  ├── OpenAiClient
  ├── QwenAiClient
  └── MockAiClient
```

---

## 22. 概要设计结论

InternPilot 系统采用前后端分离架构，后端基于 Spring Boot 构建，结合 Spring Security + JWT 完成认证授权，使用 MySQL 存储业务数据，使用 Redis 缓存 AI 分析结果，使用大语言模型 API 完成简历与岗位 JD 的智能匹配分析。

系统第一阶段重点完成核心业务闭环：

```text
用户登录
  ↓
上传简历
  ↓
创建岗位
  ↓
AI 分析
  ↓
生成报告
  ↓
管理投递记录
```

该设计在功能上能够满足大学生实习投递场景的核心需求，在技术上能够体现 Java 后端开发、权限认证、文件处理、缓存优化、AI 应用集成和工程化设计能力。

第一阶段建议严格控制范围，优先完成 MVP；后续再逐步扩展 WebSocket、异步任务、RAG、岗位推荐、数据看板和管理员后台等高级功能。
