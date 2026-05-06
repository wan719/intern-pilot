# InternPilot 数据库设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的数据库设计，包括数据库命名规范、核心实体关系、数据表设计、字段说明、索引设计、状态枚举设计和完整建表 SQL。

InternPilot 是一个面向大学生实习求职场景的 AI 实习投递与简历优化平台，核心业务包括：

1. 用户注册登录；
2. 简历上传与解析；
3. 岗位 JD 管理；
4. AI 简历岗位匹配分析；
5. 分析报告管理；
6. 投递记录管理；
7. 后续数据看板与管理员管理。

---

## 2. 数据库设计目标

### 2.1 业务目标

数据库需要支持以下业务：

1. 保存用户基础信息；
2. 保存用户上传的简历文件信息和解析文本；
3. 保存用户创建的岗位 JD；
4. 保存 AI 分析报告；
5. 保存实习投递记录；
6. 支持用户维度的数据隔离；
7. 支持后续数据统计和功能扩展。

### 2.2 技术目标

数据库设计需要满足以下要求：

1. 表结构清晰；
2. 字段命名规范；
3. 支持逻辑删除；
4. 支持创建时间和更新时间；
5. 关键字段建立索引；
6. 支持用户数据权限控制；
7. 方便 MyBatis / MyBatis Plus 映射；
8. 方便后续扩展 AI 调用日志、Prompt 模板、用户画像等功能。

---

## 3. 数据库命名规范

### 3.1 数据库名称

建议数据库名称：

```sql
intern_pilot
```

### 3.2 表命名规范

1. 表名使用小写字母；
2. 多个单词使用下划线连接；
3. 表名使用单数或业务名词；
4. 不使用数据库关键字；
5. 业务表建议统一包含 `id`、`created_at`、`updated_at`、`deleted` 字段。

示例：

```text
user
resume
job_description
analysis_report
application_record
```

### 3.3 字段命名规范

1. 字段名使用小写字母；
2. 多个单词使用下划线连接；
3. 主键统一命名为 `id`；
4. 外键字段命名为 `{表名}_id` 或业务对象名 + `_id`；
5. 时间字段统一使用 `_at` 结尾；
6. 布尔字段可以使用 `is_` 前缀。

示例：

```text
user_id
resume_id
job_id
created_at
updated_at
is_default
```

### 3.4 Java 映射规范

数据库字段使用下划线命名，Java 实体类使用驼峰命名。

| 数据库字段 | Java 字段 |
|---|---|
| user_id | userId |
| file_name | fileName |
| parsed_text | parsedText |
| created_at | createdAt |
| updated_at | updatedAt |

MyBatis Plus 配置：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

---

## 4. 核心实体关系设计

### 4.1 核心实体

第一阶段核心实体包括：

| 实体 | 说明 |
|---|---|
| User | 用户 |
| Resume | 简历 |
| JobDescription | 岗位 JD |
| AnalysisReport | AI 分析报告 |
| ApplicationRecord | 投递记录 |

### 4.2 实体关系

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

### 4.3 关系说明

1. 一个用户可以上传多份简历；
2. 一个用户可以创建多个岗位 JD；
3. 一个用户可以拥有多份 AI 分析报告；
4. 一个用户可以拥有多条投递记录；
5. 一份简历可以与多个岗位进行匹配分析；
6. 一个岗位可以与多份简历进行匹配分析；
7. 一条投递记录可以关联一个岗位；
8. 一条投递记录可以关联一份简历；
9. 所有业务数据都必须绑定 `user_id`，用于数据权限控制。

### 4.4 ER 图文本版

```text
┌────────────────────┐
│        user         │
│--------------------│
│ id                 │
│ username           │
│ password           │
│ email              │
│ role               │
└─────────┬──────────┘
          │ 1
          │
          │ N
┌─────────▼──────────┐
│       resume        │
│--------------------│
│ id                 │
│ user_id            │
│ file_name          │
│ parsed_text        │
└─────────┬──────────┘
          │ 1
          │
          │ N
┌─────────▼──────────┐
│  analysis_report    │
│--------------------│
│ id                 │
│ user_id            │
│ resume_id          │
│ job_id             │
│ match_score        │
└─────────▲──────────┘
          │ N
          │
          │ 1
┌─────────┴──────────┐
│  job_description    │
│--------------------│
│ id                 │
│ user_id            │
│ company_name       │
│ job_title          │
│ jd_content         │
└─────────┬──────────┘
          │ 1
          │
          │ N
┌─────────▼──────────────┐
│   application_record    │
│------------------------│
│ id                     │
│ user_id                │
│ job_id                 │
│ resume_id              │
│ status                 │
└────────────────────────┘
```

