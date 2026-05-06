# InternPilot API 接口测试指南

## 1. 文档说明

本文档用于整理 InternPilot 后端接口的完整测试流程，帮助开发者从零开始验证系统是否能够跑通完整业务闭环。

测试目标：

1. 验证项目是否启动成功；
2. 验证用户注册登录是否正常；
3. 验证 JWT 鉴权是否生效；
4. 验证简历上传与解析是否正常；
5. 验证岗位 JD 管理是否正常；
6. 验证 AI 匹配分析是否正常；
7. 验证分析报告查询是否正常；
8. 验证投递记录管理是否正常；
9. 验证未登录、非法参数、越权访问等异常场景；
10. 为 README、演示和面试提供接口验收依据。

---

## 2. 测试前置条件

### 2.1 环境要求

| 环境 | 要求 |
| --- | --- |
| JDK | Java 17 |
| MySQL | 8.x |
| Redis | 7.x |
| Gradle | 使用项目自带 Wrapper |
| 后端端口 | 8080 |
| 接口文档 | Knife4j / Swagger |

### 2.2 数据库准备

确保 MySQL 已经创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

确保已经执行：

```text
src/main/resources/sql/init.sql
```

至少需要存在以下表：

1. `user`
2. `resume`
3. `job_description`
4. `analysis_report`
5. `application_record`

### 2.3 Redis 准备

启动 Redis 后测试：

```powershell
redis-cli ping
```

期望返回：

```text
PONG
```

### 2.4 AI 配置准备

如果使用真实 AI API，需要设置环境变量：

Windows PowerShell：

```powershell
$env:AI_API_KEY="你的 API Key"
```

Linux / macOS：

```bash
export AI_API_KEY="你的 API Key"
```

开发阶段建议优先使用 Mock AI，避免 API Key、额度和网络问题。

## 3. 启动项目

### 3.1 Windows PowerShell

进入后端目录：

```powershell
cd backend/intern-pilot-backend
```

启动：

```powershell
.\gradlew.bat bootRun
```

或者：

```powershell
./gradlew bootRun
```

### 3.2 Linux / macOS

```bash
cd backend/intern-pilot-backend
./gradlew bootRun
```

### 3.3 启动成功标志

控制台看到类似：

```text
Tomcat started on port 8080
Started InternPilotApplication
```

表示项目启动成功。

## 4. 测试工具说明

推荐使用以下任一方式测试：

1. Knife4j / Swagger；
2. Postman；
3. Apifox；
4. PowerShell `Invoke-RestMethod`；
5. `curl.exe`。

本文主要提供 PowerShell 测试脚本，因为你在 Windows 环境下使用较多。

接口文档地址：

```text
http://localhost:8080/doc.html
```

健康检查地址：

```text
http://localhost:8080/api/health
```

## 5. 测试流程总览

完整业务闭环测试顺序：

1. 健康检查
2. 用户注册
3. 用户登录
4. 保存 JWT Token
5. 查询当前用户信息
6. 上传简历
7. 查询简历列表
8. 查询简历详情
9. 创建岗位 JD
10. 查询岗位列表
11. 查询岗位详情
12. 发起 AI 匹配分析
13. 查询分析报告列表
14. 查询分析报告详情
15. 创建投递记录
16. 查询投递记录列表
17. 查询投递记录详情
18. 修改投递状态
19. 修改投递备注和复盘
20. 删除投递记录
21. 异常场景测试

## 6. 全局变量准备

建议先在 PowerShell 中定义基础变量：

```powershell
$baseUrl = "http://localhost:8080"
```

后续登录成功后会设置：

```powershell
$token = ""
$headers = @{ Authorization = "Bearer $token" }
```

## 7. 健康检查测试

### 7.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/health" `
  -Method Get
