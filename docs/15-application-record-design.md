# InternPilot 投递记录模块设计与实现文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的投递记录模块设计与实现方案，包括模块目标、业务流程、数据库设计、接口设计、DTO/VO 设计、Entity/Mapper 设计、Service 设计、Controller 设计、数据权限控制、异常处理、测试流程和后续扩展方案。

投递记录模块是 InternPilot 后端 MVP 闭环的最后一个核心模块。它负责将 AI 分析结果转化为用户实际求职行动，帮助用户记录岗位投递状态、面试时间、备注和复盘内容。

---

## 2. 模块目标

投递记录模块需要完成以下目标：

1. 支持用户为某个岗位创建投递记录；
2. 支持关联岗位 JD；
3. 支持关联简历；
4. 支持关联 AI 分析报告；
5. 支持设置投递状态；
6. 支持设置投递优先级；
7. 支持记录投递日期；
8. 支持记录面试时间；
9. 支持记录备注；
10. 支持记录笔试 / 面试复盘；
11. 支持分页查询投递记录；
12. 支持按状态筛选；
13. 支持按关键词搜索公司或岗位；
14. 支持修改投递状态；
15. 支持修改备注和复盘；
16. 支持逻辑删除投递记录；
17. 保证用户只能访问自己的投递记录。

---

## 3. 模块业务定位

InternPilot 的核心业务链路是：

```text
上传简历
  ↓
创建岗位 JD
  ↓
AI 匹配分析
  ↓
生成分析报告
  ↓
创建投递记录
  ↓
持续跟踪投递进度
```

投递记录模块的作用是把前面的“分析”转化为“行动”。

用户通过 AI 分析报告判断岗位是否值得投递，然后创建投递记录，后续持续更新状态：

```text
待投递
  ↓
已投递
  ↓
笔试中
  ↓
一面
  ↓
二面
  ↓
HR 面
  ↓
Offer / 被拒 / 放弃
```

## 4. 功能范围

### 4.1 第一阶段必须实现

第一阶段需要实现：

1. 创建投递记录；
2. 查询投递记录列表；
3. 查询投递记录详情；
4. 修改投递状态；
5. 修改备注、复盘、面试时间；
6. 删除投递记录；
7. 按状态筛选；
8. 按公司名称或岗位名称关键词搜索；
9. 用户数据权限控制；
10. 状态枚举校验；
11. 优先级枚举校验。

### 4.2 第二阶段可扩展

第二阶段可以扩展：

1. 投递状态变更日志；
2. 投递时间线；
3. 面试提醒；
4. 邮件提醒；
5. 数据看板；
6. Offer 统计；
7. 面试复盘 AI 总结；
8. 根据投递状态自动生成待办事项；
9. 日历集成；
10. 投递成功率分析。

## 5. 数据库设计

### 5.1 application_record 表

投递记录模块使用 `application_record` 表。

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

### 5.2 字段说明

| 字段名 | 说明 |
| --- | --- |
| id | 投递记录 ID |
| user_id | 当前投递记录所属用户 |
| job_id | 关联岗位 ID |
| resume_id | 投递时使用的简历 ID，可为空 |
| report_id | 关联的 AI 分析报告 ID，可为空 |
| status | 当前投递状态 |
| apply_date | 投递日期 |
| interview_date | 面试时间 |
| note | 普通备注 |
| review | 笔试或面试复盘 |
| priority | 投递优先级 |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| deleted | 逻辑删除标记 |

### 5.3 设计说明

1. `job_id` 必填，因为投递记录必须对应一个岗位；
2. `resume_id` 可选，因为用户可能暂时只想记录岗位；
3. `report_id` 可选，因为用户可能没有经过 AI 分析就直接创建投递记录；
4. `status` 使用枚举控制；
5. `priority` 使用枚举控制；
6. 删除投递记录使用逻辑删除；
7. 所有查询都必须带 `user_id` 条件；
8. 第一阶段不使用物理外键，由业务层校验关联关系。

## 6. 状态枚举设计

### 6.1 投递状态枚举

| 状态值 | 中文含义 |
| --- | --- |
| TO_APPLY | 待投递 |
| APPLIED | 已投递 |
| WRITTEN_TEST | 笔试中 |
| FIRST_INTERVIEW | 一面 |
| SECOND_INTERVIEW | 二面 |
| HR_INTERVIEW | HR 面 |
| OFFER | 已 Offer |
| REJECTED | 被拒 |
| GIVEN_UP | 放弃 |

### 6.2 ApplicationStatusEnum

路径：

