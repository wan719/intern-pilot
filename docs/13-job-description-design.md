# InternPilot 岗位 JD 管理模块设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的岗位 JD 管理模块设计，包括模块目标、业务流程、数据库设计、接口设计、DTO/VO 设计、Entity/Mapper 设计、Service 设计、Controller 设计、权限控制、搜索筛选、异常处理、测试流程和后续扩展方案。

岗位 JD 管理模块是 InternPilot 的核心输入模块之一。用户需要保存目标实习岗位的岗位描述，系统后续会结合用户简历文本与岗位 JD 内容，调用 AI 生成匹配分析报告。

---

## 2. 模块目标

岗位 JD 管理模块需要完成以下目标：

1. 支持用户创建岗位 JD；
2. 支持保存公司名称、岗位名称、岗位类型、工作地点等信息；
3. 支持保存完整岗位描述；
4. 支持保存技能要求；
5. 支持岗位列表分页查询；
6. 支持岗位关键词搜索；
7. 支持岗位类型筛选；
8. 支持岗位详情查询；
9. 支持岗位信息修改；
10. 支持岗位逻辑删除；
11. 保证用户只能访问自己的岗位；
12. 为后续 AI 分析模块提供岗位输入；
13. 为后续投递记录模块提供岗位关联。

---

## 3. 模块业务定位

在 InternPilot 中，岗位 JD 模块承担的是“目标岗位信息输入”的角色。

整体业务链路如下：

```text
用户保存岗位 JD
  ↓
系统保存岗位信息
  ↓
用户选择简历 + 岗位
  ↓
AI 分析模块读取 resume.parsed_text + job_description.jd_content
  ↓
生成匹配分析报告
  ↓
用户根据分析结果决定是否投递
  ↓
创建投递记录
```

因此，岗位 JD 模块不仅是岗位信息管理模块，也是 AI 分析模块和投递管理模块的前置依赖。

## 4. 功能范围

### 4.1 第一阶段必须实现

第一阶段需要实现：

1. 创建岗位 JD；
2. 查询岗位列表；
3. 查询岗位详情；
4. 修改岗位 JD；
5. 删除岗位 JD；
6. 按关键词搜索；
7. 按岗位类型筛选；
8. 按工作地点筛选；
9. 数据权限控制。

### 4.2 第二阶段可扩展

第二阶段可以扩展：

1. AI 自动提取岗位技能要求；
2. AI 判断岗位难度；
3. AI 给出投递建议；
4. 岗位收藏；
5. 岗位优先级；
6. 岗位标签；
7. 岗位去重；
8. 从 URL 自动解析岗位 JD；
9. 招聘平台岗位爬取；
10. 岗位推荐系统。

## 5. 数据库设计

### 5.1 `job_description` 表

岗位 JD 模块主要使用 `job_description` 表。

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

### 5.2 字段说明

| 字段名 | 说明 |
|---|---|
| `id` | 岗位 ID |
| `user_id` | 岗位所属用户 ID |
| `company_name` | 公司名称 |
| `job_title` | 岗位名称 |
| `job_type` | 岗位类型，例如 Java 后端、AI 应用、前端、算法 |
| `location` | 工作地点 |
| `source_platform` | 岗位来源平台，例如 Boss 直聘、牛客、官网 |
| `job_url` | 岗位链接 |
| `jd_content` | 完整岗位 JD 原文 |
| `skill_requirements` | 技能要求，可手动填写，也可后续由 AI 提取 |
| `salary_range` | 薪资范围 |
| `work_days_per_week` | 每周工作天数 |
| `internship_duration` | 实习周期 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |
| `deleted` | 逻辑删除标记 |

### 5.3 设计说明

1. `company_name`、`job_title`、`jd_content` 是核心必填字段；
2. `user_id` 用于数据权限控制；
3. `jd_content` 使用 `LONGTEXT`，用于保存完整岗位描述；
4. `skill_requirements` 第一阶段可以手动填写，后续由 AI 自动提取；
5. 删除岗位建议使用逻辑删除，避免影响历史 AI 分析报告和投递记录；
6. 第一阶段不单独设计岗位标签表，避免过度设计。

## 6. 岗位类型设计

### 6.1 第一阶段岗位类型

第一阶段可以用字符串保存岗位类型。

推荐常见类型：

- Java后端
- 前端开发
- 全栈开发
- AI应用开发
- 算法实习
- 自动驾驶感知
- 测试开发
- 运维开发
- 数据开发
- 其他

### 6.2 是否需要枚举

第一阶段建议前端下拉框提供固定选项，后端用 `String` 保存。

原因：

1. 岗位类型后续可能变化；
2. 不同平台岗位分类不统一；
3. 用字符串更灵活；
4. MVP 阶段不需要单独建字典表。

后续如果做管理员后台，可以设计岗位类型字典表。