```

### 7.2 期望响应

```json
{
  "code": 200,
  "message": "success",
  "data": "InternPilot backend is running"
}
```

## 8. 用户注册测试

### 8.1 请求

```powershell
$registerBody = @{
  username = "wan"
  password = "123456"
  confirmPassword = "123456"
  email = "wan@example.com"
  school = "西南大学"
  major = "软件工程"
  grade = "大二"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/auth/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body $registerBody
```

### 8.2 期望响应

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

### 8.3 数据库检查

```sql
SELECT id, username, password, role, enabled
FROM user
WHERE username = 'wan';
```

检查点：

1. `password` 不应该是明文 `123456`
2. `role` 应该是 `USER`
3. `enabled` 应该是 `1`

## 9. 重复注册测试

### 9.1 请求

再次执行注册请求。

### 9.2 期望响应

```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}
```

如果你的项目使用 409 表示冲突，也可以返回：

```json
{
  "code": 409,
  "message": "用户名已存在",
  "data": null
}
```

## 10. 用户登录测试

### 10.1 请求

```powershell
$loginBody = @{
  username = "wan"
  password = "123456"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
  -Uri "$baseUrl/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $loginBody

$loginResponse
```

### 10.2 期望响应

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

## 11. 保存 Token

```powershell
$token = $loginResponse.data.token
$headers = @{ Authorization = "Bearer $token" }

$token
```

后续所有需要登录的接口都使用：

```powershell
-Headers $headers
```

## 12. 查询当前用户信息

### 12.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/user/me" `
  -Method Get `
  -Headers $headers
```

### 12.2 期望响应

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

## 13. 未登录访问测试

### 13.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/user/me" `
  -Method Get
```

### 13.2 期望响应

```json
{
  "code": 401,
  "message": "请先登录或 Token 已失效",
  "data": null
}
```

说明：

如果 PowerShell 抛出异常，可以用 `try/catch` 查看响应体。

## 14. 上传简历测试

### 14.1 准备测试文件

准备一份 PDF 或 DOCX 简历，例如：

```text
D:\resume.pdf
```

或者：

```text
D:\resume.docx
```

### 14.2 PowerShell 上传 PDF

```powershell
$form = @{
  file = Get-Item "D:\resume.pdf"
  resumeName = "Java后端实习简历"
}

$resumeUploadResponse = Invoke-RestMethod `
  -Uri "$baseUrl/api/resumes/upload" `
  -Method Post `
  -Headers $headers `
  -Form $form

$resumeUploadResponse
```

如果你的 PowerShell 不支持 `-Form`，使用 `curl.exe`。

### 14.3 curl.exe 上传 PDF

```powershell
curl.exe -X POST "$baseUrl/api/resumes/upload" `
  -H "Authorization: Bearer $token" `
  -F "file=@D:\resume.pdf" `
  -F "resumeName=Java后端实习简历"
```

### 14.4 期望响应

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
    "isDefault": true,
    "createdAt": "2026-05-06T20:10:00"
  }
}
```

### 14.5 保存 resumeId

```powershell
$resumeId = $resumeUploadResponse.data.resumeId
$resumeId
```

如果使用 `curl.exe`，需要手动记录响应中的 `resumeId`。

## 15. 查询简历列表

### 15.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/resumes?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

### 15.2 检查点

1. `code = 200`
2. `records` 中包含刚上传的简历
3. `fileType = PDF` 或 `DOCX`
4. `parseStatus = SUCCESS`
5. 列表中不应该返回完整 `parsedText`

## 16. 查询简历详情

### 16.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/resumes/$resumeId" `
  -Method Get `
  -Headers $headers
```

### 16.2 检查点

1. 能看到 `parsedText`
2. `resumeId` 正确
3. 只能看到当前用户的简历

## 17. 创建岗位 JD

### 17.1 请求

```powershell
$jobBody = @{
  companyName = "腾讯"
  jobTitle = "Java后端开发实习生"
  jobType = "Java后端"
  location = "深圳"
  sourcePlatform = "Boss直聘"
  jobUrl = "https://example.com/job/123"
  jdContent = "岗位职责：参与后端系统开发，要求熟悉 Java、Spring Boot、MySQL、Redis，有良好的编码习惯。"
  skillRequirements = "Java, Spring Boot, MySQL, Redis"
  salaryRange = "200-400元/天"
  workDaysPerWeek = "5天/周"
  internshipDuration = "3个月"
} | ConvertTo-Json

$jobCreateResponse = Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs" `
  -Method Post `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $jobBody

$jobCreateResponse
```

### 17.2 期望响应

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
    "createdAt": "2026-05-06T20:20:00"
  }
}
```

### 17.3 保存 jobId

```powershell
$jobId = $jobCreateResponse.data.jobId
$jobId
```

## 18. 查询岗位列表

### 18.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

### 18.2 关键词搜索

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs?keyword=Java&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

### 18.3 岗位类型筛选

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs?jobType=Java后端&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

## 19. 查询岗位详情

### 19.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs/$jobId" `
  -Method Get `
  -Headers $headers
```