`src/main/java/com/internpilot/enums/ApplicationStatusEnum.java`

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatusEnum {

    TO_APPLY("TO_APPLY", "待投递"),
    APPLIED("APPLIED", "已投递"),
    WRITTEN_TEST("WRITTEN_TEST", "笔试中"),
    FIRST_INTERVIEW("FIRST_INTERVIEW", "一面"),
    SECOND_INTERVIEW("SECOND_INTERVIEW", "二面"),
    HR_INTERVIEW("HR_INTERVIEW", "HR面"),
    OFFER("OFFER", "已Offer"),
    REJECTED("REJECTED", "被拒"),
    GIVEN_UP("GIVEN_UP", "放弃");

    private final String code;
    private final String description;

    ApplicationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (ApplicationStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
```

### 6.3 投递优先级枚举

| 优先级 | 中文含义 |
| --- | --- |
| HIGH | 高 |
| MEDIUM | 中 |
| LOW | 低 |

### 6.4 ApplicationPriorityEnum

路径：

`src/main/java/com/internpilot/enums/ApplicationPriorityEnum.java`

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ApplicationPriorityEnum {

    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String description;

    ApplicationPriorityEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (ApplicationPriorityEnum priority : values()) {
            if (priority.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
```

## 7. 接口设计

### 7.1 创建投递记录

基本信息：

| 项目 | 内容 |
| --- | --- |
| URL | `/api/applications` |
| Method | `POST` |
| 权限 | `USER` |
| Content-Type | `application/json` |

请求参数：

```json
{
  "jobId": 1,
  "resumeId": 1,
  "reportId": 1,
  "status": "TO_APPLY",
  "applyDate": "2026-05-06",
  "interviewDate": null,
  "note": "准备投递该岗位",
  "priority": "HIGH"
}
```

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
    "createdAt": "2026-05-06 21:30:00"
  }
}
```

业务规则：

1. 用户必须登录；
2. `jobId` 不能为空；
3. 岗位必须属于当前用户；
4. 如果传入 `resumeId`，简历必须属于当前用户；
5. 如果传入 `reportId`，报告必须属于当前用户；
6. `status` 为空时默认 `TO_APPLY`；
7. `priority` 为空时默认 `MEDIUM`；
8. `status` 必须是合法枚举；
9. `priority` 必须是合法枚举；
10. 同一用户同一岗位第一阶段建议只允许创建一条投递记录。

### 7.2 查询投递记录列表

基本信息：

| 项目 | 内容 |
| --- | --- |
| URL | `/api/applications` |
| Method | `GET` |
| 权限 | `USER` |

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| status | String | 否 | 无 | 投递状态 |
| keyword | String | 否 | 无 | 公司名称或岗位名称关键词 |
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 10 | 每页数量 |

请求示例：

```http
GET /api/applications?status=APPLIED&keyword=Java&pageNum=1&pageSize=10
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
        "jobId": 1,
        "resumeId": 1,
        "reportId": 1,
        "companyName": "腾讯",
        "jobTitle": "Java后端开发实习生",
        "status": "APPLIED",
        "priority": "HIGH",
        "applyDate": "2026-05-06",
        "interviewDate": null,
        "note": "已投递，等待反馈",
        "updatedAt": "2026-05-06 21:40:00"
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
2. 支持按状态筛选；
3. 支持按公司名称或岗位名称关键词搜索；
4. 默认按更新时间倒序；
5. 逻辑删除的记录不显示。

### 7.3 查询投递记录详情

基本信息：

