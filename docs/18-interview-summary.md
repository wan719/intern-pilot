# InternPilot 面试总结与简历包装文档

## 1. 文档说明

本文档用于整理 InternPilot 项目的简历写法、面试讲解思路、技术亮点、常见追问和回答模板。

InternPilot 是一个面向大学生实习求职场景的 AI 实习投递与简历优化平台。该项目适合作为 Java 后端实习求职项目，重点展示以下能力：

1. Spring Boot 后端开发能力；
2. Spring Security + JWT 认证鉴权能力；
3. MySQL 数据建模能力；
4. Redis 缓存设计能力；
5. 文件上传与解析能力；
6. AI 应用集成能力；
7. 用户数据权限控制能力；
8. 工程化设计能力；
9. 完整业务闭环设计能力。

---

## 2. 项目一句话介绍

### 2.1 简洁版

InternPilot 是一个面向大学生实习求职的 AI 简历岗位匹配与投递管理平台，支持简历上传解析、岗位 JD 管理、AI 匹配分析、简历优化建议生成和投递进度跟踪。

### 2.2 面试版

InternPilot 是我为了提升大学生实习投递效率设计的一个 AI 求职辅助平台。用户可以上传 PDF 或 DOCX 简历，保存目标岗位 JD，系统会调用大语言模型分析简历和岗位的匹配度，生成匹配分数、技能缺口、简历优化建议和面试准备建议。同时系统还支持投递记录管理，帮助用户跟踪从待投递、已投递、笔试、面试到 Offer 或被拒的完整流程。

### 2.3 简历项目标题

```text
InternPilot：面向大学生的 AI 实习投递与简历优化平台
```

## 3. 项目背景

### 3.1 背景描述

大学生在找实习时通常会遇到几个问题：

1. 不知道自己的简历和岗位 JD 是否匹配；
2. 不知道简历应该针对某个岗位怎么优化；
3. 投递多个岗位后，投递状态容易混乱；
4. 面试前不知道应该重点准备哪些内容；
5. 缺少一个完整的求职过程管理工具。

因此，我设计了 InternPilot，用 AI 帮助学生分析简历与岗位 JD 的匹配情况，并将分析结果与投递管理结合起来，形成完整的实习求职辅助流程。

### 3.2 项目解决的问题

InternPilot 主要解决三个问题：

1. 简历和岗位是否匹配？
2. 简历应该怎么优化？
3. 投递进度如何管理？

对应功能：

| 问题 | 对应模块 |
| --- | --- |
| 简历和岗位是否匹配 | AI 分析模块 |
| 简历如何优化 | AI 建议生成 |
| 投递进度如何管理 | 投递记录模块 |
| 简历内容如何输入 | 简历上传与解析模块 |
| 岗位信息如何输入 | 岗位 JD 管理模块 |

## 4. 项目技术栈

### 4.1 后端技术栈

- Java 17
- Spring Boot 3
- Spring Security 6
- JWT
- MyBatis Plus
- MySQL 8
- Redis 7
- Knife4j / Swagger
- Apache PDFBox
- Apache POI
- Lombok
- Gradle

### 4.2 AI 相关技术

- DeepSeek API / OpenAI API / Qwen API 可扩展
- Prompt Engineering
- JSON 结构化输出
- AI 调用封装
- Redis 缓存 AI 分析结果

### 4.3 工程化技术

- 统一响应结构
- 全局异常处理
- 参数校验
- 逻辑删除
- Dockerfile
- Docker Compose
- 环境变量配置
- Swagger 接口文档

## 5. 项目整体架构

### 5.1 架构概述

InternPilot 采用前后端分离架构。

后端基于 Spring Boot 3 构建，使用 Spring Security + JWT 实现无状态认证，使用 MySQL 存储业务数据，使用 Redis 缓存 AI 分析结果，使用 PDFBox / POI 解析简历文件，使用大语言模型 API 生成匹配分析报告。

### 5.2 架构图文本版

```text
前端 Vue 应用
  ↓
HTTP / JSON / Multipart
  ↓
Spring Boot 后端
  ↓
Controller 层
  ↓
Service 层
  ↓
Mapper 层
  ↓
MySQL

Spring Boot 后端
  ↓
Redis 缓存 AI 分析结果

Spring Boot 后端
  ↓
文件存储 uploads/resumes

Spring Boot 后端
  ↓
大语言模型 API
```

