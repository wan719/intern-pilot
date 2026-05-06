# InternPilot 接口设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的后端接口设计，包括接口规范、统一响应格式、认证方式、错误码设计、用户认证接口、用户信息接口、简历管理接口、岗位 JD 管理接口、AI 分析接口和投递记录接口。

InternPilot 后端采用 RESTful API 风格，前端通过 HTTP 请求调用后端接口，后端返回统一 JSON 响应。

---

## 2. 接口设计原则

### 2.1 RESTful 风格

接口路径使用资源名词，尽量避免动词。

示例：

```text
GET    /api/resumes
POST   /api/resumes/upload
GET    /api/resumes/{id}
DELETE /api/resumes/{id}
```

### 2.2 统一接口前缀

所有业务接口统一使用：

```text
/api
```

例如：

```text
/api/auth/login
/api/resumes
/api/jobs
/api/analysis/match
/api/applications
```

### 2.3 请求和响应格式

普通接口使用 JSON 请求体：

```text
Content-Type: application/json
```

文件上传接口使用：

```text
Content-Type: multipart/form-data
```

所有接口统一返回 JSON。

### 2.4 权限控制原则

系统接口分为三类：

| 接口类型 | 权限要求 |
|---|---|
| 公开接口 | 不需要登录 |
| 用户接口 | 需要 USER 登录 |
| 管理员接口 | 需要 ADMIN 权限 |

第一阶段主要实现公开接口和普通用户接口。

---

## 3. 统一响应格式

### 3.1 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 3.2 失败响应

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

### 3.3 分页响应格式

分页接口统一使用：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| records | 当前页数据 |
| total | 总记录数 |
| pageNum | 当前页码 |
| pageSize | 每页数量 |
| pages | 总页数 |

---

## 4. 认证方式设计

### 4.1 JWT 认证

用户登录成功后，后端返回 JWT Token。

前端请求受保护接口时，需要在请求头中携带：

```text
Authorization: Bearer {token}
```