---

## 5. 数据表总览

| 表名 | 中文名 | 说明 |
|---|---|---|
| user | 用户表 | 存储用户账号和个人信息 |
| resume | 简历表 | 存储简历文件信息和解析文本 |
| job_description | 岗位 JD 表 | 存储用户保存的岗位信息 |
| analysis_report | AI 分析报告表 | 存储简历与岗位的 AI 分析结果 |
| application_record | 投递记录表 | 存储实习投递进度 |
| ai_call_log | AI 调用日志表 | 第二阶段扩展 |
| prompt_template | Prompt 模板表 | 第二阶段扩展 |
| application_status_log | 投递状态日志表 | 第二阶段扩展 |

---

## 6. 用户表设计

### 6.1 表名

```text
user
```

### 6.2 表说明

用户表用于存储系统用户账号信息、个人基础信息和角色信息。

### 6.3 字段设计

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 用户 ID，主键 |
| username | VARCHAR(50) | 是 | 无 | 用户名，唯一 |
| password | VARCHAR(255) | 是 | 无 | 加密后的密码 |
| email | VARCHAR(100) | 否 | NULL | 邮箱 |
| phone | VARCHAR(20) | 否 | NULL | 手机号 |
| real_name | VARCHAR(50) | 否 | NULL | 真实姓名 |
| school | VARCHAR(100) | 否 | NULL | 学校 |
| major | VARCHAR(100) | 否 | NULL | 专业 |
| grade | VARCHAR(30) | 否 | NULL | 年级 |
| role | VARCHAR(30) | 是 | USER | 用户角色 |
| enabled | TINYINT | 是 | 1 | 是否启用 |
| last_login_at | DATETIME | 否 | NULL | 最后登录时间 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 是否逻辑删除 |

### 6.4 设计说明

1. `username` 必须唯一；
2. `password` 存储 BCrypt 加密后的密码；
3. `role` 第一阶段可以使用字符串保存：`USER`、`ADMIN`；
4. `enabled` 用于后续禁用用户；
5. `deleted` 用于逻辑删除；
6. 第一阶段不单独设计 role 和 permission 表，避免复杂度过高；
7. 如果后续做完整 RBAC，可以扩展 `role`、`permission`、`user_role`、`role_permission` 表。

---

## 7. 简历表设计

### 7.1 表名

```text
resume
```

### 7.2 表说明

简历表用于存储用户上传的简历文件信息、文件存储路径、文件类型和解析后的简历文本。

### 7.3 字段设计

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 简历 ID，主键 |
| user_id | BIGINT | 是 | 无 | 用户 ID |
| resume_name | VARCHAR(100) | 否 | NULL | 简历名称 |
| original_file_name | VARCHAR(255) | 是 | 无 | 原始文件名 |
| stored_file_name | VARCHAR(255) | 是 | 无 | 存储文件名 |
| file_path | VARCHAR(500) | 是 | 无 | 文件存储路径 |
| file_type | VARCHAR(20) | 是 | 无 | 文件类型，PDF/DOCX |
| file_size | BIGINT | 是 | 0 | 文件大小，单位字节 |
| parsed_text | LONGTEXT | 否 | NULL | 解析后的文本 |
| parse_status | VARCHAR(30) | 是 | SUCCESS | 解析状态 |
| is_default | TINYINT | 是 | 0 | 是否默认简历 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 是否逻辑删除 |

### 7.4 parse_status 枚举

| 值 | 说明 |
|---|---|
| SUCCESS | 解析成功 |
| FAILED | 解析失败 |
| PENDING | 等待解析 |

### 7.5 设计说明

1. `user_id` 用于绑定简历所属用户；
2. `original_file_name` 保存用户上传时的原始文件名；
3. `stored_file_name` 保存系统生成的文件名，避免重名；
4. `file_path` 保存文件路径；
5. `parsed_text` 保存解析后的简历文本，用于 AI 分析；
6. `is_default` 表示是否为默认简历；
7. 删除简历建议使用逻辑删除；
8. 简历列表接口不建议返回 `parsed_text`，详情接口再返回。