## 7. 接口设计

### 7.1 创建岗位 JD

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/jobs` |
| Method | `POST` |
| 权限 | `USER` |
| Content-Type | `application/json` |

**请求参数**

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

**响应示例**

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
    "createdAt": "2026-05-06 20:20:00"
  }
}
```

**业务规则**

1. 用户必须登录；
2. 公司名称不能为空；
3. 岗位名称不能为空；
4. 岗位 JD 内容不能为空；
5. 岗位信息必须绑定当前用户；
6. 岗位链接如果填写，需要校验 URL 格式；
7. 第一阶段允许用户重复保存相似岗位；
8. 后续可以增加岗位去重逻辑。

### 7.2 查询岗位列表

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/jobs` |
| Method | `GET` |
| 权限 | `USER` |

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `keyword` | `String` | 否 | 无 | 公司名称或岗位名称关键词 |
| `jobType` | `String` | 否 | 无 | 岗位类型 |
| `location` | `String` | 否 | 无 | 工作地点 |
| `pageNum` | `Integer` | 否 | `1` | 页码 |
| `pageSize` | `Integer` | 否 | `10` | 每页数量 |

**请求示例**

```text
GET /api/jobs?keyword=Java&jobType=Java后端&pageNum=1&pageSize=10
```

**响应示例**

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
        "workDaysPerWeek": "5天/周",
        "internshipDuration": "3个月",
        "createdAt": "2026-05-06 20:20:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

**业务规则**

1. 只能查询当前用户创建的岗位；
2. 默认按创建时间倒序；
3. 支持关键词搜索；
4. 关键词搜索范围包括公司名称和岗位名称；
5. 支持岗位类型筛选；
6. 支持工作地点筛选；
7. 列表接口不返回完整 `jdContent`，避免响应过大。

### 7.3 查询岗位详情

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/jobs/{id}` |
| Method | `GET` |
| 权限 | `USER` |

**响应示例**

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
    "jdContent": "岗位职责：参与后端系统开发，要求熟悉 Java、Spring Boot、MySQL、Redis...",
    "skillRequirements": "Java, Spring Boot, MySQL, Redis",
    "salaryRange": "200-400元/天",
    "workDaysPerWeek": "5天/周",
    "internshipDuration": "3个月",
    "createdAt": "2026-05-06 20:20:00",
    "updatedAt": "2026-05-06 20:20:00"
  }
}
```

**业务规则**

1. 岗位必须存在；
2. 岗位必须属于当前用户；
3. 返回完整 `jdContent`；
4. 如果岗位不存在，返回错误；
5. 如果访问他人岗位，返回无权限或岗位不存在。

### 7.4 修改岗位 JD

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/jobs/{id}` |
| Method | `PUT` |
| 权限 | `USER` |
| Content-Type | `application/json` |

**请求参数**

```json
{
  "companyName": "腾讯",
  "jobTitle": "Java后端开发实习生",
  "jobType": "Java后端",
  "location": "深圳",
  "sourcePlatform": "Boss直聘",
  "jobUrl": "https://example.com/job/123",
  "jdContent": "修改后的岗位 JD 内容，要求熟悉 Java、Spring Boot、MySQL、Redis、Docker...",
  "skillRequirements": "Java, Spring Boot, MySQL, Redis, Docker",
  "salaryRange": "200-400元/天",
  "workDaysPerWeek": "5天/周",
  "internshipDuration": "3个月"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

**业务规则**

1. 用户只能修改自己的岗位；
2. 公司名称不能为空；
3. 岗位名称不能为空；
4. 岗位 JD 内容不能为空；
5. 修改岗位 JD 后，不自动修改历史 AI 分析报告；
6. 后续可以提示用户重新分析。

### 7.5 删除岗位 JD

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/jobs/{id}` |
| Method | `DELETE` |
| 权限 | `USER` |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

**业务规则**

1. 用户只能删除自己的岗位；
2. 第一阶段采用逻辑删除；
3. 删除后岗位列表不再显示；
4. 历史 AI 分析报告可以保留；
5. 历史投递记录可以保留；
6. 后续可以在删除时提示用户“该岗位已有投递记录”。

## 8. DTO 与 VO 设计

### 8.1 `JobCreateRequest`

路径：

```text
src/main/java/com/internpilot/dto/job/JobCreateRequest.java
```