### 5.3 后端分层

- Controller：接收请求，参数校验，返回统一响应
- Service：处理业务逻辑
- Mapper：访问数据库
- Entity：数据库实体
- DTO：请求参数对象
- VO：响应结果对象
- Security：JWT 鉴权
- Config：配置类
- Exception：异常处理
- Util：工具类

## 6. 核心业务流程

### 6.1 完整业务闭环

```text
用户注册登录
  ↓
上传简历
  ↓
系统解析 PDF / DOCX 文本
  ↓
用户创建岗位 JD
  ↓
用户选择简历和岗位
  ↓
AI 分析简历岗位匹配度
  ↓
生成分析报告
  ↓
用户创建投递记录
  ↓
持续更新投递状态
```

### 6.2 AI 分析流程

```text
用户选择 resumeId 和 jobId
  ↓
校验简历属于当前用户
  ↓
校验岗位属于当前用户
  ↓
读取 resume.parsed_text
  ↓
读取 job_description.jd_content
  ↓
检查 Redis 缓存
  ↓
缓存命中：直接返回
  ↓
缓存未命中：构造 Prompt
  ↓
调用大语言模型 API
  ↓
解析 JSON 结果
  ↓
保存 analysis_report
  ↓
写入 Redis
  ↓
返回分析结果
```

### 6.3 投递管理流程

```text
用户查看 AI 分析报告
  ↓
决定是否投递
  ↓
创建投递记录
  ↓
状态从 TO_APPLY 更新到 APPLIED
  ↓
后续更新为 WRITTEN_TEST / FIRST_INTERVIEW / OFFER / REJECTED
  ↓
记录备注和复盘
```

## 7. 简历推荐写法

### 7.1 简历项目描述完整版

```markdown
InternPilot：面向大学生的 AI 实习投递与简历优化平台

项目描述：
基于 Spring Boot 3 + Spring Security + JWT + MySQL + Redis + 大语言模型 API 构建的实习求职辅助平台，支持用户上传 PDF / DOCX 简历、保存岗位 JD、调用 AI 分析简历与岗位匹配度，并生成匹配分数、技能缺口、简历优化建议和面试准备建议。同时支持投递记录管理，形成从岗位分析到投递跟踪的完整求职管理闭环。

技术栈：
Java 17、Spring Boot 3、Spring Security、JWT、MyBatis Plus、MySQL、Redis、PDFBox、Apache POI、Knife4j、Docker Compose

主要工作：
- 设计并实现用户注册登录模块，基于 Spring Security + JWT 实现前后端分离场景下的无状态认证；
- 实现 PDF / DOCX 简历上传与文本解析功能，使用 PDFBox 和 Apache POI 提取简历文本；
- 设计岗位 JD 管理模块，支持岗位创建、搜索、修改、删除和用户数据隔离；
- 集成大语言模型 API，构造 Prompt 分析简历与岗位 JD 的匹配程度，生成结构化 JSON 分析报告；
- 使用 Redis 缓存相同简历和岗位组合的 AI 分析结果，降低重复调用成本；
- 实现投递记录管理模块，支持投递状态跟踪、面试时间记录和复盘备注；
- 使用统一响应结构、全局异常处理、参数校验、Swagger 文档和 Docker Compose 提升项目工程化程度。
```

### 7.2 简历项目描述精简版

```markdown
InternPilot：面向大学生的 AI 实习投递与简历优化平台

基于 Spring Boot 3 + Spring Security + JWT + MySQL + Redis + 大语言模型 API 构建，支持简历上传解析、岗位 JD 管理、AI 匹配分析、分析报告生成和投递记录管理。负责用户认证、简历解析、AI 分析、Redis 缓存、数据权限控制和接口文档等核心模块，实现从岗位分析到投递跟踪的完整业务闭环。
```

### 7.3 简历项目 bullet 版