---

## 8. 岗位 JD 表设计

### 8.1 表名

```text
job_description
```

### 8.2 表说明

岗位 JD 表用于存储用户保存的实习岗位信息，包括公司名称、岗位名称、岗位类型、工作地点、岗位链接和岗位 JD 内容。

### 8.3 字段设计

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 岗位 ID，主键 |
| user_id | BIGINT | 是 | 无 | 用户 ID |
| company_name | VARCHAR(100) | 是 | 无 | 公司名称 |
| job_title | VARCHAR(100) | 是 | 无 | 岗位名称 |
| job_type | VARCHAR(50) | 否 | NULL | 岗位类型 |
| location | VARCHAR(100) | 否 | NULL | 工作地点 |
| source_platform | VARCHAR(50) | 否 | NULL | 来源平台 |
| job_url | VARCHAR(500) | 否 | NULL | 岗位链接 |
| jd_content | LONGTEXT | 是 | 无 | 岗位 JD 原文 |
| skill_requirements | TEXT | 否 | NULL | 技能要求 |
| salary_range | VARCHAR(50) | 否 | NULL | 薪资范围 |
| work_days_per_week | VARCHAR(30) | 否 | NULL | 每周工作天数 |
| internship_duration | VARCHAR(50) | 否 | NULL | 实习周期 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 是否逻辑删除 |

### 8.4 设计说明

1. `company_name`、`job_title`、`jd_content` 是核心字段；
2. `job_type` 可以保存 Java 后端、AI 应用、前端、算法等类型；
3. `source_platform` 可以保存 Boss 直聘、牛客、官网、实习僧等；
4. `jd_content` 存储完整岗位描述；
5. `skill_requirements` 可存储用户手动提取或 AI 提取的技能要求；
6. 删除岗位建议使用逻辑删除，避免影响历史报告和投递记录。

---

## 9. AI 分析报告表设计

### 9.1 表名

```text
analysis_report
```

### 9.2 表说明

AI 分析报告表用于保存简历与岗位 JD 的匹配分析结果，包括匹配分数、匹配等级、优势、短板、缺失技能、优化建议和原始 AI 返回内容。

### 9.3 字段设计

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 报告 ID，主键 |
| user_id | BIGINT | 是 | 无 | 用户 ID |
| resume_id | BIGINT | 是 | 无 | 简历 ID |
| job_id | BIGINT | 是 | 无 | 岗位 ID |
| match_score | INT | 否 | NULL | 匹配分数，0-100 |
| match_level | VARCHAR(30) | 否 | NULL | 匹配等级 |
| strengths | TEXT | 否 | NULL | 简历优势 |
| weaknesses | TEXT | 否 | NULL | 简历短板 |
| missing_skills | TEXT | 否 | NULL | 缺失技能 |
| suggestions | TEXT | 否 | NULL | 简历优化建议 |
| interview_tips | TEXT | 否 | NULL | 面试准备建议 |
| raw_ai_response | LONGTEXT | 否 | NULL | AI 原始返回 |
| ai_provider | VARCHAR(50) | 否 | NULL | AI 服务商 |
| ai_model | VARCHAR(100) | 否 | NULL | AI 模型 |
| cache_hit | TINYINT | 是 | 0 | 是否命中缓存 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 是否逻辑删除 |

### 9.4 match_level 枚举

| 值 | 分数范围 | 说明 |
|---|---|---|
| HIGH | 85-100 | 高匹配 |
| MEDIUM_HIGH | 70-84 | 较高匹配 |
| MEDIUM | 60-69 | 中等匹配 |
| LOW | 40-59 | 低匹配 |
| VERY_LOW | 0-39 | 很低匹配 |

### 9.5 设计说明

1. `user_id` 用于数据权限控制；
2. `resume_id` 关联简历；
3. `job_id` 关联岗位；
4. `strengths`、`weaknesses`、`missing_skills`、`suggestions`、`interview_tips` 可以第一阶段用 JSON 字符串保存；
5. `raw_ai_response` 保存模型原始返回，方便排查问题；
6. `ai_provider` 和 `ai_model` 为后续多模型切换做准备；
7. `cache_hit` 用于记录该报告是否来自缓存；
8. 第一阶段可以每次分析都生成一条报告，也可以缓存命中直接返回历史报告。

---

## 10. 投递记录表设计