示例：

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx.xxx
```

### 4.2 Token 信息

JWT 中建议包含以下信息：

```json
{
  "userId": 1,
  "username": "wan",
  "role": "USER",
  "exp": 1710000000
}
```

### 4.3 放行接口

以下接口不需要登录：

| 方法 | URL | 说明 |
|---|---|---|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| GET | /v3/api-docs/** | Swagger API 文档 |
| GET | /swagger-ui/** | Swagger UI |
| GET | /doc.html | Knife4j 文档 |

### 4.4 受保护接口

除注册、登录和文档接口外，其余业务接口默认需要登录。

---

## 5. 错误码设计

| 错误码 | HTTP 状态 | 含义 |
|---|---|---|
| 200 | 200 | 成功 |
| 400 | 400 | 参数错误 |
| 401 | 401 | 未登录或 Token 无效 |
| 403 | 403 | 无权限访问 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 数据冲突 |
| 500 | 500 | 系统内部错误 |
| 600 | 500 | AI 服务异常 |
| 700 | 500 | 文件处理异常 |

### 5.1 常见错误响应示例

未登录：

```json
{
  "code": 401,
  "message": "请先登录",
  "data": null
}
```

无权限：

```json
{
  "code": 403,
  "message": "无权限访问该资源",
  "data": null
}
```

资源不存在：

```json
{
  "code": 404,
  "message": "资源不存在",
  "data": null
}
```

AI 服务异常：

```json
{
  "code": 600,
  "message": "AI 分析服务暂时不可用，请稍后重试",
  "data": null
}
```

文件解析失败：

```json
{
  "code": 700,
  "message": "简历文件解析失败，请检查文件格式",
  "data": null
}
```

---

## 6. 用户认证接口

### 6.1 用户注册

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/auth/register |
| Method | POST |
| 权限 | 无需登录 |
| Content-Type | application/json |

请求参数：

```json
{
  "username": "wan",
  "password": "123456",
  "confirmPassword": "123456",
  "email": "wan@example.com",
  "school": "西南大学",
  "major": "软件工程",
  "grade": "大二"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名，唯一 |
| password | String | 是 | 密码 |
| confirmPassword | String | 是 | 确认密码 |
| email | String | 否 | 邮箱 |
| school | String | 否 | 学校 |
| major | String | 否 | 专业 |
| grade | String | 否 | 年级 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "wan",
    "email": "wan@example.com",
    "school": "西南大学",
    "major": "软件工程",
    "grade": "大二",
    "role": "USER"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 用户名为空 | 400 | 用户名不能为空 |
| 密码为空 | 400 | 密码不能为空 |
| 两次密码不一致 | 400 | 两次密码输入不一致 |
| 用户名重复 | 409 | 用户名已存在 |
| 邮箱格式错误 | 400 | 邮箱格式不正确 |

### 6.2 用户登录

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/auth/login |
| Method | POST |
| 权限 | 无需登录 |
| Content-Type | application/json |

请求参数：

```json
{
  "username": "wan",
  "password": "123456"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx.xxx",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": 1,
      "username": "wan",
      "role": "USER"
    }
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 用户名为空 | 400 | 用户名不能为空 |
| 密码为空 | 400 | 密码不能为空 |
| 用户不存在 | 401 | 用户名或密码错误 |
| 密码错误 | 401 | 用户名或密码错误 |
| 用户被禁用 | 403 | 当前用户已被禁用 |

---

## 7. 用户信息接口

### 7.1 查询当前用户信息

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/user/me |
| Method | GET |
| 权限 | USER |
| Header | Authorization: Bearer Token |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "wan",
    "email": "wan@example.com",
    "phone": null,
    "realName": "黎宏",
    "school": "西南大学",
    "major": "软件工程",
    "grade": "大二",
    "role": "USER",
    "createdAt": "2026-05-05 20:00:00"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 未登录 | 401 | 请先登录 |
| Token 无效 | 401 | Token 无效或已过期 |

### 7.2 修改当前用户信息

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/user/me |
| Method | PUT |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "email": "new@example.com",
  "phone": "13200000000",
  "realName": "黎宏",
  "school": "西南大学",
  "major": "软件工程",
  "grade": "大二"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

业务规则：

1. 用户只能修改自己的信息；
2. 用户名不允许修改；
3. 角色不允许普通用户修改；
4. 密码修改后续单独设计接口。

---

## 8. 简历管理接口

### 8.1 上传简历

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/resumes/upload |
| Method | POST |
| 权限 | USER |
| Content-Type | multipart/form-data |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | MultipartFile | 是 | 简历文件 |
| resumeName | String | 否 | 简历名称 |

支持文件类型：

| 类型 | 说明 |
|---|---|
| PDF | 使用 PDFBox 解析 |
| DOCX | 使用 Apache POI 解析 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "resumeId": 1,
    "resumeName": "Java后端实习简历",
    "originalFileName": "resume.pdf",
    "fileType": "PDF",
    "fileSize": 204800,
    "parseStatus": "SUCCESS",
    "parsedTextPreview": "黎宏 西南大学 软件工程 Java Spring Boot...",
    "createdAt": "2026-05-05 20:10:00"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 未登录 | 401 | 请先登录 |
| 文件为空 | 400 | 上传文件不能为空 |
| 文件过大 | 400 | 文件大小不能超过 10MB |
| 文件格式不支持 | 700 | 仅支持 PDF 或 DOCX 文件 |
| 文件解析失败 | 700 | 简历文件解析失败 |

### 8.2 查询简历列表

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/resumes |
| Method | GET |
| 权限 | USER |

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 10 | 每页数量 |

请求示例：

```http
GET /api/resumes?pageNum=1&pageSize=10
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "resumeId": 1,
        "resumeName": "Java后端实习简历",
        "originalFileName": "resume.pdf",
        "fileType": "PDF",
        "fileSize": 204800,
        "parseStatus": "SUCCESS",
        "isDefault": true,
        "createdAt": "2026-05-05 20:10:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

业务规则：

1. 只返回当前用户的简历；
2. 默认按创建时间倒序；
3. 列表接口不返回完整 `parsedText`。

### 8.3 查询简历详情

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/resumes/{id} |
| Method | GET |
| 权限 | USER |

路径参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 简历 ID |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "resumeId": 1,
    "resumeName": "Java后端实习简历",
    "originalFileName": "resume.pdf",
    "storedFileName": "1_20260505_resume.pdf",
    "fileType": "PDF",
    "fileSize": 204800,
    "parseStatus": "SUCCESS",
    "isDefault": true,
    "parsedText": "黎宏，西南大学软件工程专业，项目经历包括...",
    "createdAt": "2026-05-05 20:10:00",
    "updatedAt": "2026-05-05 20:10:00"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 简历不存在 | 404 | 简历不存在 |
| 访问他人简历 | 403 | 无权限访问该简历 |

### 8.4 删除简历

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/resumes/{id} |
| Method | DELETE |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

业务规则：

1. 只能删除自己的简历；
2. 第一阶段采用逻辑删除；
3. 如果存在历史分析报告，报告保留。

### 8.5 设置默认简历

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/resumes/{id}/default |
| Method | PUT |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

业务规则：

1. 每个用户只能有一份默认简历；
2. 设置新默认简历时，取消原默认简历；
3. 只能设置自己的简历。

---

## 9. 岗位 JD 管理接口

### 9.1 创建岗位 JD

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/jobs |
| Method | POST |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "companyName": "腾讯",
  "jobTitle": "Java后端开发实习生",
  "jobType": "Java后端",
  "location": "深圳",
  "sourcePlatform": "Boss直聘",
  "jobUrl": "https://example.com/job/123",
  "jdContent": "岗位职责：参与后端系统开发，要求熟悉 Java、Spring Boot、MySQL、Redis...",
  "skillRequirements": "Java, Spring Boot, MySQL, Redis",
  "salaryRange": "200-400元/天",
  "workDaysPerWeek": "5天/周",
  "internshipDuration": "3个月"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| companyName | String | 是 | 公司名称 |
| jobTitle | String | 是 | 岗位名称 |
| jobType | String | 否 | 岗位类型 |
| location | String | 否 | 工作地点 |
| sourcePlatform | String | 否 | 来源平台 |
| jobUrl | String | 否 | 岗位链接 |
| jdContent | String | 是 | 岗位 JD 原文 |
| skillRequirements | String | 否 | 技能要求 |
| salaryRange | String | 否 | 薪资范围 |
| workDaysPerWeek | String | 否 | 每周工作天数 |
| internshipDuration | String | 否 | 实习周期 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "jobId": 1,
    "companyName": "腾讯",
    "jobTitle": "Java后端开发实习生",
    "jobType": "Java后端",
    "location": "深圳",
    "createdAt": "2026-05-05 20:20:00"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 公司名称为空 | 400 | 公司名称不能为空 |
| 岗位名称为空 | 400 | 岗位名称不能为空 |
| JD 内容为空 | 400 | 岗位 JD 不能为空 |
| URL 格式错误 | 400 | 岗位链接格式不正确 |

### 9.2 查询岗位列表

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/jobs |
| Method | GET |
| 权限 | USER |

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | String | 否 | 无 | 公司名或岗位名关键词 |
| jobType | String | 否 | 无 | 岗位类型 |
| location | String | 否 | 无 | 工作地点 |
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 10 | 每页数量 |

请求示例：

```http
GET /api/jobs?keyword=Java&jobType=Java后端&pageNum=1&pageSize=10
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "jobId": 1,
        "companyName": "腾讯",
        "jobTitle": "Java后端开发实习生",
        "jobType": "Java后端",
        "location": "深圳",
        "sourcePlatform": "Boss直聘",
        "salaryRange": "200-400元/天",
        "createdAt": "2026-05-05 20:20:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

业务规则：

1. 只返回当前用户创建的岗位；
2. 默认按创建时间倒序；
3. 支持关键词搜索；
4. 支持岗位类型筛选。

### 9.3 查询岗位详情

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/jobs/{id} |
| Method | GET |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "jobId": 1,
    "companyName": "腾讯",
    "jobTitle": "Java后端开发实习生",
    "jobType": "Java后端",
    "location": "深圳",
    "sourcePlatform": "Boss直聘",
    "jobUrl": "https://example.com/job/123",
    "jdContent": "岗位职责：参与后端系统开发...",
    "skillRequirements": "Java, Spring Boot, MySQL, Redis",
    "salaryRange": "200-400元/天",
    "workDaysPerWeek": "5天/周",
    "internshipDuration": "3个月",
    "createdAt": "2026-05-05 20:20:00",
    "updatedAt": "2026-05-05 20:20:00"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 岗位不存在 | 404 | 岗位不存在 |
| 访问他人岗位 | 403 | 无权限访问该岗位 |

### 9.4 修改岗位 JD

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/jobs/{id} |
| Method | PUT |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "companyName": "腾讯",
  "jobTitle": "Java后端开发实习生",
  "jobType": "Java后端",
  "location": "深圳",
  "sourcePlatform": "Boss直聘",
  "jobUrl": "https://example.com/job/123",
  "jdContent": "修改后的岗位 JD 内容...",
  "skillRequirements": "Java, Spring Boot, MySQL, Redis, Docker",
  "salaryRange": "200-400元/天",
  "workDaysPerWeek": "5天/周",
  "internshipDuration": "3个月"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

业务规则：

1. 只能修改自己的岗位；
2. 修改 JD 后，不自动修改历史 AI 分析报告；
3. 后续可以提示用户重新分析。

### 9.5 删除岗位 JD

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/jobs/{id} |
| Method | DELETE |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

业务规则：

1. 只能删除自己的岗位；
2. 第一阶段建议逻辑删除；
3. 历史分析报告和投递记录可以保留。

---

## 10. AI 分析接口

### 10.1 简历岗位匹配分析

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/analysis/match |
| Method | POST |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "resumeId": 1,
  "jobId": 1,
  "forceRefresh": false
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| resumeId | Long | 是 | 简历 ID |
| jobId | Long | 是 | 岗位 ID |
| forceRefresh | Boolean | 否 | 是否强制重新分析，不走缓存 |

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
    "createdAt": "2026-05-05 20:30:00"
  }
}
```

业务流程：

1. 校验用户是否登录；
2. 校验 `resumeId` 是否存在；
3. 校验 `jobId` 是否存在；
4. 校验简历是否属于当前用户；
5. 校验岗位是否属于当前用户；
6. 如果 `forceRefresh=false`，先检查 Redis 缓存；
7. 缓存命中则返回缓存结果；
8. 缓存未命中则构造 Prompt；
9. 调用 AI API；
10. 解析 AI 返回结果；
11. 保存分析报告；
12. 写入 Redis 缓存；
13. 返回分析结果。

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| resumeId 为空 | 400 | 简历 ID 不能为空 |
| jobId 为空 | 400 | 岗位 ID 不能为空 |
| 简历不存在 | 404 | 简历不存在 |
| 岗位不存在 | 404 | 岗位不存在 |
| 访问他人简历 | 403 | 无权限访问该简历 |
| 访问他人岗位 | 403 | 无权限访问该岗位 |
| 简历文本为空 | 400 | 简历解析文本为空 |
| 岗位 JD 为空 | 400 | 岗位 JD 不能为空 |
| AI 调用失败 | 600 | AI 分析服务暂时不可用 |
| AI 返回解析失败 | 600 | AI 返回结果解析失败 |

### 10.2 查询分析报告列表

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/analysis/reports |
| Method | GET |
| 权限 | USER |

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| resumeId | Long | 否 | 无 | 按简历筛选 |
| jobId | Long | 否 | 无 | 按岗位筛选 |
| minScore | Integer | 否 | 无 | 最低匹配分数 |
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 10 | 每页数量 |

请求示例：

```http
GET /api/analysis/reports?minScore=70&pageNum=1&pageSize=10
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "reportId": 1,
        "resumeId": 1,
        "jobId": 1,
        "companyName": "腾讯",
        "jobTitle": "Java后端开发实习生",
        "matchScore": 82,
        "matchLevel": "MEDIUM_HIGH",
        "cacheHit": false,
        "createdAt": "2026-05-05 20:30:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

业务规则：

1. 只能查询当前用户的分析报告；
2. 默认按创建时间倒序；
3. 列表不返回完整 AI 原始响应；
4. 详情接口返回完整内容。

### 10.3 查询分析报告详情

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/analysis/reports/{id} |
| Method | GET |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 1,
    "resumeId": 1,
    "resumeName": "Java后端实习简历",
    "jobId": 1,
    "companyName": "腾讯",
    "jobTitle": "Java后端开发实习生",
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
    "aiProvider": "deepseek",
    "aiModel": "deepseek-chat",
    "cacheHit": false,
    "createdAt": "2026-05-05 20:30:00"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 报告不存在 | 404 | 分析报告不存在 |
| 访问他人报告 | 403 | 无权限访问该分析报告 |

### 10.4 生成面试问题

这个接口属于第二阶段功能，可以先设计，后续实现。

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/analysis/interview-questions |
| Method | POST |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "resumeId": 1,
  "jobId": 1
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "basicQuestions": [
      "请介绍一下 JVM 内存结构",
      "HashMap 的底层原理是什么"
    ],
    "frameworkQuestions": [
      "Spring Boot 自动配置原理是什么",
      "Spring Security 过滤链是怎么执行的"
    ],
    "projectQuestions": [
      "你的校园搭子系统中 JWT 是如何实现的",
      "你为什么使用 Redis"
    ],
    "databaseQuestions": [
      "MySQL 索引失效的场景有哪些"
    ]
  }
}
```

---

## 11. 投递记录接口

### 11.1 创建投递记录

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/applications |
| Method | POST |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "jobId": 1,
  "resumeId": 1,
  "reportId": 1,
  "status": "TO_APPLY",
  "applyDate": "2026-05-05",
  "interviewDate": null,
  "note": "准备投递该岗位",
  "priority": "HIGH"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| jobId | Long | 是 | 岗位 ID |
| resumeId | Long | 否 | 简历 ID |
| reportId | Long | 否 | 分析报告 ID |
| status | String | 否 | 投递状态 |
| applyDate | Date | 否 | 投递日期 |
| interviewDate | DateTime | 否 | 面试时间 |
| note | String | 否 | 备注 |
| priority | String | 否 | 优先级 |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "applicationId": 1,
    "jobId": 1,
    "resumeId": 1,
    "reportId": 1,
    "status": "TO_APPLY",
    "priority": "HIGH",
    "createdAt": "2026-05-05 20:40:00"
  }
}
```

业务规则：

1. 岗位必须属于当前用户；
2. 简历如果填写，也必须属于当前用户；
3. 报告如果填写，也必须属于当前用户；
4. `status` 为空时默认为 `TO_APPLY`；
5. `priority` 为空时默认为 `MEDIUM`；
6. 同一用户同一岗位可以限制只创建一条投递记录。

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| jobId 为空 | 400 | 岗位 ID 不能为空 |
| 岗位不存在 | 404 | 岗位不存在 |
| 访问他人岗位 | 403 | 无权限访问该岗位 |
| 简历不存在 | 404 | 简历不存在 |
| 状态非法 | 400 | 投递状态不合法 |
| 重复创建 | 409 | 该岗位已存在投递记录 |

### 11.2 查询投递记录列表

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/applications |
| Method | GET |
| 权限 | USER |

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| status | String | 否 | 无 | 投递状态 |
| keyword | String | 否 | 无 | 公司或岗位关键词 |
| startDate | Date | 否 | 无 | 投递开始日期 |
| endDate | Date | 否 | 无 | 投递结束日期 |
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 10 | 每页数量 |

请求示例：

```http
GET /api/applications?status=APPLIED&pageNum=1&pageSize=10
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "applicationId": 1,
        "companyName": "腾讯",
        "jobTitle": "Java后端开发实习生",
        "status": "APPLIED",
        "priority": "HIGH",
        "applyDate": "2026-05-05",
        "interviewDate": null,
        "note": "已投递，等待反馈",
        "updatedAt": "2026-05-05 20:45:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