```java
package com.internpilot.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
@Schema(description = "创建岗位请求")
public class JobCreateRequest {

    @Schema(description = "公司名称", example = "腾讯")
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @Schema(description = "岗位名称", example = "Java后端开发实习生")
    @NotBlank(message = "岗位名称不能为空")
    private String jobTitle;

    @Schema(description = "岗位类型", example = "Java后端")
    private String jobType;

    @Schema(description = "工作地点", example = "深圳")
    private String location;

    @Schema(description = "来源平台", example = "Boss直聘")
    private String sourcePlatform;

    @Schema(description = "岗位链接", example = "https://example.com/job/123")
    @URL(message = "岗位链接格式不正确")
    private String jobUrl;

    @Schema(description = "岗位 JD 原文")
    @NotBlank(message = "岗位 JD 不能为空")
    private String jdContent;

    @Schema(description = "技能要求", example = "Java, Spring Boot, MySQL, Redis")
    private String skillRequirements;

    @Schema(description = "薪资范围", example = "200-400元/天")
    private String salaryRange;

    @Schema(description = "每周工作天数", example = "5天/周")
    private String workDaysPerWeek;

    @Schema(description = "实习周期", example = "3个月")
    private String internshipDuration;
}
```

### 8.2 `JobUpdateRequest`

路径：

```text
src/main/java/com/internpilot/dto/job/JobUpdateRequest.java
```

```java
package com.internpilot.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "修改岗位请求")
public class JobUpdateRequest {

    @Schema(description = "公司名称", example = "腾讯")
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @Schema(description = "岗位名称", example = "Java后端开发实习生")
    @NotBlank(message = "岗位名称不能为空")
    private String jobTitle;

    @Schema(description = "岗位类型", example = "Java后端")
    private String jobType;

    @Schema(description = "工作地点", example = "深圳")
    private String location;

    @Schema(description = "来源平台", example = "Boss直聘")
    private String sourcePlatform;

    @Schema(description = "岗位链接", example = "https://example.com/job/123")
    @URL(message = "岗位链接格式不正确")
    private String jobUrl;

    @Schema(description = "岗位 JD 原文")
    @NotBlank(message = "岗位 JD 不能为空")
    private String jdContent;

    @Schema(description = "技能要求", example = "Java, Spring Boot, MySQL, Redis")
    private String skillRequirements;

    @Schema(description = "薪资范围", example = "200-400元/天")
    private String salaryRange;

    @Schema(description = "每周工作天数", example = "5天/周")
    private String workDaysPerWeek;

    @Schema(description = "实习周期", example = "3个月")
    private String internshipDuration;
}
```

### 8.3 `JobCreateResponse`

路径：

```text
src/main/java/com/internpilot/vo/job/JobCreateResponse.java
```

```java
package com.internpilot.vo.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建岗位响应")
public class JobCreateResponse {

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "岗位类型")
    private String jobType;

    @Schema(description = "工作地点")
    private String location;

    @Schema(description = "来源平台")
    private String sourcePlatform;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 8.4 `JobListResponse`

路径：

```text
src/main/java/com/internpilot/vo/job/JobListResponse.java
```

```java
package com.internpilot.vo.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "岗位列表响应")
public class JobListResponse {

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "岗位类型")
    private String jobType;

    @Schema(description = "工作地点")
    private String location;

    @Schema(description = "来源平台")
    private String sourcePlatform;

    @Schema(description = "薪资范围")
    private String salaryRange;

    @Schema(description = "每周工作天数")
    private String workDaysPerWeek;

    @Schema(description = "实习周期")
    private String internshipDuration;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 8.5 `JobDetailResponse`

路径：

```text
src/main/java/com/internpilot/vo/job/JobDetailResponse.java
```

```java
package com.internpilot.vo.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "岗位详情响应")
public class JobDetailResponse {

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "岗位类型")
    private String jobType;

    @Schema(description = "工作地点")
    private String location;

    @Schema(description = "来源平台")
    private String sourcePlatform;

    @Schema(description = "岗位链接")
    private String jobUrl;

    @Schema(description = "岗位 JD 原文")
    private String jdContent;

    @Schema(description = "技能要求")
    private String skillRequirements;

    @Schema(description = "薪资范围")
    private String salaryRange;

    @Schema(description = "每周工作天数")
    private String workDaysPerWeek;

    @Schema(description = "实习周期")
    private String internshipDuration;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
```

## 9. Entity 与 Mapper 设计

### 9.1 `JobDescription` Entity

路径：

```text
src/main/java/com/internpilot/entity/JobDescription.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_description")
public class JobDescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String companyName;

    private String jobTitle;

    private String jobType;

    private String location;

    private String sourcePlatform;

    private String jobUrl;

    private String jdContent;

    private String skillRequirements;

    private String salaryRange;

    private String workDaysPerWeek;

    private String internshipDuration;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

### 9.2 `JobDescriptionMapper`

路径：

```text
src/main/java/com/internpilot/mapper/JobDescriptionMapper.java
```

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.JobDescription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobDescriptionMapper extends BaseMapper<JobDescription> {
}
```

## 10. Service 设计

### 10.1 `JobService`

路径：

```text
src/main/java/com/internpilot/service/JobService.java
```

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;

public interface JobService {

    JobCreateResponse create(JobCreateRequest request);