- 基于 Spring Security + JWT 实现用户注册登录和接口鉴权，支持前后端分离场景下的无状态认证；
- 使用 PDFBox 和 Apache POI 实现 PDF / DOCX 简历上传与文本解析，解析结果用于后续 AI 分析；
- 设计岗位 JD 管理和投递记录模块，支持岗位搜索、投递状态跟踪、面试时间和复盘记录；
- 集成大语言模型 API，通过 Prompt Engineering 生成匹配分数、技能缺口、简历优化建议和面试准备建议；
- 使用 Redis 缓存相同简历与岗位组合的 AI 分析结果，减少重复调用并提升接口响应速度；
- 基于 userId 实现简历、岗位、分析报告和投递记录的数据隔离，防止越权访问；
- 通过统一响应、全局异常处理、参数校验、Swagger 文档和 Docker Compose 提升项目工程化水平。

## 8. 面试开场讲法

### 8.1 1 分钟版本

面试官问“介绍一下你的项目”，可以这样说：

我做的项目叫 InternPilot，是一个面向大学生实习求职的 AI 简历岗位匹配和投递管理平台。

用户可以上传 PDF 或 DOCX 简历，系统会解析出简历文本；然后用户可以保存目标岗位 JD。之后系统会把简历文本和岗位 JD 组合成 Prompt，调用大语言模型分析匹配度，生成匹配分数、优势、短板、技能缺口、简历优化建议和面试准备建议。

除了 AI 分析，我还做了投递记录管理，用户可以记录某个岗位是待投递、已投递、笔试、面试、Offer 还是被拒，形成完整的实习投递闭环。

技术上我主要用了 Spring Boot 3、Spring Security、JWT、MyBatis Plus、MySQL、Redis、PDFBox、POI 和大模型 API。项目中我比较重点做了 JWT 鉴权、数据权限控制、AI 结果缓存、文件解析和统一异常处理。

### 8.2 3 分钟版本

InternPilot 是我为了提升大学生找实习效率做的一个项目。它的核心目标是解决三个问题：简历和岗位是否匹配、简历应该怎么优化、投递过程如何管理。

系统整体采用前后端分离架构，后端基于 Spring Boot 3 构建。用户首先注册登录，登录成功后后端签发 JWT，后续请求通过 Authorization Header 携带 Token。Spring Security 中配置了 JWT 过滤器，用来解析 Token 并把用户信息放入 SecurityContext。

业务上，用户可以上传 PDF 或 DOCX 简历，后端会校验文件类型和大小，然后保存文件，并用 PDFBox 或 Apache POI 解析文本，保存到 resume 表的 parsed_text 字段。用户还可以创建岗位 JD，保存公司、岗位、地点、薪资、JD 内容等信息。

AI 分析时，用户选择一份简历和一个岗位，后端会先校验这两个资源是否属于当前用户，防止越权访问。然后读取简历解析文本和岗位 JD，构造 Prompt，调用大语言模型 API。Prompt 要求模型严格返回 JSON，包括 matchScore、matchLevel、strengths、weaknesses、missingSkills、suggestions 和 interviewTips。后端解析 JSON 后保存到 analysis_report 表。

为了降低重复调用 AI 的成本，我用 Redis 缓存相同 userId、resumeId、jobId 组合下的分析结果。如果用户再次分析同一组简历和岗位，就直接从 Redis 返回；如果用户选择 forceRefresh，才重新调用 AI。

最后，系统支持投递记录管理，用户可以把分析后的岗位加入投递记录，并更新状态，比如待投递、已投递、笔试、一面、二面、Offer 或被拒，也可以记录面试时间和复盘内容。

这个项目对我来说比较有价值的地方是，它不是单纯 CRUD，而是把认证鉴权、文件处理、AI 能力、Redis 缓存、数据权限和工程化设计串成了一个完整业务闭环。

## 9. 核心技术亮点

### 9.1 Spring Security + JWT

亮点说明：

基于 Spring Security + JWT 实现前后端分离项目中的无状态认证。登录成功后生成 Token，后续请求通过 JwtAuthenticationFilter 解析 Token，并将用户信息写入 SecurityContext。

能讲的点：

- 为什么用 JWT；
- JWT 里存了什么；
- Token 怎么校验；
- 401 和 403 如何处理；
- Security Filter Chain 执行流程；
- 如何获取当前登录用户。

### 9.2 数据权限控制

亮点说明：

所有简历、岗位、分析报告和投递记录都绑定 userId，业务查询时必须同时校验资源 ID 和当前用户 ID，防止用户通过修改 URL 访问他人数据。