### 10.1 表名

```text
application_record
```

### 10.2 表说明

投递记录表用于保存用户对某个岗位的投递状态、投递时间、面试时间、备注和复盘信息。

### 10.3 字段设计

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 投递记录 ID，主键 |
| user_id | BIGINT | 是 | 无 | 用户 ID |
| job_id | BIGINT | 是 | 无 | 岗位 ID |
| resume_id | BIGINT | 否 | NULL | 使用的简历 ID |
| report_id | BIGINT | 否 | NULL | 关联的分析报告 ID |
| status | VARCHAR(50) | 是 | TO_APPLY | 投递状态 |
| apply_date | DATE | 否 | NULL | 投递日期 |
| interview_date | DATETIME | 否 | NULL | 面试时间 |
| note | TEXT | 否 | NULL | 备注 |
| review | TEXT | 否 | NULL | 面试复盘 |
| priority | VARCHAR(30) | 否 | MEDIUM | 投递优先级 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 是否逻辑删除 |

### 10.4 status 枚举

| 值 | 说明 |
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

### 10.5 priority 枚举

| 值 | 说明 |
|---|---|
| HIGH | 高优先级 |
| MEDIUM | 中优先级 |
| LOW | 低优先级 |

### 10.6 设计说明

1. `job_id` 必填，因为投递记录必须对应一个岗位；
2. `resume_id` 可选，表示投递使用的简历；
3. `report_id` 可选，表示该投递是否来源于某次 AI 分析；
4. `status` 使用枚举控制；
5. `note` 用于普通备注；
6. `review` 用于笔试、面试后的复盘；
7. `priority` 用于标记重点岗位；
8. 删除投递记录建议使用逻辑删除。

---

## 11. 第二阶段扩展表设计

以下表不是 MVP 必须，但建议在文档中保留设计，方便后续扩展。

### 11.1 AI 调用日志表

表名：

```text
ai_call_log
```

表说明：

记录每次 AI 调用的基本信息，用于统计调用次数、调用耗时、失败原因和成本控制。

字段设计：

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 日志 ID |
| user_id | BIGINT | 是 | 无 | 用户 ID |
| provider | VARCHAR(50) | 否 | NULL | AI 服务商 |
| model | VARCHAR(100) | 否 | NULL | 模型名称 |
| request_type | VARCHAR(50) | 是 | ANALYSIS | 请求类型 |
| prompt_tokens | INT | 否 | NULL | 输入 token 数 |
| completion_tokens | INT | 否 | NULL | 输出 token 数 |
| total_tokens | INT | 否 | NULL | 总 token 数 |
| success | TINYINT | 是 | 1 | 是否成功 |
| error_message | TEXT | 否 | NULL | 错误信息 |
| duration_ms | BIGINT | 否 | NULL | 调用耗时 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

### 11.2 Prompt 模板表

表名：

```text
prompt_template
```

表说明：

用于保存系统 Prompt 模板，后续支持管理员维护 Prompt。

字段设计：

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 模板 ID |
| template_name | VARCHAR(100) | 是 | 无 | 模板名称 |
| template_type | VARCHAR(50) | 是 | 无 | 模板类型 |
| content | LONGTEXT | 是 | 无 | Prompt 内容 |
| version | VARCHAR(30) | 否 | v1 | 版本号 |
| enabled | TINYINT | 是 | 1 | 是否启用 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 是否逻辑删除 |

### 11.3 投递状态变化日志表

表名：

```text
application_status_log
```

表说明：

记录投递记录的状态变化历史，后续用于时间线展示。

字段设计：

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | 是 | 自增 | 日志 ID |
| application_id | BIGINT | 是 | 无 | 投递记录 ID |
| user_id | BIGINT | 是 | 无 | 用户 ID |
| old_status | VARCHAR(50) | 否 | NULL | 原状态 |
| new_status | VARCHAR(50) | 是 | 无 | 新状态 |
| remark | VARCHAR(500) | 否 | NULL | 备注 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

---

## 12. 索引设计

### 12.1 用户表索引

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|---|---|---|---|---|
| user | uk_user_username | username | UNIQUE | 保证用户名唯一 |
| user | idx_user_email | email | NORMAL | 邮箱查询 |
| user | idx_user_role | role | NORMAL | 角色筛选 |