    PageResult<JobListResponse> list(
            String keyword,
            String jobType,
            String location,
            Integer pageNum,
            Integer pageSize
    );

    JobDetailResponse getDetail(Long id);

    Boolean update(Long id, JobUpdateRequest request);

    Boolean delete(Long id);
}
```

### 10.2 `JobServiceImpl`

路径：

```text
src/main/java/com/internpilot/service/impl/JobServiceImpl.java
```

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.entity.JobDescription;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.service.JobService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobDescriptionMapper jobDescriptionMapper;

    @Override
    @Transactional
    public JobCreateResponse create(JobCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        JobDescription job = new JobDescription();
        job.setUserId(currentUserId);
        job.setCompanyName(request.getCompanyName());
        job.setJobTitle(request.getJobTitle());
        job.setJobType(request.getJobType());
        job.setLocation(request.getLocation());
        job.setSourcePlatform(request.getSourcePlatform());
        job.setJobUrl(request.getJobUrl());
        job.setJdContent(request.getJdContent());
        job.setSkillRequirements(request.getSkillRequirements());
        job.setSalaryRange(request.getSalaryRange());
        job.setWorkDaysPerWeek(request.getWorkDaysPerWeek());
        job.setInternshipDuration(request.getInternshipDuration());

        jobDescriptionMapper.insert(job);

        return toCreateResponse(job);
    }

    @Override
    public PageResult<JobListResponse> list(
            String keyword,
            String jobType,
            String location,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<JobDescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobDescription::getUserId, currentUserId)
                .eq(JobDescription::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(JobDescription::getCompanyName, keyword)
                    .or()
                    .like(JobDescription::getJobTitle, keyword)
            );
        }

        if (StringUtils.hasText(jobType)) {
            wrapper.eq(JobDescription::getJobType, jobType);
        }

        if (StringUtils.hasText(location)) {
            wrapper.like(JobDescription::getLocation, location);
        }

        wrapper.orderByDesc(JobDescription::getCreatedAt);

        Page<JobDescription> page = new Page<>(pageNum, pageSize);
        Page<JobDescription> resultPage = jobDescriptionMapper.selectPage(page, wrapper);

        List<JobListResponse> records = resultPage.getRecords()
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
    public JobDetailResponse getDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobDescription job = getUserJobOrThrow(id, currentUserId);
        return toDetailResponse(job);
    }

    @Override
    @Transactional
    public Boolean update(Long id, JobUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobDescription job = getUserJobOrThrow(id, currentUserId);

        job.setCompanyName(request.getCompanyName());
        job.setJobTitle(request.getJobTitle());
        job.setJobType(request.getJobType());
        job.setLocation(request.getLocation());
        job.setSourcePlatform(request.getSourcePlatform());
        job.setJobUrl(request.getJobUrl());
        job.setJdContent(request.getJdContent());
        job.setSkillRequirements(request.getSkillRequirements());
        job.setSalaryRange(request.getSalaryRange());
        job.setWorkDaysPerWeek(request.getWorkDaysPerWeek());
        job.setInternshipDuration(request.getInternshipDuration());

        jobDescriptionMapper.updateById(job);

        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobDescription job = getUserJobOrThrow(id, currentUserId);

        job.setDeleted(1);
        jobDescriptionMapper.updateById(job);

        return true;
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

    private JobCreateResponse toCreateResponse(JobDescription job) {
        JobCreateResponse response = new JobCreateResponse();
        response.setJobId(job.getId());
        response.setCompanyName(job.getCompanyName());
        response.setJobTitle(job.getJobTitle());
        response.setJobType(job.getJobType());
        response.setLocation(job.getLocation());
        response.setSourcePlatform(job.getSourcePlatform());
        response.setCreatedAt(job.getCreatedAt());
        return response;
    }

    private JobListResponse toListResponse(JobDescription job) {
        JobListResponse response = new JobListResponse();
        response.setJobId(job.getId());
        response.setCompanyName(job.getCompanyName());
        response.setJobTitle(job.getJobTitle());
        response.setJobType(job.getJobType());
        response.setLocation(job.getLocation());
        response.setSourcePlatform(job.getSourcePlatform());
        response.setSalaryRange(job.getSalaryRange());
        response.setWorkDaysPerWeek(job.getWorkDaysPerWeek());
        response.setInternshipDuration(job.getInternshipDuration());
        response.setCreatedAt(job.getCreatedAt());
        return response;
    }

    private JobDetailResponse toDetailResponse(JobDescription job) {
        JobDetailResponse response = new JobDetailResponse();
        response.setJobId(job.getId());
        response.setCompanyName(job.getCompanyName());
        response.setJobTitle(job.getJobTitle());
        response.setJobType(job.getJobType());
        response.setLocation(job.getLocation());
        response.setSourcePlatform(job.getSourcePlatform());
        response.setJobUrl(job.getJobUrl());
        response.setJdContent(job.getJdContent());
        response.setSkillRequirements(job.getSkillRequirements());
        response.setSalaryRange(job.getSalaryRange());
        response.setWorkDaysPerWeek(job.getWorkDaysPerWeek());
        response.setInternshipDuration(job.getInternshipDuration());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }
}
```