| 项目 | 内容 |
| --- | --- |
| URL | `/api/applications/{id}` |
| Method | `GET` |
| 权限 | `USER` |

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
    "resumeName": "Java后端实习简历",
    "matchScore": 82,
    "matchLevel": "MEDIUM_HIGH",
    "status": "APPLIED",
    "priority": "HIGH",
    "applyDate": "2026-05-06",
    "interviewDate": null,
    "note": "已投递，等待反馈",
    "review": null,
    "createdAt": "2026-05-06 21:30:00",
    "updatedAt": "2026-05-06 21:40:00"
  }
}
```

业务规则：

1. 投递记录必须存在；
2. 投递记录必须属于当前用户；
3. 可以展示关联岗位的公司名称和岗位名称；
4. 可以展示关联简历名称；
5. 可以展示关联报告的匹配分数和等级。

### 7.4 修改投递状态

基本信息：

| 项目 | 内容 |
| --- | --- |
| URL | `/api/applications/{id}/status` |
| Method | `PUT` |
| 权限 | `USER` |
| Content-Type | `application/json` |

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

业务规则：

1. 只能修改自己的投递记录；
2. 状态不能为空；
3. 状态必须是合法枚举；
4. 修改状态后更新 `updated_at`；
5. 第二阶段可以记录状态变化日志。

### 7.5 修改投递备注

基本信息：

| 项目 | 内容 |
| --- | --- |
| URL | `/api/applications/{id}/note` |
| Method | `PUT` |
| 权限 | `USER` |
| Content-Type | `application/json` |

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
2. `note` 可以为空；
3. `review` 可以为空；
4. `interviewDate` 可以为空；
5. 建议限制 `note` 和 `review` 最大长度。

### 7.6 删除投递记录

基本信息：

| 项目 | 内容 |
| --- | --- |
| URL | `/api/applications/{id}` |
| Method | `DELETE` |
| 权限 | `USER` |

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
3. 删除后列表不再显示；
4. 历史数据仍保留在数据库中。

## 8. Entity 与 Mapper 设计

### 8.1 ApplicationRecord Entity

路径：

`src/main/java/com/internpilot/entity/ApplicationRecord.java`

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("application_record")
public class ApplicationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long jobId;

    private Long resumeId;

    private Long reportId;

    private String status;

    private LocalDate applyDate;

    private LocalDateTime interviewDate;

    private String note;

    private String review;

    private String priority;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

### 8.2 ApplicationRecordMapper

路径：

`src/main/java/com/internpilot/mapper/ApplicationRecordMapper.java`

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.ApplicationRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationRecordMapper extends BaseMapper<ApplicationRecord> {
}
```

## 9. DTO 设计

### 9.1 ApplicationCreateRequest

路径：

`src/main/java/com/internpilot/dto/application/ApplicationCreateRequest.java`

```java
package com.internpilot.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建投递记录请求")
public class ApplicationCreateRequest {

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位 ID 不能为空")
    private Long jobId;

    @Schema(description = "简历ID", example = "1")
    private Long resumeId;

    @Schema(description = "分析报告ID", example = "1")
    private Long reportId;

    @Schema(description = "投递状态", example = "TO_APPLY")
    private String status;

    @Schema(description = "投递日期", example = "2026-05-06")
    private LocalDate applyDate;

    @Schema(description = "面试时间", example = "2026-05-10T14:00:00")
    private LocalDateTime interviewDate;

    @Schema(description = "备注", example = "准备投递该岗位")
    private String note;

    @Schema(description = "优先级", example = "HIGH")
    private String priority;
}
```

### 9.2 ApplicationStatusUpdateRequest

路径：

`src/main/java/com/internpilot/dto/application/ApplicationStatusUpdateRequest.java`

```java
package com.internpilot.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改投递状态请求")
public class ApplicationStatusUpdateRequest {

    @Schema(description = "投递状态", example = "APPLIED")
    @NotBlank(message = "投递状态不能为空")
    private String status;
}
```

### 9.3 ApplicationNoteUpdateRequest

路径：

`src/main/java/com/internpilot/dto/application/ApplicationNoteUpdateRequest.java`

```java
package com.internpilot.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "修改投递备注请求")
public class ApplicationNoteUpdateRequest {

    @Schema(description = "备注", example = "已完成一面，主要问了 Spring Security 和 Redis。")
    private String note;

    @Schema(description = "面试或笔试复盘", example = "Spring Security 过滤链回答不够清楚，需要复习。")
    private String review;

    @Schema(description = "面试时间", example = "2026-05-10T14:00:00")
    private LocalDateTime interviewDate;
}
```

## 10. VO 设计

### 10.1 ApplicationCreateResponse

路径：

`src/main/java/com/internpilot/vo/application/ApplicationCreateResponse.java`

```java
package com.internpilot.vo.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建投递记录响应")
public class ApplicationCreateResponse {

    @Schema(description = "投递记录ID")
    private Long applicationId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "投递状态")
    private String status;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 10.2 ApplicationListResponse

路径：

`src/main/java/com/internpilot/vo/application/ApplicationListResponse.java`

```java
package com.internpilot.vo.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "投递记录列表响应")
public class ApplicationListResponse {

    @Schema(description = "投递记录ID")
    private Long applicationId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "投递状态")
    private String status;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "投递日期")
    private LocalDate applyDate;

    @Schema(description = "面试时间")
    private LocalDateTime interviewDate;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
```

### 10.3 ApplicationDetailResponse

路径：

`src/main/java/com/internpilot/vo/application/ApplicationDetailResponse.java`

```java
package com.internpilot.vo.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "投递记录详情响应")
public class ApplicationDetailResponse {