典型代码思路：

```sql
WHERE id = ? AND user_id = 当前登录用户ID AND deleted = 0
```

能讲的点：

- JWT 只能证明“你是谁”；
- 业务层还需要判断“这条数据是不是你的”；
- 防止水平越权；
- 为什么不能只根据 id 查询。

### 9.3 文件上传与解析

亮点说明：

支持 PDF / DOCX 简历上传，后端校验文件类型、大小和路径安全，保存文件后使用 PDFBox / Apache POI 提取文本，用于后续 AI 分析。

能讲的点：

- 文件名为什么不能直接用用户原始文件名；
- PDFBox 解析文本型 PDF；
- POI 解析 DOCX；
- 扫描版 PDF 为什么解析不了；
- 后续如何接 OCR；
- uploads 为什么要加入 .gitignore。

### 9.4 AI 分析模块

亮点说明：

通过 Prompt Engineering 将简历文本和岗位 JD 输入大语言模型，要求模型返回结构化 JSON，后端解析后保存为分析报告。

能讲的点：

- Prompt 如何设计；
- 为什么要求 JSON 输出；
- AI 返回 Markdown 怎么处理；
- raw_ai_response 为什么要保存；
- AI 失败如何处理；
- 多模型如何扩展。

### 9.5 Redis 缓存

亮点说明：

使用 Redis 缓存相同用户、相同简历、相同岗位组合的 AI 分析结果，避免重复调用大模型，提升响应速度并降低调用成本。

缓存 Key：

```text
internpilot:analysis:{userId}:{resumeId}:{jobId}
```

能讲的点：

- 为什么 AI 分析适合缓存；
- 缓存 Key 怎么设计；
- forceRefresh 如何绕过缓存；
- 缓存过期时间；
- 简历或岗位修改后如何失效缓存；
- Redis 常见问题：穿透、击穿、雪崩。

### 9.6 工程化设计

亮点说明：

项目使用统一响应结构、全局异常处理、参数校验、Swagger 接口文档、Docker Compose 和环境变量配置，提升可维护性和可部署性。

能讲的点：

- Result 统一响应；
- GlobalExceptionHandler；
- 参数校验；
- Swagger / Knife4j；
- Docker Compose；
- application-example.yml；
- 敏感配置不能提交 GitHub。

## 10. 常见面试问题与回答

### 10.1 你为什么做这个项目？

回答：

因为我自己也处在找实习的阶段，发现很多大学生在投递实习时不知道自己的简历和岗位是否匹配，也不知道简历应该针对某个岗位怎么优化。现在大语言模型可以理解简历和岗位 JD，所以我想把 AI 能力和传统求职管理结合起来，做一个既能分析匹配度，又能管理投递进度的平台。

### 10.2 这个项目和普通 CRUD 项目有什么区别？

回答：

普通 CRUD 项目主要是增删改查，而 InternPilot 在 CRUD 基础上加入了几个更有实际价值的点。

第一，它有完整的认证鉴权和数据权限控制；第二，它支持 PDF / DOCX 文件上传和文本解析；第三，它集成了大语言模型，通过 Prompt 生成结构化分析报告；第四，它用 Redis 缓存 AI 分析结果，降低重复调用成本；第五，它形成了从简历、岗位、AI 分析到投递记录的完整业务闭环。

### 10.3 为什么用 JWT？

回答：

因为这个项目是前后端分离架构，后端主要提供 RESTful API。JWT 是无状态认证，服务端不需要保存 Session，适合前后端分离和后续多端接入。

用户登录成功后，后端生成 JWT，里面包含 userId、username 和 role。前端后续请求时把 Token 放在 Authorization Header 中，后端通过 JWT 过滤器解析 Token 并放入 SecurityContext。

### 10.4 JWT 有什么缺点？

回答：

JWT 的一个缺点是服务端不保存状态，所以 Token 一旦签发，在过期前默认一直有效，主动失效比较麻烦。比如用户退出登录或者修改密码后，希望旧 Token 立即失效，就需要额外引入 Token 黑名单、Redis 存储失效 Token，或者使用短 Access Token + Refresh Token 的方案。