## 11. Controller 设计

### 11.1 `JobController`

路径：

```text
src/main/java/com/internpilot/controller/JobController.java
```

```java
package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.service.JobService;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "岗位 JD 管理接口")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "创建岗位 JD", description = "创建目标实习岗位 JD，用于后续 AI 匹配分析")
    @PostMapping
    public Result<JobCreateResponse> create(@RequestBody @Valid JobCreateRequest request) {
        return Result.success(jobService.create(request));
    }

    @Operation(summary = "查询岗位列表", description = "分页查询当前用户保存的岗位 JD")
    @GetMapping
    public Result<PageResult<JobListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(jobService.list(keyword, jobType, location, pageNum, pageSize));
    }

    @Operation(summary = "查询岗位详情", description = "查询当前用户某个岗位 JD 的完整详情")
    @GetMapping("/{id}")
    public Result<JobDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(jobService.getDetail(id));
    }

    @Operation(summary = "修改岗位 JD", description = "修改当前用户保存的岗位 JD")
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @PathVariable Long id,
            @RequestBody @Valid JobUpdateRequest request
    ) {
        return Result.success(jobService.update(id, request));
    }

    @Operation(summary = "删除岗位 JD", description = "逻辑删除当前用户保存的岗位 JD")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(jobService.delete(id));
    }
}
```

## 12. 搜索与筛选设计

### 12.1 关键词搜索

关键词搜索范围：

- `company_name`
- `job_title`

实现方式：

```java
wrapper.and(w -> w
    .like(JobDescription::getCompanyName, keyword)
    .or()
    .like(JobDescription::getJobTitle, keyword)
);
```

### 12.2 岗位类型筛选

字段：

`job_type`

示例：

- Java后端
- AI应用开发
- 前端开发
- 自动驾驶感知

查询方式：

```java
wrapper.eq(JobDescription::getJobType, jobType);
```

### 12.3 工作地点筛选

字段：

`location`

建议使用模糊匹配：

```java
wrapper.like(JobDescription::getLocation, location);
```

原因：

用户可能输入：

- 重庆
- 重庆市
- 深圳
- 远程

模糊匹配更友好。

### 12.4 排序规则

默认排序：

```text
created_at DESC
```

也就是新保存的岗位显示在前面。

## 13. 数据权限设计

### 13.1 核心原则

岗位 JD 是用户私有数据，所有查询、修改、删除都必须校验：

```text
job_description.user_id == 当前登录用户ID
```

### 13.2 错误示例

错误写法：

```java
JobDescription job = jobDescriptionMapper.selectById(jobId);
```

问题：只根据 `id` 查询，可能查到别人的岗位。

### 13.3 正确示例

正确写法：

```java
JobDescription job = jobDescriptionMapper.selectOne(
    new LambdaQueryWrapper<JobDescription>()
        .eq(JobDescription::getId, jobId)
        .eq(JobDescription::getUserId, currentUserId)
        .eq(JobDescription::getDeleted, 0)
        .last("LIMIT 1")
);
```

### 13.4 返回 403 还是 404

如果用户访问别人的岗位，有两种处理方式：

方案一：返回 `403`

```text
无权限访问该岗位
```

优点：语义明确。

方案二：返回 `404`

```text
岗位不存在
```

优点：不暴露资源是否存在。

第一阶段推荐统一返回：

```text
岗位不存在或无权限访问
```

这样简单、安全、易实现。

## 14. 与 AI 分析模块的关系

AI 分析模块会读取：

- `job_description.jd_content`
- `job_description.skill_requirements`

主要用途：

1. 作为 AI Prompt 的岗位输入；
2. 提取岗位技能要求；
3. 与简历 `parsedText` 做匹配分析；
4. 生成匹配分数；
5. 生成技能缺口；
6. 生成简历优化建议。

AI 分析模块中必须校验：

```text
job.user_id == currentUserId
```

不能使用其他用户保存的岗位。

## 15. 与投递记录模块的关系

投递记录表中会关联：

`application_record.job_id`

用途：

1. 记录用户投递的是哪个岗位；
2. 展示公司名称和岗位名称；
3. 支持按岗位查询投递状态；
4. 支持后续投递数据统计。

删除岗位时要注意：

1. 第一阶段使用逻辑删除；
2. 历史投递记录可以保留；
3. 投递记录详情仍然可以通过 `job_id` 找到岗位信息；
4. 后续可以做“已删除岗位”的展示标识。

## 16. 异常处理设计

### 16.1 常见异常