    @Schema(description = "投递记录ID")
    private Long applicationId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "分析报告ID")
    private Long reportId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "匹配分数")
    private Integer matchScore;

    @Schema(description = "匹配等级")
    private String matchLevel;

    @Schema(description = "投递状态")
    private String status;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "投递日期")
    private LocalDate applyDate;

    @Schema(description = "面试时间")
    private LocalDateTime interviewDate;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "面试或笔试复盘")
    private String review;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
```

## 11. Service 设计

### 11.1 ApplicationService

路径：

`src/main/java/com/internpilot/service/ApplicationService.java`

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;

public interface ApplicationService {

    ApplicationCreateResponse create(ApplicationCreateRequest request);

    PageResult<ApplicationListResponse> list(
            String status,
            String keyword,
            Integer pageNum,
            Integer pageSize
    );

    ApplicationDetailResponse getDetail(Long id);

    Boolean updateStatus(Long id, ApplicationStatusUpdateRequest request);

    Boolean updateNote(Long id, ApplicationNoteUpdateRequest request);

    Boolean delete(Long id);
}
```

### 11.2 ApplicationServiceImpl

路径：

`src/main/java/com/internpilot/service/impl/ApplicationServiceImpl.java`

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.ApplicationRecord;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.enums.ApplicationPriorityEnum;
import com.internpilot.enums.ApplicationStatusEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.service.ApplicationService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRecordMapper applicationRecordMapper;

    private final JobDescriptionMapper jobDescriptionMapper;

    private final ResumeMapper resumeMapper;

    private final AnalysisReportMapper analysisReportMapper;

    @Override
    @Transactional
    public ApplicationCreateResponse create(ApplicationCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        JobDescription job = getUserJobOrThrow(request.getJobId(), currentUserId);

        if (request.getResumeId() != null) {
            getUserResumeOrThrow(request.getResumeId(), currentUserId);
        }

        if (request.getReportId() != null) {
            getUserReportOrThrow(request.getReportId(), currentUserId);
        }

        Long existingCount = applicationRecordMapper.selectCount(
                new LambdaQueryWrapper<ApplicationRecord>()
                        .eq(ApplicationRecord::getUserId, currentUserId)
                        .eq(ApplicationRecord::getJobId, request.getJobId())
                        .eq(ApplicationRecord::getDeleted, 0)
        );

        if (existingCount != null && existingCount > 0) {
            throw new BusinessException("该岗位已存在投递记录");
        }

        String status = StringUtils.hasText(request.getStatus())
                ? request.getStatus()
                : ApplicationStatusEnum.TO_APPLY.getCode();

        if (!ApplicationStatusEnum.isValid(status)) {
            throw new BusinessException("投递状态不合法");
        }

        String priority = StringUtils.hasText(request.getPriority())
                ? request.getPriority()
                : ApplicationPriorityEnum.MEDIUM.getCode();

        if (!ApplicationPriorityEnum.isValid(priority)) {
            throw new BusinessException("投递优先级不合法");
        }

        ApplicationRecord record = new ApplicationRecord();
        record.setUserId(currentUserId);
        record.setJobId(job.getId());
        record.setResumeId(request.getResumeId());
        record.setReportId(request.getReportId());
        record.setStatus(status);
        record.setApplyDate(request.getApplyDate());
        record.setInterviewDate(request.getInterviewDate());
        record.setNote(request.getNote());
        record.setPriority(priority);

        applicationRecordMapper.insert(record);

        return toCreateResponse(record);
    }

    @Override
    public PageResult<ApplicationListResponse> list(
            String status,
            String keyword,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (StringUtils.hasText(status) && !ApplicationStatusEnum.isValid(status)) {
            throw new BusinessException("投递状态不合法");
        }

        Page<ApplicationRecord> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ApplicationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationRecord::getUserId, currentUserId)
                .eq(ApplicationRecord::getDeleted, 0);

        if (StringUtils.hasText(status)) {
            wrapper.eq(ApplicationRecord::getStatus, status);
        }

        wrapper.orderByDesc(ApplicationRecord::getUpdatedAt);

        Page<ApplicationRecord> resultPage = applicationRecordMapper.selectPage(page, wrapper);

        List<ApplicationListResponse> records = resultPage.getRecords()
                .stream()
                .map(this::toListResponse)
                .filter(item -> matchKeyword(item, keyword))
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
    public ApplicationDetailResponse getDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        return toDetailResponse(record);
    }

    @Override
    @Transactional
    public Boolean updateStatus(Long id, ApplicationStatusUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        if (!ApplicationStatusEnum.isValid(request.getStatus())) {
            throw new BusinessException("投递状态不合法");
        }

        record.setStatus(request.getStatus());
        applicationRecordMapper.updateById(record);

        return true;
    }

    @Override
    @Transactional
    public Boolean updateNote(Long id, ApplicationNoteUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        record.setNote(request.getNote());
        record.setReview(request.getReview());
        record.setInterviewDate(request.getInterviewDate());

        applicationRecordMapper.updateById(record);

        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        ApplicationRecord record = getUserApplicationOrThrow(id, currentUserId);

        record.setDeleted(1);
        applicationRecordMapper.updateById(record);

        return true;
    }

    private ApplicationRecord getUserApplicationOrThrow(Long id, Long userId) {
        ApplicationRecord record = applicationRecordMapper.selectOne(
                new LambdaQueryWrapper<ApplicationRecord>()
                        .eq(ApplicationRecord::getId, id)
                        .eq(ApplicationRecord::getUserId, userId)
                        .eq(ApplicationRecord::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (record == null) {
            throw new BusinessException("投递记录不存在或无权限访问");
        }

        return record;
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

    private ApplicationCreateResponse toCreateResponse(ApplicationRecord record) {
        ApplicationCreateResponse response = new ApplicationCreateResponse();
        response.setApplicationId(record.getId());
        response.setJobId(record.getJobId());
        response.setResumeId(record.getResumeId());
        response.setReportId(record.getReportId());
        response.setStatus(record.getStatus());
        response.setPriority(record.getPriority());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private ApplicationListResponse toListResponse(ApplicationRecord record) {
        ApplicationListResponse response = new ApplicationListResponse();
        response.setApplicationId(record.getId());
        response.setJobId(record.getJobId());
        response.setResumeId(record.getResumeId());
        response.setReportId(record.getReportId());
        response.setStatus(record.getStatus());
        response.setPriority(record.getPriority());
        response.setApplyDate(record.getApplyDate());
        response.setInterviewDate(record.getInterviewDate());
        response.setNote(record.getNote());
        response.setUpdatedAt(record.getUpdatedAt());

        JobDescription job = jobDescriptionMapper.selectById(record.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        return response;
    }

    private ApplicationDetailResponse toDetailResponse(ApplicationRecord record) {
        ApplicationDetailResponse response = new ApplicationDetailResponse();
        response.setApplicationId(record.getId());
        response.setJobId(record.getJobId());
        response.setResumeId(record.getResumeId());
        response.setReportId(record.getReportId());
        response.setStatus(record.getStatus());
        response.setPriority(record.getPriority());
        response.setApplyDate(record.getApplyDate());
        response.setInterviewDate(record.getInterviewDate());
        response.setNote(record.getNote());
        response.setReview(record.getReview());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        JobDescription job = jobDescriptionMapper.selectById(record.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        if (record.getResumeId() != null) {
            Resume resume = resumeMapper.selectById(record.getResumeId());
            if (resume != null) {
                response.setResumeName(resume.getResumeName());
            }
        }

        if (record.getReportId() != null) {
            AnalysisReport report = analysisReportMapper.selectById(record.getReportId());
            if (report != null) {
                response.setMatchScore(report.getMatchScore());
                response.setMatchLevel(report.getMatchLevel());
            }
        }

        return response;
    }

    private boolean matchKeyword(ApplicationListResponse item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase();

        return containsIgnoreCase(item.getCompanyName(), lowerKeyword)
                || containsIgnoreCase(item.getJobTitle(), lowerKeyword);
    }

    private boolean containsIgnoreCase(String source, String lowerKeyword) {
        return source != null && source.toLowerCase().contains(lowerKeyword);
    }
}
```

### 11.3 关于列表 keyword 搜索的说明

上面的实现是：

1. 先查投递记录；
2. 再补充岗位信息；
3. 最后在 Java 内存中过滤 `keyword`。

优点：

1. 实现简单；
2. 不需要写 XML 联表 SQL；
3. 适合 MVP 阶段。

缺点：

1. 分页总数可能和过滤后 `records` 数量不完全一致；
2. 数据量大时效率不高。

第一阶段可以接受。

如果你想做得更企业级，后续可以写自定义 SQL：

```sql
SELECT ar.*, jd.company_name, jd.job_title
FROM application_record ar
LEFT JOIN job_description jd ON ar.job_id = jd.id
WHERE ar.user_id = ?
  AND ar.deleted = 0
  AND (jd.company_name LIKE ? OR jd.job_title LIKE ?)
ORDER BY ar.updated_at DESC
LIMIT ?, ?
```

## 12. Controller 设计

### 12.1 ApplicationController

路径：

`src/main/java/com/internpilot/controller/ApplicationController.java`

```java
package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.service.ApplicationService;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "投递记录管理接口")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "创建投递记录", description = "为当前用户的某个岗位创建投递记录")
    @PostMapping
    public Result<ApplicationCreateResponse> create(
            @RequestBody @Valid ApplicationCreateRequest request
    ) {
        return Result.success(applicationService.create(request));
    }

    @Operation(summary = "查询投递记录列表", description = "分页查询当前用户的投递记录")
    @GetMapping
    public Result<PageResult<ApplicationListResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(applicationService.list(status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "查询投递记录详情", description = "查询当前用户某条投递记录详情")
    @GetMapping("/{id}")
    public Result<ApplicationDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(applicationService.getDetail(id));
    }

    @Operation(summary = "修改投递状态", description = "修改当前用户某条投递记录的状态")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid ApplicationStatusUpdateRequest request
    ) {
        return Result.success(applicationService.updateStatus(id, request));
    }

    @Operation(summary = "修改投递备注", description = "修改当前用户某条投递记录的备注、复盘和面试时间")
    @PutMapping("/{id}/note")
    public Result<Boolean> updateNote(
            @PathVariable Long id,
            @RequestBody @Valid ApplicationNoteUpdateRequest request
    ) {
        return Result.success(applicationService.updateNote(id, request));
    }

    @Operation(summary = "删除投递记录", description = "逻辑删除当前用户某条投递记录")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(applicationService.delete(id));
    }
}
```

## 13. 数据权限设计

### 13.1 核心原则

投递记录属于用户私有数据。

所有操作都必须满足：

```text
application_record.user_id == 当前登录用户ID
```

创建投递记录时，还必须校验关联对象：

```text
job_description.user_id == 当前登录用户ID
resume.user_id == 当前登录用户ID
analysis_report.user_id == 当前登录用户ID
```

### 13.2 不能只校验 ID

错误做法：

```java
JobDescription job = jobDescriptionMapper.selectById(request.getJobId());
```

问题：

用户可能传入别人的 `jobId`，从而为别人的岗位创建投递记录。

正确做法：

```java
JobDescription job = jobDescriptionMapper.selectOne(
    new LambdaQueryWrapper<JobDescription>()
        .eq(JobDescription::getId, jobId)
        .eq(JobDescription::getUserId, currentUserId)
        .eq(JobDescription::getDeleted, 0)
);
```

## 14. 异常处理设计

### 14.1 常见异常

| 场景 | 错误信息 |
| --- | --- |
| jobId 为空 | 岗位 ID 不能为空 |
| 岗位不存在 | 岗位不存在或无权限访问 |
| 简历不存在 | 简历不存在或无权限访问 |
| 分析报告不存在 | 分析报告不存在或无权限访问 |
| 投递记录不存在 | 投递记录不存在或无权限访问 |
| 状态非法 | 投递状态不合法 |
| 优先级非法 | 投递优先级不合法 |
| 重复创建 | 该岗位已存在投递记录 |
| 未登录访问 | 请先登录 |

## 15. 事务设计

### 15.1 创建投递记录

建议加：

```java
@Transactional
```

原因：

1. 需要校验岗位、简历、报告；
2. 需要写入投递记录；
3. 后续可能写入状态日志。

### 15.2 修改投递状态

建议加：

```java
@Transactional
```

原因：

1. 更新当前记录；
2. 后续可能写入状态变化日志。

### 15.3 删除投递记录

建议加：

```java
@Transactional
```

原因：

1. 当前是逻辑删除；
2. 后续可能同步删除状态日志或写操作记录。

## 16. 测试流程

### 16.1 测试前置条件

1. 用户认证模块完成；
2. 简历模块完成；
3. 岗位 JD 模块完成；
4. AI 分析模块完成；
5. `application_record` 表已创建；
6. 已注册用户；
7. 已登录并获取 JWT Token；
8. 已创建岗位；
9. 可选：已上传简历；
10. 可选：已生成 AI 分析报告。

### 16.2 测试顺序

1. 登录获取 Token
2. 创建岗位 JD
3. 上传简历
4. 生成 AI 分析报告
5. 创建投递记录
6. 查询投递记录列表
7. 查询投递记录详情
8. 修改投递状态
9. 修改备注和复盘
10. 删除投递记录
11. 再次查询列表验证删除结果
12. 不带 Token 测试 401
13. 使用非法状态测试 400
14. 重复创建同一岗位投递记录测试 409 或业务异常

## 17. PowerShell 测试示例

### 17.1 登录获取 Token

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

### 17.2 创建投递记录

```powershell
$body = @{
  jobId = 1
  resumeId = 1
  reportId = 1
  status = "TO_APPLY"
  applyDate = "2026-05-06"
  note = "准备投递该岗位"
  priority = "HIGH"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/applications" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

### 17.3 查询投递记录列表

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/applications?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 17.4 按状态筛选

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/applications?status=TO_APPLY&pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 17.5 查询投递记录详情

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/applications/1" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 17.6 修改投递状态

```powershell
$body = @{
  status = "APPLIED"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/applications/1/status" `
  -Method Put `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

### 17.7 修改备注和复盘

```powershell
$body = @{
  note = "已完成一面，主要问了 Spring Security 和 Redis。"
  review = "Spring Security 过滤链回答不够清楚，需要复习。"
  interviewDate = "2026-05-10T14:00:00"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/applications/1/note" `
  -Method Put `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

### 17.8 删除投递记录

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/applications/1" `
  -Method Delete `
  -Headers @{ Authorization = "Bearer $token" }
```

## 18. Swagger 测试流程

访问：

```text
http://localhost:8080/doc.html
```

测试步骤：

1. 调用登录接口；
2. 复制返回 Token；
3. 点击 Authorize；
4. 输入 Bearer Token；
5. 打开投递记录管理接口；
6. 调用创建投递记录接口；
7. 调用投递记录列表接口；
8. 调用投递记录详情接口；
9. 调用修改投递状态接口；
10. 调用修改备注接口；
11. 调用删除接口。

## 19. 常见问题与解决方案

### 19.1 创建投递记录提示“岗位不存在或无权限访问”

可能原因：

1. `jobId` 不存在；
2. `jobId` 属于其他用户；
3. 岗位已被逻辑删除；
4. 当前 Token 对应用户不对。

排查 SQL：

```sql
SELECT id, user_id, company_name, job_title, deleted
FROM job_description
WHERE id = 1;
```

### 19.2 创建投递记录提示“该岗位已存在投递记录”

原因：

当前设计限制：

```text
同一用户同一岗位只能创建一条投递记录
```

如果你希望允许重复创建，可以删除这段校验。

但建议保留，因为一个岗位通常只需要一条投递记录。

### 19.3 修改状态提示“投递状态不合法”

检查传入状态是否属于：

```text
TO_APPLY
APPLIED
WRITTEN_TEST
FIRST_INTERVIEW
SECOND_INTERVIEW
HR_INTERVIEW
OFFER
REJECTED
GIVEN_UP
```

### 19.4 列表 keyword 搜索结果数量不准

原因：

当前 MVP 版本是在 Java 内存中过滤 `keyword`，而不是数据库联表分页查询。

解决：

第一阶段可以接受。

后续优化为自定义 Mapper XML 联表查询。

## 20. 后续增强：状态变化日志

第二阶段可以增加表：

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

状态变化流程：

```text
修改投递状态
  ↓
读取 oldStatus
  ↓
更新 newStatus
  ↓
插入 application_status_log
  ↓
前端展示投递时间线
```

## 21. 后续增强：投递数据看板

可以统计：

1. 总投递数；
2. 待投递数量；
3. 已投递数量；
4. 面试中数量；
5. Offer 数量；
6. 被拒数量；
7. 平均匹配分数；
8. 高优先级岗位数量；
9. 本周投递数量；
10. 本月投递数量。

接口示例：

```http
GET /api/dashboard/summary
```

## 22. 面试讲解准备

### 22.1 面试官可能问：投递记录模块的作用是什么？

回答思路：

投递记录模块是 InternPilot 的业务闭环模块。

前面的简历上传、岗位管理、AI 分析主要解决“是否匹配”和“怎么优化”的问题。

投递记录模块负责把分析结果转化为实际求职行动，记录用户对岗位的投递状态、面试时间、备注和复盘。

这样系统就不是单纯的 AI 分析工具，而是形成了完整的实习投递管理闭环。

### 22.2 面试官可能问：为什么投递记录要关联 jobId、resumeId、reportId？

回答思路：

`jobId` 是必需的，因为投递记录一定对应一个岗位。

`resumeId` 表示这次投递使用的是哪份简历，方便用户后续复盘不同版本简历效果。

`reportId` 表示该投递是否来源于某次 AI 分析报告，后续可以统计高匹配分数岗位的投递结果，比如匹配分数高是否更容易进入面试。

### 22.3 面试官可能问：如何防止用户访问别人的投递记录？

回答思路：

所有投递记录查询、修改和删除都不会只根据 `applicationId` 查询，而是同时加上 `userId` 条件。

`userId` 来自 JWT 解析后的当前登录用户。

创建投递记录时，也会校验 `jobId`、`resumeId`、`reportId` 都属于当前用户。

这样可以防止用户通过修改 URL 或请求体 ID 访问别人的数据。

### 22.4 面试官可能问：为什么使用逻辑删除？

回答思路：

投递记录属于用户求职过程中的历史数据，可能和岗位、简历、AI 分析报告有关联。

如果物理删除，后续统计和复盘会丢失上下文。

所以第一阶段使用 `deleted` 字段进行逻辑删除，列表中不展示已删除记录，但数据库中保留历史数据。

### 22.5 面试官可能问：这个模块后续怎么优化？

回答思路：

后续可以增加投递状态变化日志，每次状态从待投递到已投递、面试、Offer 都记录下来，前端展示成时间线。

还可以增加数据看板，统计投递数量、面试数量、Offer 数量和平均匹配分数。

进一步可以结合 AI 对面试复盘进行总结，生成下一次面试改进建议。

## 23. 开发顺序建议

建议按以下顺序开发：

1. 创建 `application_record` 表；
2. 创建 `ApplicationStatusEnum`；
3. 创建 `ApplicationPriorityEnum`；
4. 创建 `ApplicationRecord` Entity；
5. 创建 `ApplicationRecordMapper`；
6. 创建 `ApplicationCreateRequest`；
7. 创建 `ApplicationStatusUpdateRequest`；
8. 创建 `ApplicationNoteUpdateRequest`；
9. 创建 `ApplicationCreateResponse`；
10. 创建 `ApplicationListResponse`；
11. 创建 `ApplicationDetailResponse`；
12. 创建 `ApplicationService`；
13. 创建 `ApplicationServiceImpl`；
14. 创建 `ApplicationController`；
15. 启动项目；
16. Swagger 测试创建投递记录；
17. 测试投递记录列表；
18. 测试投递记录详情；
19. 测试修改状态；
20. 测试修改备注；
21. 测试删除；
22. 测试未登录访问返回 401；
23. 测试非法状态；
24. 测试访问他人数据。

## 24. 验收标准

### 24.1 创建投递记录验收

- 登录用户可以创建投递记录；
- 未登录用户不能创建投递记录；
- `jobId` 不能为空；
- 岗位必须属于当前用户；
- 简历如果传入，必须属于当前用户；
- 报告如果传入，必须属于当前用户；
- `status` 为空时默认为 `TO_APPLY`；
- `priority` 为空时默认为 `MEDIUM`；
- 非法 `status` 不能创建；
- 非法 `priority` 不能创建；
- 同一用户同一岗位不能重复创建投递记录。

### 24.2 查询投递记录验收

- 用户可以查看自己的投递记录列表；
- 用户只能查看自己的投递记录；
- 支持按状态筛选；
- 支持分页；
- 详情接口可以查看岗位名称；
- 详情接口可以查看简历名称；
- 详情接口可以查看匹配分数和匹配等级。

### 24.3 修改投递记录验收

- 用户可以修改自己的投递状态；
- 用户不能修改他人的投递状态；
- 非法状态不能修改；
- 用户可以修改备注；
- 用户可以修改复盘；
- 用户可以修改面试时间。

### 24.4 删除投递记录验收

- 用户可以删除自己的投递记录；
- 用户不能删除他人的投递记录；
- 删除后列表不再显示；
- 数据库中记录为逻辑删除。

## 25. 模块设计结论

投递记录模块是 InternPilot 后端 MVP 闭环的最后一块，负责把“AI 分析结果”转化为“实习投递管理”。

第一阶段采用：

```text
岗位关联 + 简历关联 + 报告关联 + 状态枚举 + 优先级枚举 + 逻辑删除 + 用户数据隔离
```

核心流程为：

```text
用户选择岗位
  ↓
可选关联简历和 AI 分析报告
  ↓
创建投递记录
  ↓
更新投递状态
  ↓
记录备注和复盘
  ↓
形成完整求职过程记录
```

完成该模块后，InternPilot 后端 MVP 将形成完整闭环：

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

这时项目就可以进入下一阶段：工程化增强，包括 README 更新、测试完善、Docker Compose、配置安全清理和前端页面开发。