### 19.2 检查点

1. 能看到完整 `jdContent`
2. `companyName` 正确
3. `jobTitle` 正确

## 20. 发起 AI 匹配分析

### 20.1 请求

```powershell
$analysisBody = @{
  resumeId = $resumeId
  jobId = $jobId
  forceRefresh = $false
} | ConvertTo-Json

$analysisResponse = Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/match" `
  -Method Post `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $analysisBody

$analysisResponse
```

### 20.2 期望响应

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
      "具备 Spring Boot 项目经验"
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
    "cacheHit": false,
    "createdAt": "2026-05-06T21:00:00"
  }
}
```

### 20.3 保存 reportId

```powershell
$reportId = $analysisResponse.data.reportId
$reportId
```

## 21. AI 缓存测试

### 21.1 第二次调用相同分析

```powershell
$analysisResponse2 = Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/match" `
  -Method Post `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $analysisBody

$analysisResponse2
```

### 21.2 期望结果

```text
cacheHit = true
```

### 21.3 Redis 检查

```powershell
redis-cli keys "internpilot:analysis:*"
```

期望看到类似：

```text
internpilot:analysis:1:1:1
```

## 22. forceRefresh 重新分析测试

### 22.1 请求

```powershell
$analysisRefreshBody = @{
  resumeId = $resumeId
  jobId = $jobId
  forceRefresh = $true
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/match" `
  -Method Post `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $analysisRefreshBody
```

### 22.2 检查点

1. 应该重新调用 AI
2. `cacheHit = false`
3. 数据库新增一条 `analysis_report` 记录

## 23. 查询分析报告列表

### 23.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/reports?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

### 23.2 按最低分筛选

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/reports?minScore=70&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

## 24. 查询分析报告详情

### 24.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/reports/$reportId" `
  -Method Get `
  -Headers $headers
```

### 24.2 检查点

1. `reportId` 正确
2. `resumeId` 正确
3. `jobId` 正确
4. 能看到 `strengths / weaknesses / suggestions`

## 25. 创建投递记录

### 25.1 请求

```powershell
$applicationBody = @{
  jobId = $jobId
  resumeId = $resumeId
  reportId = $reportId
  status = "TO_APPLY"
  applyDate = "2026-05-06"
  note = "准备投递该岗位"
  priority = "HIGH"
} | ConvertTo-Json

$applicationResponse = Invoke-RestMethod `
  -Uri "$baseUrl/api/applications" `
  -Method Post `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $applicationBody

$applicationResponse
```

### 25.2 期望响应

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
    "createdAt": "2026-05-06T21:30:00"
  }
}
```

### 25.3 保存 applicationId

```powershell
$applicationId = $applicationResponse.data.applicationId
$applicationId
```

## 26. 查询投递记录列表

### 26.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

### 26.2 按状态筛选

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications?status=TO_APPLY&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

### 26.3 关键词搜索

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications?keyword=腾讯&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

## 27. 查询投递记录详情

### 27.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications/$applicationId" `
  -Method Get `
  -Headers $headers
```

### 27.2 检查点

1. 能看到 `companyName`
2. 能看到 `jobTitle`
3. 能看到 `resumeName`
4. 能看到 `matchScore`
5. 能看到 `matchLevel`
6. 能看到 `status`

## 28. 修改投递状态

### 28.1 请求

```powershell
$statusBody = @{
  status = "APPLIED"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/applications/$applicationId/status" `
  -Method Put `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $statusBody
```

### 28.2 验证

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications/$applicationId" `
  -Method Get `
  -Headers $headers
```

检查：

```text
status = APPLIED
```

## 29. 修改投递备注和复盘

### 29.1 请求

```powershell
$noteBody = @{
  note = "已投递，等待 HR 反馈。"
  review = "后续重点复习 Spring Security、Redis 缓存和 MySQL 索引。"
  interviewDate = "2026-05-10T14:00:00"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/applications/$applicationId/note" `
  -Method Put `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $noteBody
```

### 29.2 验证

查询详情，检查：

1. `note` 已更新
2. `review` 已更新
3. `interviewDate` 已更新

## 30. 删除投递记录

### 30.1 请求

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications/$applicationId" `
  -Method Delete `
  -Headers $headers
```