| 场景 | 错误信息 |
|---|---|
| 公司名称为空 | 公司名称不能为空 |
| 岗位名称为空 | 岗位名称不能为空 |
| 岗位 JD 为空 | 岗位 JD 不能为空 |
| 岗位链接格式错误 | 岗位链接格式不正确 |
| 岗位不存在 | 岗位不存在或无权限访问 |
| 未登录访问 | 请先登录 |
| 修改他人岗位 | 岗位不存在或无权限访问 |
| 删除他人岗位 | 岗位不存在或无权限访问 |

### 16.2 使用异常类型

第一阶段主要使用：

`BusinessException`

示例：

```java
throw new BusinessException("岗位不存在或无权限访问");
```

## 17. 参数校验设计

### 17.1 必填字段

创建和修改岗位时，以下字段必填：

- `companyName`
- `jobTitle`
- `jdContent`

### 17.2 URL 校验

如果 `jobUrl` 不为空，需要符合 URL 格式。

```java
@URL(message = "岗位链接格式不正确")
private String jobUrl;
```

注意：如果用户输入的是普通文本，可能会校验失败。第一阶段可以让前端提示用户填写完整链接，例如：

```text
https://example.com/job/123
```

如果你觉得 URL 校验太严格，也可以暂时去掉 `@URL`，只做字符串保存。

### 17.3 长度限制建议

可以后续加入长度限制：

```java
@Size(max = 100, message = "公司名称不能超过100个字符")
private String companyName;

@Size(max = 100, message = "岗位名称不能超过100个字符")
private String jobTitle;

@Size(max = 500, message = "岗位链接不能超过500个字符")
private String jobUrl;
```

第一阶段可以先不加太多限制，避免开发时被格式卡住。

## 18. 事务设计

### 18.1 创建岗位

创建岗位是单表插入，事务不是必须，但可以加：

`@Transactional`

### 18.2 修改岗位

修改岗位是单表更新，事务不是必须，但可以加：

`@Transactional`

### 18.3 删除岗位

删除岗位是逻辑删除，事务不是必须，但可以加：

`@Transactional`

## 19. 测试流程

### 19.1 测试前置条件

1. 项目启动成功；
2. 用户认证模块完成；
3. 已注册用户；
4. 已登录并获得 JWT Token；
5. `job_description` 表已创建。

### 19.2 测试顺序

1. 登录获取 Token
2. 创建岗位 JD
3. 查询岗位列表
4. 查询岗位详情
5. 使用 `keyword` 搜索岗位
6. 使用 `jobType` 筛选岗位
7. 使用 `location` 筛选岗位
8. 修改岗位 JD
9. 再次查询岗位详情确认修改成功
10. 删除岗位
11. 再次查询列表确认岗位不显示
12. 不带 Token 创建岗位，验证 401
13. 创建岗位时 `companyName` 为空，验证 400
14. 创建岗位时 `jobTitle` 为空，验证 400
15. 创建岗位时 `jdContent` 为空，验证 400

## 20. PowerShell 测试示例

### 20.1 登录获取 Token

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

### 20.2 创建岗位 JD