### 12.2 简历表索引

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|---|---|---|---|---|
| resume | idx_resume_user_id | user_id | NORMAL | 查询用户简历 |
| resume | idx_resume_user_default | user_id, is_default | NORMAL | 查询默认简历 |
| resume | idx_resume_created_at | created_at | NORMAL | 按时间排序 |

### 12.3 岗位表索引

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|---|---|---|---|---|
| job_description | idx_job_user_id | user_id | NORMAL | 查询用户岗位 |
| job_description | idx_job_company | company_name | NORMAL | 公司搜索 |
| job_description | idx_job_title | job_title | NORMAL | 岗位搜索 |
| job_description | idx_job_type | job_type | NORMAL | 岗位类型筛选 |
| job_description | idx_job_created_at | created_at | NORMAL | 按时间排序 |

### 12.4 分析报告表索引

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|---|---|---|---|---|
| analysis_report | idx_report_user_id | user_id | NORMAL | 查询用户报告 |
| analysis_report | idx_report_resume_id | resume_id | NORMAL | 按简历查询报告 |
| analysis_report | idx_report_job_id | job_id | NORMAL | 按岗位查询报告 |
| analysis_report | idx_report_user_resume_job | user_id, resume_id, job_id | NORMAL | 查询相同简历岗位分析 |
| analysis_report | idx_report_score | match_score | NORMAL | 按分数筛选 |

### 12.5 投递记录表索引

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|---|---|---|---|---|
| application_record | idx_app_user_id | user_id | NORMAL | 查询用户投递 |
| application_record | idx_app_job_id | job_id | NORMAL | 查询岗位投递 |
| application_record | idx_app_resume_id | resume_id | NORMAL | 查询简历投递 |
| application_record | idx_app_status | status | NORMAL | 状态筛选 |
| application_record | idx_app_apply_date | apply_date | NORMAL | 投递日期筛选 |
| application_record | idx_app_user_job | user_id, job_id | NORMAL | 防止同一用户重复创建岗位投递 |

---

## 13. 逻辑删除设计

### 13.1 逻辑删除字段

核心业务表统一使用：

```sql
deleted TINYINT DEFAULT 0
```

含义：

| 值 | 说明 |
|---|---|
| 0 | 未删除 |
| 1 | 已删除 |

### 13.2 使用逻辑删除的表

建议以下表使用逻辑删除：

1. `user`；
2. `resume`；
3. `job_description`；
4. `analysis_report`；
5. `application_record`；
6. `prompt_template`。

### 13.3 逻辑删除查询规则

业务查询默认加条件：

```sql
deleted = 0
```

MyBatis Plus 后续可以使用逻辑删除注解：

```java
@TableLogic
private Integer deleted;
```

---

## 14. 时间字段设计

### 14.1 通用时间字段

核心业务表统一包含：

```text
created_at
updated_at
```

含义：

| 字段 | 说明 |
|---|---|
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 14.2 MySQL 自动更新时间

字段定义：

```sql
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

---

## 15. 外键设计说明

### 15.1 是否使用数据库外键

本项目第一阶段建议：不强制使用数据库物理外键。

原因：

1. 开发更灵活；
2. 方便逻辑删除；
3. 业务层可以控制权限和关联关系；
4. 避免删除和迁移时被外键约束卡住；
5. Java 后端项目中常见做法是通过业务逻辑维护关联。

### 15.2 业务层关联校验

虽然不使用物理外键，但业务层必须校验：

1. `resume_id` 是否存在；
2. `job_id` 是否存在；
3. `report_id` 是否存在；
4. 数据是否属于当前用户；
5. 删除岗位或简历时，是否已有相关报告或投递记录。

---

## 16. 完整建表 SQL

### 16.1 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE intern_pilot;
```

### 16.2 用户表

```sql
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    school VARCHAR(100) DEFAULT NULL COMMENT '学校',
    major VARCHAR(100) DEFAULT NULL COMMENT '专业',
    grade VARCHAR(30) DEFAULT NULL COMMENT '年级',
    role VARCHAR(30) NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_email (email),
    KEY idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### 16.3 简历表

```sql
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '简历ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_name VARCHAR(100) DEFAULT NULL COMMENT '简历名称',
    original_file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_file_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型：PDF/DOCX',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    parsed_text LONGTEXT DEFAULT NULL COMMENT '解析后的文本',
    parse_status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS' COMMENT '解析状态：SUCCESS/FAILED/PENDING',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认简历：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_resume_user_id (user_id),
    KEY idx_resume_user_default (user_id, is_default),
    KEY idx_resume_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';