我当前项目第一阶段主要实现基础 JWT 鉴权，后续可以通过 Redis 黑名单或 Refresh Token 进一步优化。

### 10.5 Spring Security 过滤链怎么工作的？

回答：

请求进入后会先经过 Spring Security 的过滤器链。对于注册、登录和 Swagger 文档接口，在 SecurityConfig 中配置了 permitAll，可以直接访问。

对于其他业务接口，请求会经过自定义的 JwtAuthenticationFilter。过滤器会从 Authorization 请求头中提取 Bearer Token，校验 Token 是否有效，解析出 userId、username 和 role，然后构造 UsernamePasswordAuthenticationToken 放入 SecurityContext。

后续的授权逻辑会根据 SecurityContext 判断当前用户是否已经认证，以及是否拥有对应角色权限。

### 10.6 401 和 403 的区别？

回答：

401 表示未认证，也就是用户没有登录、Token 无效或 Token 过期。403 表示已认证但没有权限，比如普通用户访问管理员接口。

项目中我通过 AuthenticationEntryPoint 统一处理 401，通过 AccessDeniedHandler 统一处理 403，并返回统一 JSON 响应。

### 10.7 怎么防止用户访问别人的简历？

回答：

JWT 只能证明当前用户是谁，但不能自动保证数据属于这个用户。所以所有用户私有资源都必须在业务查询时带上 userId 条件。

比如查询简历详情时，不是只根据 resumeId 查询，而是使用 resumeId + currentUserId + deleted=0 一起查询。如果查不到，就返回简历不存在或无权限访问。

岗位、分析报告和投递记录也是一样的处理方式。

### 10.8 简历上传怎么做的？

回答：

用户上传简历后，后端先校验文件是否为空、文件大小是否超过限制、文件扩展名是否是 PDF 或 DOCX。然后后端生成新的存储文件名，按 userId 分目录保存到本地 uploads/resumes 目录下。

保存成功后，根据文件类型调用不同解析器，PDF 使用 PDFBox，DOCX 使用 Apache POI。解析出的文本会经过简单清洗后保存到 resume 表的 parsed_text 字段，后续 AI 分析模块会读取这个字段。

### 10.9 为什么不直接用原始文件名保存？

回答：

因为原始文件名可能重复，也可能包含中文、特殊字符，甚至可能出现路径穿越风险。所以我只把原始文件名保存到数据库用于展示，实际存储文件名由系统根据 userId、时间戳和随机字符串生成。

这样可以避免重名，也更安全。

### 10.10 PDF 解析失败怎么办？

回答：

PDF 解析失败通常有几种情况，比如扫描版 PDF、图片型 PDF、加密 PDF 或特殊字体。PDFBox 对文本型 PDF 支持比较好，但对扫描版 PDF 无法直接提取文字。

第一阶段我会捕获解析异常并返回友好提示。后续可以接入 OCR，比如 PaddleOCR 或云 OCR 服务，来支持扫描版 PDF。

### 10.11 AI 分析模块怎么实现？

回答：

用户选择简历和岗位后，后端先校验简历和岗位都属于当前用户。然后读取简历解析文本和岗位 JD，构造 Prompt，调用大语言模型 API。

Prompt 中要求模型严格返回 JSON，字段包括 matchScore、matchLevel、strengths、weaknesses、missingSkills、suggestions 和 interviewTips。后端拿到返回后会清洗可能的 Markdown 包裹内容，然后用 Jackson 解析 JSON，保存到 analysis_report 表，并返回给前端。

### 10.12 AI 返回格式不稳定怎么办？

回答：

我主要从三方面处理。

第一，在 Prompt 中明确要求必须返回 JSON，不要返回 Markdown，也不要额外解释。第二，后端对模型返回结果做简单清洗，比如去掉 Markdown 的 json 代码块标记，只截取第一个左大括号到最后一个右大括号之间的内容。第三，如果 Jackson 解析失败，就抛出 AI 服务异常，同时保留 raw_ai_response 字段，方便排查模型实际返回了什么。

### 10.13 为什么要用 Redis 缓存 AI 分析结果？

回答：

AI 分析接口和普通数据库查询不同，它调用外部大模型，响应更慢，也可能产生调用成本。对于同一用户、同一简历、同一岗位，在短时间内重复分析通常没有必要重新调用模型。