```powershell
$body = @{
  companyName = "腾讯"
  jobTitle = "Java后端开发实习生"
  jobType = "Java后端"
  location = "深圳"
  sourcePlatform = "Boss直聘"
  jobUrl = "https://example.com/job/123"
  jdContent = "岗位职责：参与后端系统开发，要求熟悉 Java、Spring Boot、MySQL、Redis。"
  skillRequirements = "Java, Spring Boot, MySQL, Redis"
  salaryRange = "200-400元/天"
  workDaysPerWeek = "5天/周"
  internshipDuration = "3个月"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/jobs" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

### 20.3 查询岗位列表

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/jobs?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 20.4 关键词搜索

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/jobs?keyword=Java&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 20.5 查询岗位详情

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/jobs/1" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 20.6 修改岗位 JD

```powershell
$body = @{
  companyName = "腾讯"
  jobTitle = "Java后端开发实习生"
  jobType = "Java后端"
  location = "深圳"
  sourcePlatform = "Boss直聘"
  jobUrl = "https://example.com/job/123"
  jdContent = "修改后的岗位职责：参与后端系统开发，要求熟悉 Java、Spring Boot、MySQL、Redis、Docker。"
  skillRequirements = "Java, Spring Boot, MySQL, Redis, Docker"
  salaryRange = "200-400元/天"
  workDaysPerWeek = "5天/周"
  internshipDuration = "3个月"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/jobs/1" `
  -Method Put `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

### 20.7 删除岗位

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/jobs/1" `
  -Method Delete `
  -Headers @{ Authorization = "Bearer $token" }
```

## 21. `curl.exe` 测试示例

### 21.1 创建岗位 JD

```powershell
curl.exe -X POST "http://localhost:8080/api/jobs" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer YOUR_TOKEN" `
  -d "{`"companyName`":`"腾讯`",`"jobTitle`":`"Java后端开发实习生`",`"jobType`":`"Java后端`",`"location`":`"深圳`",`"jdContent`":`"要求熟悉 Java、Spring Boot、MySQL、Redis。`"}"
```

### 21.2 查询岗位列表

```powershell
curl.exe -X GET "http://localhost:8080/api/jobs?pageNum=1&pageSize=10" `
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 22. Swagger 测试流程

访问：

```text
http://localhost:8080/doc.html
```

测试步骤：

1. 调用登录接口；
2. 复制返回 Token；
3. 点击 Authorize；
4. 输入 Bearer Token；
5. 打开岗位 JD 管理接口；
6. 调用创建岗位接口；
7. 调用岗位列表接口；
8. 调用岗位详情接口；
9. 调用修改岗位接口；
10. 调用删除岗位接口。

## 23. 常见问题与解决方案

### 23.1 创建岗位返回 401

原因：

1. 没有携带 Token；
2. Token 过期；
3. Token 格式错误。

正确格式：

```text
Authorization: Bearer your_token
```

### 23.2 创建岗位返回“岗位链接格式不正确”

原因：`@URL` 要求链接格式完整。

错误示例：

```text
www.example.com/job
```

正确示例：

```text
https://www.example.com/job
```

如果开发阶段不想被 URL 格式影响，可以先去掉 `@URL` 注解。

### 23.3 查询岗位详情返回“岗位不存在或无权限访问”

可能原因：

1. 岗位 ID 不存在；
2. 岗位已被逻辑删除；
3. 当前用户不是该岗位创建者；
4. Token 对应用户不对。

排查 SQL：

```sql
SELECT *
FROM job_description
WHERE id = 1;
```

重点检查：

- `user_id`
- `deleted`

### 23.4 岗位删除后数据库还在

这是正常现象。

因为采用逻辑删除：

```text
deleted = 1
```

列表查询时加：

```text
deleted = 0
```

所以不会显示已删除岗位。

### 23.5 搜索不到岗位

检查：

1. 是否使用当前用户 Token；
2. 岗位是否被逻辑删除；
3. `keyword` 是否匹配 `companyName` 或 `jobTitle`；
4. `jobType` 是否完全一致；
5. `location` 是否模糊匹配。

## 24. 后续增强：AI 自动提取岗位技能

第一阶段 `skillRequirements` 由用户手动填写。

后续可以增加接口：

```text
POST /api/jobs/{id}/extract-skills
```

功能：

```text
读取岗位 JD
  ↓
调用 AI
  ↓
提取技能关键词
  ↓
保存到 skill_requirements
```

AI 输出示例：

```json
{
  "skills": [
    "Java",
    "Spring Boot",
    "MySQL",
    "Redis",
    "Docker",
    "Linux"
  ],
  "difficulty": "MEDIUM",
  "suitableFor": "有 Spring Boot 项目经验的本科生"
}
```

## 25. 后续增强：岗位难度评估

可以增加字段或报告：

`difficulty_level`

可选值：

- `EASY`
- `MEDIUM`
- `HARD`

AI 可以根据岗位 JD 判断：

1. 技术要求数量；
2. 是否要求实习经历；
3. 是否要求算法；
4. 是否要求高并发；
5. 是否要求云原生；
6. 是否要求英语；
7. 是否要求研究经历。

## 26. 后续增强：岗位去重

用户可能重复保存同一个岗位。

第一阶段可以不做去重。

后续可以通过以下字段判断：

`user_id + company_name + job_title + job_url`

如果 `job_url` 相同，可以提示：

```text
该岗位已保存，请勿重复添加
```

## 27. 后续增强：从 URL 自动解析岗位

后续可以让用户只输入岗位链接：

```text
https://www.xxx.com/job/123
```

系统自动抓取岗位信息。

流程：

```text
用户输入岗位 URL
  ↓
后端请求网页
  ↓
解析网页内容
  ↓
提取公司、岗位、JD
  ↓
保存岗位
```

注意：

1. 需要遵守网站 robots 和使用规则；
2. 不要做大规模爬虫；
3. 第一阶段不建议做；
4. 可以只支持用户手动粘贴 JD。

## 28. 后续增强：岗位标签

后续可以为岗位增加标签：

- 远程
- 重庆
- Java
- AI应用
- 高薪
- 可转正
- 急招
- 低门槛

可以设计新表：

- `job_tag`
- `job_tag_relation`

第一阶段不建议加，直接用 `jobType` 和 `location` 已经够用。

## 29. 面试讲解准备

### 29.1 面试官可能问：岗位 JD 模块有什么作用？

回答思路：

岗位 JD 模块是系统的目标岗位输入层。用户保存岗位信息后，系统会把岗位 JD 和用户简历解析文本一起传给 AI 分析模块，生成岗位匹配度、技能缺口和简历优化建议。后续投递记录模块也会关联岗位 ID，用于管理投递状态。

### 29.2 面试官可能问：怎么防止用户访问别人的岗位？

回答思路：

所有岗位查询、修改、删除都不会只根据 `jobId` 查询，而是同时加上 `userId` 条件。`userId` 来自 JWT 解析后的当前登录用户。例如查询岗位详情时，会使用 `id = jobId AND user_id = currentUserId AND deleted = 0`。这样即使用户手动修改 URL 中的岗位 ID，也访问不到别人的岗位。

### 29.3 面试官可能问：为什么删除岗位用逻辑删除？

回答思路：

因为岗位可能已经关联了 AI 分析报告和投递记录。如果直接物理删除，历史报告和投递记录会失去上下文。所以第一阶段使用 `deleted` 字段做逻辑删除，岗位列表不展示已删除数据，但历史数据可以保留，后续也方便做恢复或审计。

### 29.4 面试官可能问：岗位 JD 修改后，历史分析报告怎么处理？

回答思路：

岗位 JD 修改后，我不会自动覆盖历史 AI 分析报告，因为历史报告是基于当时的简历和岗位 JD 生成的结果，应该保留原始上下文。如果用户希望基于新 JD 重新分析，可以重新调用 AI 分析接口生成新报告。后续可以在前端提示“岗位 JD 已修改，建议重新分析”。

### 29.5 面试官可能问：为什么列表接口不返回完整 JD？

回答思路：

岗位 JD 内容可能很长，列表接口如果每条都返回完整 `jdContent`，会导致响应体过大，影响性能。所以列表接口只返回公司、岗位名称、类型、地点等摘要信息。用户点击详情时，再通过详情接口返回完整 `jdContent`。

## 30. 开发顺序建议

岗位 JD 模块建议按以下顺序开发：

1. 创建 `job_description` 表；
2. 创建 `JobDescription` Entity；
3. 创建 `JobDescriptionMapper`；
4. 创建 `JobCreateRequest`；
5. 创建 `JobUpdateRequest`；
6. 创建 `JobCreateResponse`；
7. 创建 `JobListResponse`；
8. 创建 `JobDetailResponse`；
9. 创建 `JobService`；
10. 实现 `JobServiceImpl`；
11. 创建 `JobController`；
12. 测试创建岗位；
13. 测试岗位列表；
14. 测试关键词搜索；
15. 测试岗位类型筛选；
16. 测试岗位详情；
17. 测试修改岗位；
18. 测试删除岗位；
19. 测试未登录访问返回 401；
20. 测试访问他人岗位被拦截。

## 31. 验收标准

### 31.1 创建岗位验收

- 登录用户可以创建岗位；
- 未登录用户不能创建岗位；
- 公司名称不能为空；
- 岗位名称不能为空；
- 岗位 JD 不能为空；
- 创建成功后数据库存在记录；
- 记录绑定当前用户 ID。

### 31.2 查询岗位验收

- 用户可以查看自己的岗位列表；
- 用户只能查看自己的岗位；
- 列表接口支持分页；
- 列表接口支持关键词搜索；
- 列表接口支持岗位类型筛选；
- 列表接口不返回完整 `jdContent`；
- 详情接口返回完整 `jdContent`。

### 31.3 修改岗位验收

- 用户可以修改自己的岗位；
- 用户不能修改他人的岗位；
- 修改后查询详情能看到新数据；
- 修改 JD 不影响历史 AI 分析报告。

### 31.4 删除岗位验收

- 用户可以删除自己的岗位；
- 用户不能删除他人的岗位；
- 删除后列表不显示；
- 数据库中记录为逻辑删除；
- 历史分析报告和投递记录可以保留。

### 31.5 数据权限验收

- 所有岗位查询都带 `userId` 条件；
- 所有岗位修改都带 `userId` 条件；
- 所有岗位删除都带 `userId` 条件；
- 用户修改 URL 中的 `jobId` 不能访问别人的岗位。

## 32. 模块设计结论

岗位 JD 管理模块是 InternPilot 的核心输入模块之一，负责保存用户目标岗位信息，并为 AI 分析模块和投递记录模块提供基础数据。

第一阶段采用：

```text
手动录入岗位 JD + MySQL 持久化 + 分页查询 + 关键词搜索 + 逻辑删除 + 用户数据隔离
```

核心流程为：

```text
用户创建岗位
  ↓
保存岗位 JD
  ↓
查询岗位列表
  ↓
选择岗位详情
  ↓
结合简历进行 AI 分析
  ↓
根据分析结果创建投递记录
```

该设计能够满足 MVP 阶段的核心需求，同时保留了后续扩展 AI 技能提取、岗位难度评估、岗位去重、岗位标签、岗位推荐和 URL 自动解析的空间。