```

### 16.4 岗位 JD 表

```sql
CREATE TABLE IF NOT EXISTS job_description (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '岗位ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    company_name VARCHAR(100) NOT NULL COMMENT '公司名称',
    job_title VARCHAR(100) NOT NULL COMMENT '岗位名称',
    job_type VARCHAR(50) DEFAULT NULL COMMENT '岗位类型',
    location VARCHAR(100) DEFAULT NULL COMMENT '工作地点',
    source_platform VARCHAR(50) DEFAULT NULL COMMENT '来源平台',
    job_url VARCHAR(500) DEFAULT NULL COMMENT '岗位链接',
    jd_content LONGTEXT NOT NULL COMMENT '岗位JD原文',
    skill_requirements TEXT DEFAULT NULL COMMENT '技能要求',
    salary_range VARCHAR(50) DEFAULT NULL COMMENT '薪资范围',
    work_days_per_week VARCHAR(30) DEFAULT NULL COMMENT '每周工作天数',
    internship_duration VARCHAR(50) DEFAULT NULL COMMENT '实习周期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_job_user_id (user_id),
    KEY idx_job_company (company_name),
    KEY idx_job_title (job_title),
    KEY idx_job_type (job_type),
    KEY idx_job_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位JD表';
```

### 16.5 AI 分析报告表

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

### 16.6 投递记录表

```sql
CREATE TABLE IF NOT EXISTS application_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '投递记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    job_id BIGINT NOT NULL COMMENT '岗位ID',
    resume_id BIGINT DEFAULT NULL COMMENT '使用的简历ID',
    report_id BIGINT DEFAULT NULL COMMENT '关联的分析报告ID',
    status VARCHAR(50) NOT NULL DEFAULT 'TO_APPLY' COMMENT '投递状态',
    apply_date DATE DEFAULT NULL COMMENT '投递日期',
    interview_date DATETIME DEFAULT NULL COMMENT '面试时间',
    note TEXT DEFAULT NULL COMMENT '备注',
    review TEXT DEFAULT NULL COMMENT '面试或笔试复盘',
    priority VARCHAR(30) DEFAULT 'MEDIUM' COMMENT '优先级：HIGH/MEDIUM/LOW',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_app_user_id (user_id),
    KEY idx_app_job_id (job_id),
    KEY idx_app_resume_id (resume_id),
    KEY idx_app_status (status),
    KEY idx_app_apply_date (apply_date),
    KEY idx_app_user_job (user_id, job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递记录表';
```

### 16.7 AI 调用日志表，第二阶段扩展

```sql
CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    provider VARCHAR(50) DEFAULT NULL COMMENT 'AI服务商',
    model VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
    request_type VARCHAR(50) NOT NULL DEFAULT 'ANALYSIS' COMMENT '请求类型',
    prompt_tokens INT DEFAULT NULL COMMENT '输入token数',
    completion_tokens INT DEFAULT NULL COMMENT '输出token数',
    total_tokens INT DEFAULT NULL COMMENT '总token数',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：0失败，1成功',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    duration_ms BIGINT DEFAULT NULL COMMENT '调用耗时，毫秒',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ai_log_user_id (user_id),
    KEY idx_ai_log_provider (provider),
    KEY idx_ai_log_success (success),
    KEY idx_ai_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用日志表';
```

### 16.8 Prompt 模板表，第二阶段扩展

```sql
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(50) NOT NULL COMMENT '模板类型',
    content LONGTEXT NOT NULL COMMENT 'Prompt内容',
    version VARCHAR(30) DEFAULT 'v1' COMMENT '版本号',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_prompt_type (template_type),
    KEY idx_prompt_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板表';
```

### 16.9 投递状态变化日志表，第二阶段扩展

```sql
CREATE TABLE IF NOT EXISTS application_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    application_id BIGINT NOT NULL COMMENT '投递记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    old_status VARCHAR(50) DEFAULT NULL COMMENT '原状态',
    new_status VARCHAR(50) NOT NULL COMMENT '新状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_status_log_application_id (application_id),
    KEY idx_status_log_user_id (user_id),
    KEY idx_status_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递状态变化日志表';
```

---

## 17. 初始化数据

### 17.1 初始化管理员用户说明

由于密码需要 BCrypt 加密，不建议直接手写明文密码。

可以在系统启动后通过接口注册管理员，或者使用后端生成 BCrypt 密码后插入。

示例：

```sql
INSERT INTO user (
    username,
    password,
    email,
    real_name,
    school,
    major,
    grade,
    role,
    enabled
) VALUES (
    'admin',
    '$2a$10$replace_with_bcrypt_password',
    'admin@example.com',
    '系统管理员',
    'InternPilot',
    'Admin',
    'Admin',
    'ADMIN',
    1
);
```

### 17.2 初始化 Prompt 模板，第二阶段

```sql
INSERT INTO prompt_template (
    template_name,
    template_type,
    content,
    version,
    enabled
) VALUES (
    '简历岗位匹配分析模板',
    'RESUME_JOB_MATCH',
    '你是一个资深技术招聘官和简历优化专家，请根据用户简历和岗位JD生成匹配分析报告。',
    'v1',
    1
);
```

---

## 18. 数据权限设计

### 18.1 核心原则

系统所有用户业务数据都必须以 `user_id` 作为数据隔离字段。

包括：

1. `resume`；
2. `job_description`；
3. `analysis_report`；
4. `application_record`；
5. `ai_call_log`。

### 18.2 查询规则

普通用户查询数据时，必须携带：

```sql
WHERE user_id = 当前登录用户ID
AND deleted = 0
```

示例：

```sql
SELECT *
FROM resume
WHERE id = #{resumeId}
  AND user_id = #{currentUserId}
  AND deleted = 0;
```

### 18.3 修改规则

普通用户修改数据时，必须携带：

```sql
WHERE id = #{id}
AND user_id = #{currentUserId}
```

示例：

```sql
UPDATE job_description
SET job_title = #{jobTitle},
    jd_content = #{jdContent}
WHERE id = #{jobId}
  AND user_id = #{currentUserId}
  AND deleted = 0;
```

### 18.4 删除规则

普通用户删除数据时，建议逻辑删除：

```sql
UPDATE resume
SET deleted = 1
WHERE id = #{resumeId}
  AND user_id = #{currentUserId};
```

---

## 19. 数据库设计注意事项

### 19.1 关于 JSON 字段

第一阶段建议将 AI 返回的数组内容存储为 JSON 字符串，例如：

```json
["熟悉 Spring Boot", "有 JWT 权限认证经验"]
```

保存字段：

1. `strengths`；
2. `weaknesses`；
3. `missing_skills`；
4. `suggestions`；
5. `interview_tips`。

这样做的好处：

1. 开发简单；
2. 方便直接返回前端；
3. 不需要额外建立多张子表；
4. 适合 MVP 阶段。

后续如果需要统计技能缺口，可以拆分成结构化表。

### 19.2 关于逻辑删除

第一阶段建议所有核心表使用逻辑删除，不直接物理删除。

原因：

1. 方便恢复；
2. 避免影响历史报告；
3. 避免投递记录和分析报告关联丢失；
4. 面试中可以体现数据完整性考虑。

### 19.3 关于物理外键

第一阶段不建议强制添加物理外键。

原因：

1. 降低开发和调试复杂度；
2. 避免逻辑删除冲突；
3. 方便后续重构；
4. 通过业务层保证数据一致性。

### 19.4 关于用户角色

第一阶段可以在 `user` 表中用 `role` 字段表示角色。

如果后续需要完整 RBAC，可以扩展为：

```text
role
permission
user_role
role_permission
```

但当前项目重点不是权限系统，而是 AI 实习投递平台，所以第一阶段不要过度设计。

---

## 20. 数据库设计结论

InternPilot 第一阶段数据库设计围绕以下五张核心表展开：

1. `user`：用户账号和个人信息；
2. `resume`：简历文件和解析文本；
3. `job_description`：岗位 JD 信息；
4. `analysis_report`：AI 匹配分析结果；
5. `application_record`：实习投递记录。

这五张表能够支撑系统的核心业务闭环：

```text
用户注册登录
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

数据库设计保留了 `user_id`、逻辑删除字段、时间字段、AI 服务字段和缓存标记字段，能够满足第一阶段 MVP 开发，同时支持后续扩展 AI 调用日志、Prompt 模板、投递状态日志、数据看板和多模型配置等功能。

第一阶段建议优先落地五张核心表，第二阶段再逐步补充扩展表。