业务规则：

1. 只能查询当前用户的投递记录；
2. 支持状态筛选；
3. 支持关键词搜索；
4. 支持分页；
5. 默认按更新时间倒序。

### 11.3 查询投递记录详情

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/applications/{id} |
| Method | GET |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "applicationId": 1,
    "jobId": 1,
    "resumeId": 1,
    "reportId": 1,
    "companyName": "腾讯",
    "jobTitle": "Java后端开发实习生",
    "status": "APPLIED",
    "priority": "HIGH",
    "applyDate": "2026-05-05",
    "interviewDate": null,
    "note": "已投递，等待反馈",
    "review": null,
    "createdAt": "2026-05-05 20:40:00",
    "updatedAt": "2026-05-05 20:45:00"
  }
}
```

异常情况：

| 场景 | 错误码 | message |
|---|---|---|
| 记录不存在 | 404 | 投递记录不存在 |
| 访问他人记录 | 403 | 无权限访问该投递记录 |

### 11.4 修改投递状态

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/applications/{id}/status |
| Method | PUT |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "status": "APPLIED"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

投递状态枚举：

| 状态 | 说明 |
|---|---|
| TO_APPLY | 待投递 |
| APPLIED | 已投递 |
| WRITTEN_TEST | 笔试中 |
| FIRST_INTERVIEW | 一面 |
| SECOND_INTERVIEW | 二面 |
| HR_INTERVIEW | HR 面 |
| OFFER | 已 Offer |
| REJECTED | 被拒 |
| GIVEN_UP | 放弃 |

业务规则：

1. 只能修改自己的投递记录；
2. 状态必须属于枚举值；
3. 修改状态后更新 `updatedAt`；
4. 后续可以写入状态变化日志。

### 11.5 修改投递备注

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/applications/{id}/note |
| Method | PUT |
| 权限 | USER |
| Content-Type | application/json |

请求参数：

```json
{
  "note": "已完成一面，主要问了 Spring Security 和 Redis。",
  "review": "Spring Security 过滤链回答不够清楚，需要复习。",
  "interviewDate": "2026-05-10 14:00:00"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

业务规则：

1. 只能修改自己的投递记录；
2. `note` 和 `review` 长度需要限制；
3. `interviewDate` 可以为空。

### 11.6 删除投递记录

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/applications/{id} |
| Method | DELETE |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

业务规则：

1. 只能删除自己的投递记录；
2. 第一阶段采用逻辑删除；
3. 删除后列表不再显示。

---

## 12. 数据看板接口，第二阶段

### 12.1 查询个人投递统计

基本信息：

| 项目 | 内容 |
|---|---|
| URL | /api/dashboard/summary |
| Method | GET |
| 权限 | USER |

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalApplications": 20,
    "toApplyCount": 5,
    "appliedCount": 8,
    "interviewCount": 4,
    "offerCount": 1,
    "rejectedCount": 2,
    "averageMatchScore": 76.5
  }
}
```

---

## 13. DTO 设计建议

### 13.1 auth 包

1. `RegisterRequest`
2. `LoginRequest`
3. `LoginResponse`

### 13.2 user 包

1. `UserProfileResponse`
2. `UpdateUserProfileRequest`

### 13.3 resume 包

1. `ResumeUploadResponse`
2. `ResumeListResponse`
3. `ResumeDetailResponse`

### 13.4 job 包

1. `JobCreateRequest`
2. `JobUpdateRequest`
3. `JobListResponse`
4. `JobDetailResponse`

### 13.5 analysis 包

1. `AnalysisMatchRequest`
2. `AnalysisReportListResponse`
3. `AnalysisReportDetailResponse`
4. `AnalysisResultResponse`
5. `InterviewQuestionRequest`
6. `InterviewQuestionResponse`

### 13.6 application 包

1. `ApplicationCreateRequest`
2. `ApplicationListResponse`
3. `ApplicationDetailResponse`
4. `ApplicationStatusUpdateRequest`
5. `ApplicationNoteUpdateRequest`

---

## 14. Swagger 注解规划

### 14.1 Controller 注解

每个 Controller 使用：

```java
@Tag(name = "用户认证接口")
@RestController
@RequestMapping("/api/auth")
```

### 14.2 接口注解

每个接口使用：

```java
@Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统")
```

### 14.3 DTO 字段注解

DTO 字段使用：

```java
@Schema(description = "用户名", example = "wan")
@NotBlank(message = "用户名不能为空")
private String username;
```

### 14.4 示例

```java
@Tag(name = "用户认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
```

---

## 15. 接口安全设计

### 15.1 防止越权访问

所有涉及用户资源的接口都必须校验当前登录用户。

示例，查询简历详情：

```sql
resume.id = id
AND resume.user_id = currentUserId
AND resume.deleted = 0
```

不能只根据 `id` 查询。

### 15.2 文件上传安全

文件上传接口必须校验：

1. 文件不能为空；
2. 文件大小不能超过 10MB；
3. 文件类型必须是 PDF 或 DOCX；
4. 存储文件名不能直接使用用户上传文件名；
5. 文件路径不能由用户直接传入；
6. 后续可增加病毒扫描或文件内容校验。

### 15.3 AI 接口安全

AI 分析接口必须注意：

1. 用户只能分析自己的简历；
2. 用户只能分析自己的岗位；
3. 不在日志中打印完整简历内容；
4. AI API Key 不能返回前端；
5. AI API Key 不能提交到 GitHub；
6. 限制单用户调用频率，后续扩展。

---

## 16. 接口测试建议

### 16.1 Postman / Apifox 测试顺序

建议按照以下顺序测试：

1. 注册用户
2. 登录获取 Token
3. 查询当前用户信息
4. 上传简历
5. 查询简历列表
6. 查询简历详情
7. 创建岗位 JD
8. 查询岗位列表
9. 查询岗位详情
10. 发起 AI 匹配分析
11. 查询分析报告列表
12. 查询分析报告详情
13. 创建投递记录
14. 查询投递记录
15. 修改投递状态
16. 修改投递备注
17. 删除投递记录

### 16.2 curl 测试示例

登录：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "wan",
    "password": "123456"
  }'
```

携带 Token 查询用户信息：

```bash
curl -X GET http://localhost:8080/api/user/me \
  -H "Authorization: Bearer YOUR_TOKEN"
```

创建岗位 JD：

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "companyName": "腾讯",
    "jobTitle": "Java后端开发实习生",
    "jobType": "Java后端",
    "location": "深圳",
    "jdContent": "要求熟悉 Java、Spring Boot、MySQL、Redis"
  }'