所以我使用 Redis 缓存分析结果，Key 由 userId、resumeId 和 jobId 组成。如果缓存命中就直接返回。如果用户选择 forceRefresh，才绕过缓存重新调用 AI。

### 10.14 Redis 缓存 Key 怎么设计？

回答：

我设计的 Key 是 `internpilot:analysis:{userId}:{resumeId}:{jobId}`。

这样可以保证不同用户之间的缓存隔离，也能精确定位某个用户某份简历和某个岗位的分析结果。比如 `internpilot:analysis:1:10:25` 表示用户 1 的简历 10 和岗位 25 的分析结果。

### 10.15 如果简历或岗位修改了，缓存怎么办？

回答：

第一阶段主要依赖 TTL 自动过期，同时提供 forceRefresh 参数让用户主动重新分析。

后续可以优化为：当简历 parsedText 修改或岗位 jdContent 修改时，主动删除相关缓存。更进一步可以在缓存 Key 中加入简历和岗位内容的 hash，这样内容一变 Key 就变，自然不会命中旧缓存。

### 10.16 为什么分析报告要入库？

回答：

因为 AI 分析报告不仅是一次接口返回结果，后续还需要让用户查看历史分析记录，也可以和投递记录关联。

比如用户可以看到某个岗位当时的匹配分数是多少，后续投递结果如何。这样也方便做数据统计，比如高匹配分数岗位是否更容易进入面试。

### 10.17 投递记录模块为什么要关联 reportId？

回答：

reportId 表示这条投递记录是基于哪一次 AI 分析做出的投递决策。这样后续可以追踪“AI 分析结果”和“实际投递结果”之间的关系。

比如匹配分数高的岗位是否更容易进入面试，哪些技能缺口经常导致被拒，这些都可以基于 reportId 做进一步统计。

### 10.18 为什么使用逻辑删除？

回答：

因为简历、岗位、分析报告和投递记录之间存在业务关联。如果直接物理删除，历史分析报告和投递记录可能丢失上下文。

所以我使用 deleted 字段做逻辑删除。列表查询时只查 deleted=0 的数据，但数据库中保留历史记录，后续可以用于恢复、审计或统计。

### 10.19 你项目中最难的地方是什么？

回答示例：

我觉得最难的是 AI 分析模块的稳定性和工程化处理。

因为大模型返回内容不像普通接口那么稳定，有时会返回 Markdown，有时会多一些解释文字。所以我需要在 Prompt 中约束输出格式，并在后端做 JSON 提取和解析。

另外，AI 调用比较慢且有成本，所以我设计了 Redis 缓存机制，对相同用户、简历和岗位组合的分析结果进行缓存，并通过 forceRefresh 控制是否重新分析。

### 10.20 这个项目后续怎么优化？

回答：

后续我会从几个方向优化。

第一，AI 分析改成异步任务，用户提交后立即返回 taskId，前端通过轮询或 WebSocket 获取进度。第二，增加投递状态变化日志，把投递过程展示成时间线。第三，增加数据看板，统计投递数量、面试数量、Offer 数量和平均匹配分数。第四，支持 OCR 解析扫描版 PDF。第五，引入 RAG 岗位知识库，让 AI 分析更专业。第六，做前端页面和 Docker Compose 一键部署，提升展示效果。

## 11. 模块级讲解模板

### 11.1 用户认证模块讲解

用户认证模块主要负责注册、登录和接口鉴权。注册时使用 BCrypt 对密码加密后保存到数据库。登录时根据用户名查询用户，并使用 PasswordEncoder 校验密码，校验成功后生成 JWT。

JWT 中包含 userId、username 和 role。前端后续访问接口时，在 Authorization Header 中携带 Bearer Token。后端通过 JwtAuthenticationFilter 解析 Token，如果有效就构造 Authentication 放入 SecurityContext。

对于异常情况，未登录或 Token 无效返回 401，权限不足返回 403。

### 11.2 简历模块讲解

简历模块负责上传和解析用户简历。上传时后端会校验文件大小和类型，只允许 PDF 和 DOCX。文件保存时不会直接使用原始文件名，而是使用 userId、时间戳和随机字符串生成存储文件名，避免重名和路径安全问题。