### 30.2 验证

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers $headers
```

检查：

```text
删除后的记录不再出现在列表中
```

数据库检查：

```sql
SELECT id, deleted
FROM application_record
WHERE id = 1;
```

期望：

```text
deleted = 1
```

## 31. 异常测试

### 31.1 错误密码登录

```powershell
$wrongLoginBody = @{
  username = "wan"
  password = "wrong-password"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $wrongLoginBody
```

期望：

```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

或者：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

### 31.2 不带 Token 创建岗位

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs" `
  -Method Post `
  -ContentType "application/json" `
  -Body $jobBody
```

期望：

```text
401
```

### 31.3 上传非法文件

```powershell
curl.exe -X POST "$baseUrl/api/resumes/upload" `
  -H "Authorization: Bearer $token" `
  -F "file=@D:\test.exe" `
  -F "resumeName=非法文件测试"
```

期望：

```json
{
  "code": 700,
  "message": "仅支持 PDF 或 DOCX 文件",
  "data": null
}
```

### 31.4 创建岗位时 JD 为空

```powershell
$badJobBody = @{
  companyName = "腾讯"
  jobTitle = "Java后端开发实习生"
  jdContent = ""
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs" `
  -Method Post `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $badJobBody
```

期望：

```json
{
  "code": 400,
  "message": "岗位 JD 不能为空",
  "data": null
}
```

### 31.5 AI 分析传不存在的 resumeId

```powershell
$badAnalysisBody = @{
  resumeId = 999999
  jobId = $jobId
  forceRefresh = $false
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/match" `
  -Method Post `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $badAnalysisBody
```

期望：

```json
{
  "code": 400,
  "message": "简历不存在或无权限访问",
  "data": null
}
```

### 31.6 修改非法投递状态

```powershell
$badStatusBody = @{
  status = "UNKNOWN_STATUS"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/applications/$applicationId/status" `
  -Method Put `
  -ContentType "application/json" `
  -Headers $headers `
  -Body $badStatusBody
```

期望：

```json
{
  "code": 400,
  "message": "投递状态不合法",
  "data": null
}
```

## 32. 越权访问测试

### 32.1 测试思路

越权访问测试需要准备两个用户：

1. 用户 A：`wan`
2. 用户 B：`testuser`

用户 A 创建简历、岗位、分析报告和投递记录。

用户 B 登录后，尝试访问用户 A 的资源 ID。

### 32.2 注册用户 B

```powershell
$userBRegisterBody = @{
  username = "testuser"
  password = "123456"
  confirmPassword = "123456"
  email = "testuser@example.com"
  school = "测试大学"
  major = "软件工程"
  grade = "大二"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$baseUrl/api/auth/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body $userBRegisterBody
```

### 32.3 登录用户 B

```powershell
$userBLoginBody = @{
  username = "testuser"
  password = "123456"
} | ConvertTo-Json

$userBLoginResponse = Invoke-RestMethod `
  -Uri "$baseUrl/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $userBLoginBody

$userBToken = $userBLoginResponse.data.token
$userBHeaders = @{ Authorization = "Bearer $userBToken" }
```

### 32.4 用户 B 访问用户 A 的简历

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/resumes/$resumeId" `
  -Method Get `
  -Headers $userBHeaders
```

期望：

不能访问。

可返回：

```json
{
  "code": 400,
  "message": "简历不存在或无权限访问",
  "data": null
}
```

### 32.5 用户 B 访问用户 A 的岗位

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/jobs/$jobId" `
  -Method Get `
  -Headers $userBHeaders
```

期望：

```text
不能访问
```

### 32.6 用户 B 访问用户 A 的分析报告

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/analysis/reports/$reportId" `
  -Method Get `
  -Headers $userBHeaders
```

期望：

```text
不能访问
```

### 32.7 用户 B 访问用户 A 的投递记录

```powershell
Invoke-RestMethod `
  -Uri "$baseUrl/api/applications/$applicationId" `
  -Method Get `
  -Headers $userBHeaders
```

期望：

```text
不能访问
```

## 33. Docker Compose 测试

### 33.1 启动

```powershell
cd deploy
docker compose up -d
```

### 33.2 查看服务

```powershell
docker compose ps
```

期望看到：

```text
internpilot-mysql     running
internpilot-redis     running
internpilot-backend   running
```

### 33.3 查看后端日志

```powershell
docker compose logs -f backend
```

### 33.4 健康检查

```powershell
curl http://localhost:8080/api/health
```

### 33.5 接口文档

浏览器访问：

```text
http://localhost:8080/doc.html
```

## 34. 数据库验收 SQL

### 34.1 用户表

```sql
SELECT id, username, role, enabled, deleted, created_at
FROM user;
```

### 34.2 简历表

```sql
SELECT id, user_id, resume_name, original_file_name, file_type, parse_status, is_default, deleted
FROM resume;
```

### 34.3 岗位表

```sql
SELECT id, user_id, company_name, job_title, job_type, location, deleted
FROM job_description;
```

### 34.4 分析报告表

```sql
SELECT id, user_id, resume_id, job_id, match_score, match_level, cache_hit, deleted
FROM analysis_report;
```

### 34.5 投递记录表

```sql
SELECT id, user_id, job_id, resume_id, report_id, status, priority, deleted
FROM application_record;
```

## 35. 完整验收清单

### 35.1 用户认证

- 注册成功；
- 重复用户名不能注册；
- 登录成功返回 Token；
- 错误密码不能登录；
- 不带 Token 访问业务接口返回 401；
- 携带 Token 可以访问业务接口。

### 35.2 简历模块

- 可以上传 PDF；
- 可以上传 DOCX；
- 非法文件不能上传；
- 上传后数据库有记录；
- `parsedText` 不为空；
- 简历列表正常；
- 简历详情正常；
- 不能访问他人简历。

### 35.3 岗位模块

- 可以创建岗位；
- 公司名称不能为空；
- 岗位名称不能为空；
- JD 内容不能为空；
- 岗位列表正常；
- 岗位详情正常；
- 关键词搜索正常；
- 不能访问他人岗位。

### 35.4 AI 分析模块

- 可以发起 AI 分析；
- 返回 `matchScore`；
- 返回 `matchLevel`；
- 返回 `strengths`；
- 返回 `weaknesses`；
- 返回 `missingSkills`；
- 返回 `suggestions`；
- 返回 `interviewTips`；
- 报告保存到数据库；
- 第二次分析命中缓存；
- `forceRefresh` 可以重新分析；
- 不能分析他人简历或岗位。

### 35.5 投递记录模块

- 可以创建投递记录；
- 可以查询投递列表；
- 可以查询投递详情；
- 可以修改投递状态；
- 非法状态不能修改；
- 可以修改备注和复盘；
- 可以删除投递记录；
- 删除后列表不显示；
- 不能访问他人投递记录。

### 35.6 工程化

- Swagger / Knife4j 可访问；
- `/api/health` 可访问；
- 配置不包含真实密码；
- AI API Key 不提交到 GitHub；
- `uploads` 目录不提交到 GitHub；
- Docker Compose 可启动；
- README 有启动说明；
- docs 文档完整。

## 36. 常见问题

### 36.1 PowerShell 报错但后端其实返回了 JSON

`Invoke-RestMethod` 遇到 4xx / 5xx 时可能直接抛异常。

可以这样查看：

```powershell
try {
  Invoke-RestMethod `
    -Uri "$baseUrl/api/user/me" `
    -Method Get
} catch {
  $_.Exception.Response
}
```

或者使用 `curl.exe` 查看原始响应。

### 36.2 curl 在 PowerShell 中行为异常

PowerShell 中的 `curl` 可能不是原生 curl，而是别名。

建议使用：

```powershell
curl.exe
```

不要省略 `.exe`。

### 36.3 中文乱码

可以先执行：

```powershell
chcp 65001
```

并确保终端使用 UTF-8。

### 36.4 AI 分析接口很慢

可能原因：

1. AI API 响应慢；
2. 网络问题；
3. 简历文本太长；
4. 岗位 JD 太长；
5. 没有命中 Redis 缓存。

建议：

1. 开发阶段使用 Mock AI；
2. 限制 `parsedText` 长度；
3. 使用 Redis 缓存；
4. 后续改为异步任务。

## 37. 测试结论

当你完成本文档中的完整测试流程后，InternPilot 后端应该已经具备完整 MVP 闭环：

```text
用户注册登录
  ↓
JWT 鉴权
  ↓
上传简历
  ↓
创建岗位 JD
  ↓
AI 匹配分析
  ↓
保存分析报告
  ↓
创建投递记录
  ↓
跟踪投递状态
```

这说明项目已经具备进入下一阶段的条件：

1. README 完善
2. Docker 部署
3. 前端页面开发
4. 数据看板开发
5. 面试总结整理