```

---

## 17. PowerShell 测试提醒

如果你在 Windows PowerShell 里测试，不建议直接使用 Linux 风格的反斜杠换行。

推荐使用 `curl.exe` 或 `Invoke-RestMethod`。

### 17.1 curl.exe 示例

```powershell
curl.exe -X POST "http://localhost:8080/api/auth/login" `
  -H "Content-Type: application/json" `
  -d "{`"username`":`"wan`",`"password`":`"123456`"}"
```

### 17.2 Invoke-RestMethod 示例

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

携带 Token：

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/user/me" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

---

## 18. 第一阶段接口验收标准

第一阶段完成后，接口需要满足：

### 18.1 用户认证

1. 用户可以注册；
2. 用户可以登录；
3. 登录成功返回 JWT；
4. 未登录访问业务接口返回 401；
5. Token 无效访问业务接口返回 401。

### 18.2 用户信息

1. 用户可以查询自己的信息；
2. 用户可以修改自己的基础信息；
3. 返回结果不包含密码。

### 18.3 简历接口

1. 用户可以上传 PDF 简历；
2. 用户可以上传 DOCX 简历；
3. 用户可以查看自己的简历列表；
4. 用户可以查看自己的简历详情；
5. 用户不能查看他人的简历；
6. 用户可以设置默认简历。