解析方面，PDF 使用 PDFBox，DOCX 使用 Apache POI。解析出的文本会进行简单清洗，然后保存到 parsed_text 字段，供后续 AI 分析模块使用。

### 11.3 岗位模块讲解

岗位模块用于保存目标实习岗位 JD。用户可以创建岗位，填写公司名称、岗位名称、岗位类型、地点、来源平台、薪资、实习周期和完整 JD 内容。

岗位数据也绑定 userId，查询、修改和删除时都必须校验当前用户，防止越权访问。岗位 JD 后续会作为 AI 分析模块的输入。

### 11.4 AI 分析模块讲解

AI 分析模块是项目的核心亮点。用户选择简历和岗位后，系统读取简历 parsedText 和岗位 jdContent，构造 Prompt 调用大语言模型。

模型返回结构化 JSON，包含匹配分数、匹配等级、优势、短板、缺失技能、优化建议和面试准备建议。后端解析后保存到 analysis_report 表，并将结果写入 Redis 缓存，避免重复调用。

### 11.5 投递记录模块讲解

投递记录模块负责管理用户实际投递进度。用户可以基于岗位、简历和 AI 分析报告创建投递记录，并更新投递状态，比如待投递、已投递、笔试、一面、二面、Offer、被拒等。

这个模块让系统从 AI 分析工具变成完整的求职管理工具，形成从分析到行动再到复盘的闭环。

## 12. 面试追问深挖准备

### 12.1 如果问“你的项目有哪些表？”

回答：

核心表有五张。

`user` 表保存用户账号和基础信息；`resume` 表保存简历文件信息和解析文本；`job_description` 表保存岗位 JD；`analysis_report` 表保存 AI 分析结果；`application_record` 表保存投递记录。

后续扩展表包括 `ai_call_log`、`prompt_template` 和 `application_status_log`。

### 12.2 如果问“为什么不用物理外键？”

回答：

第一阶段我没有使用数据库物理外键，而是在业务层做关联校验。

原因是项目中大量使用逻辑删除，如果使用物理外键，删除和修改时容易受到约束影响。同时业务层校验可以更灵活地处理“资源不存在或无权限访问”的逻辑。

当然，在数据一致性要求更高的场景下，也可以使用物理外键或通过事务和服务层逻辑保证一致性。

### 12.3 如果问“AI 分析结果为什么存 JSON 字符串？”

回答：

因为 strengths、weaknesses、missingSkills、suggestions 这些字段本质是列表。第一阶段为了快速实现 MVP，我把它们序列化成 JSON 字符串存到 TEXT 字段中。

这样表结构比较简单，也方便直接返回前端。后续如果需要统计哪些技能最常缺失，可以再把 missingSkills 拆成结构化子表。

### 12.4 如果问“Redis 缓存一致性怎么保证？”

回答：

当前第一阶段主要通过 TTL 和 forceRefresh 保证缓存不会长期陈旧。

如果进一步优化，我会在简历 parsedText 修改、岗位 jdContent 修改时主动删除相关缓存。也可以在缓存 Key 中加入简历文本和岗位 JD 的 hash，这样内容变化后缓存 Key 自动变化，不会命中旧结果。

### 12.5 如果问“AI 调用失败怎么办？”

回答：

AI 调用失败时，我会捕获异常并抛出统一的 AiServiceException，通过全局异常处理返回统一 JSON 响应。

同时，项目设计中保留 raw_ai_response 字段用于排查 AI 返回异常。如果后续要增强稳定性，可以增加重试机制、超时控制、降级到 Mock 模型或返回历史缓存结果。

## 13. 项目不足与改进方向

### 13.1 当前不足

1. 前端页面还不完整；
2. AI 分析目前是同步调用，耗时较长；
3. PDF 解析暂不支持扫描版 PDF；
4. Redis 缓存失效策略还可以更精细；
5. 投递状态暂未记录变化历史；
6. AI 调用日志和成本统计还未完整实现；
7. 缺少完整 CI/CD；
8. 单元测试和集成测试还可以继续补充。

### 13.2 改进方向

1. 增加 Vue 3 前端页面；
2. AI 分析改为异步任务；
3. 使用 WebSocket 展示分析进度；
4. 增加 OCR 支持扫描版简历；
5. 增加投递状态时间线；
6. 增加数据看板；
7. 增加 AI 调用日志和成本统计；
8. 增加 RAG 岗位知识库；
9. 增加 Docker Compose 一键部署；
10. 增加 GitHub Actions 自动测试。

## 14. 项目亮点提炼

面试中可以重点强调这 6 个亮点：

1. 不是单纯 CRUD，而是完整求职业务闭环；
2. 使用 Spring Security + JWT 实现认证鉴权；
3. 基于 userId 做严格数据隔离，防止越权；
4. 支持 PDF / DOCX 简历上传与解析；
5. 集成大语言模型生成结构化匹配分析报告；
6. 使用 Redis 缓存 AI 分析结果，降低重复调用成本。

## 15. 简历最终推荐版本

如果你的简历空间有限，推荐写成这样：

```markdown
InternPilot：AI 实习投递与简历优化平台
技术栈：Java 17、Spring Boot 3、Spring Security、JWT、MyBatis Plus、MySQL、Redis、PDFBox、POI、Knife4j、Docker Compose

- 基于 Spring Security + JWT 实现用户注册登录、无状态认证和接口鉴权，统一处理 401/403 异常；
- 实现 PDF / DOCX 简历上传与文本解析，使用 PDFBox / POI 提取简历文本并用于 AI 分析；
- 设计岗位 JD、AI 分析报告和投递记录模块，形成从岗位分析到投递跟踪的完整业务闭环；
- 集成大语言模型 API，通过 Prompt Engineering 生成匹配分数、技能缺口、简历优化建议和面试准备建议；
- 使用 Redis 缓存相同简历与岗位组合的分析结果，减少重复 AI 调用并提升响应速度；
- 基于 userId 实现用户私有数据隔离，防止简历、岗位、报告和投递记录的越权访问。
```

## 16. 面试最后总结话术

如果面试官让你总结这个项目，可以说：

这个项目对我最大的价值是，它让我把 Java 后端常见能力和 AI 应用场景结合起来了。

我不只是做了用户、岗位、简历这些基础 CRUD，还重点实现了 JWT 鉴权、文件解析、AI Prompt 设计、Redis 缓存、数据权限控制和投递闭环管理。

从业务上看，它解决的是大学生实习投递中的真实问题；从技术上看，它覆盖了 Spring Boot 后端开发中比较核心的认证、安全、缓存、文件处理、数据库建模和工程化能力。

后续我会继续补充前端页面、异步 AI 分析、WebSocket 进度展示和数据看板，让它更接近一个完整可上线的产品。

## 17. 面试准备清单

面试前你需要能熟练讲清楚：

### 17.1 必会

- 项目背景和业务闭环；
- 用户注册登录流程；
- JWT 生成和解析流程；
- Spring Security 过滤链；
- 401 和 403 区别；
- 如何防止越权访问；
- 简历上传和解析流程；
- AI 分析模块流程；
- Redis 缓存 Key 设计；
- 投递记录模块作用。

### 17.2 最好会

- 为什么用逻辑删除；
- 为什么不用物理外键；
- AI 返回格式不稳定怎么处理；
- 缓存一致性怎么处理；
- 文件上传有哪些安全风险；
- 如果 AI 调用慢怎么优化；
- Docker Compose 怎么启动项目；
- 项目后续如何扩展。

### 17.3 可以准备的演示顺序

1. 打开 README 介绍项目；
2. 打开 Swagger / Knife4j；
3. 注册 / 登录；
4. 上传简历；
5. 创建岗位 JD；
6. 发起 AI 分析；
7. 查看分析报告；
8. 创建投递记录；
9. 修改投递状态；
10. 展示数据库表；
11. 展示 Redis 缓存 Key。

## 18. 文档结论

InternPilot 可以包装成一个比较完整的 Java 后端实习项目。

它的核心价值在于：

- 真实业务场景
- 完整业务闭环
- Spring Boot 后端能力
- Spring Security 鉴权能力
- 文件上传解析能力
- AI 应用集成能力
- Redis 缓存优化能力
- 用户数据权限控制能力
- 工程化意识

如果后续再补充前端页面、Docker Compose、一键启动和项目截图，这个项目会更适合放到 GitHub 和简历中展示。