### 18.4 岗位接口

1. 用户可以创建岗位；
2. 用户可以查看自己的岗位列表；
3. 用户可以查看自己的岗位详情；
4. 用户可以修改自己的岗位；
5. 用户可以删除自己的岗位；
6. 用户不能访问他人的岗位。

### 18.5 AI 分析接口

1. 用户可以选择自己的简历和岗位进行分析；
2. 系统可以返回匹配分数；
3. 系统可以返回优势、短板、技能缺口和优化建议；
4. 分析报告可以保存；
5. 用户可以查询历史分析报告；
6. 用户不能访问他人的分析报告；
7. AI 服务异常时返回统一错误响应。

### 18.6 投递接口

1. 用户可以创建投递记录；
2. 用户可以查询自己的投递记录；
3. 用户可以修改投递状态；
4. 用户可以修改备注和复盘；
5. 用户可以删除投递记录；
6. 用户不能访问他人的投递记录。

---

## 19. 接口设计结论

InternPilot 第一阶段接口设计围绕以下核心业务闭环展开：

```text
用户注册登录
  ↓
上传简历
  ↓
创建岗位 JD
  ↓
AI 匹配分析
  ↓
查看分析报告
  ↓
创建投递记录
  ↓
跟踪投递状态
```

接口整体采用 RESTful 风格，并通过 JWT 进行身份认证。所有用户业务数据接口都必须校验当前用户身份，防止越权访问。

第一阶段优先实现用户认证、简历管理、岗位管理、AI 分析和投递记录五大核心模块接口。数据看板、面试题生成、管理员管理等接口可以作为第二阶段扩展。
