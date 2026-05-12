# InternPilot 简历版本管理与版本对比模块设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目中简历版本管理与版本对比模块的设计方案，包括模块背景、功能目标、数据库设计、接口设计、后端实现思路、前端页面设计、AI 优化版本设计、版本对比设计、测试流程和面试讲解准备。

当前 InternPilot 已经支持：

```text
简历上传解析
岗位 JD 管理
AI 匹配分析
AI 面试题生成
投递记录管理
RBAC 权限系统
管理员后台
```

但目前简历模块仍然是“单份简历管理”模式。

实际求职过程中，用户通常会针对不同岗位准备不同版本的简历，例如：

```text
Java 后端实习版
AI 工具链项目版
自动驾驶感知实习版
校园项目优化版
简洁投递版
面试强化版
```

因此需要增加简历版本管理，让用户可以保留原始简历、AI 优化后的简历、手动修改版本，并支持版本对比。

---

# 2. 模块背景

在实习投递场景中，一份简历往往不能适配所有岗位。

例如同一个学生可能投递：

```text
Java 后端开发实习生
AI 应用开发实习生
自动驾驶感知算法实习生
软件测试实习生
```

不同岗位关注点不同：

| 岗位      | 简历重点                           |
| ------- | ------------------------------ |
| Java 后端 | Spring Boot、MySQL、Redis、项目接口设计 |
| AI 应用开发 | LLM API、Prompt、RAG、AI 工具链      |
| 自动驾驶感知  | Python、深度学习、感知算法、点云            |
| 软件测试    | 接口测试、自动化测试、测试用例                |

如果用户只有一份简历，就难以针对岗位优化。

因此简历版本管理模块需要支持：

```text
原始简历
AI 优化版本
手动修改版本
岗位定制版本
版本对比
设为当前版本
```

---

# 3. 模块目标

简历版本管理模块需要完成以下目标：

1. 支持一份简历拥有多个版本；
2. 上传简历时自动创建原始版本；
3. 支持创建手动版本；
4. 支持基于 AI 优化建议生成新版本；
5. 支持设置当前版本；
6. 支持查看版本列表；
7. 支持查看版本详情；
8. 支持删除版本；
9. 支持版本对比；
10. 支持根据岗位生成定制版简历；
11. 支持 AI 分析时选择指定简历版本；
12. 支持 AI 面试题生成时选择指定简历版本；
13. 保证用户只能访问自己的简历版本；
14. 为后续在线简历编辑器做准备。

---

# 4. 当前简历模块存在的问题

当前简历模块大概率是：

```text
resume 表
  ├── id
  ├── user_id
  ├── resume_name
  ├── original_file_name
  ├── file_path
  ├── parsed_text
  ├── is_default
  └── deleted
```

问题：

1. 每份简历只有一个 parsedText；
2. 无法保留 AI 优化前后的差异；
3. 无法针对不同岗位生成不同版本；
4. 无法比较两个版本；
5. AI 分析只能基于整份简历，不够灵活；
6. 后续做“简历优化器”时缺少版本承载结构。

---

# 5. 改造思路

## 5.1 核心思路

保留现有 `resume` 表作为“简历主表”，新增 `resume_version` 表作为“版本表”。

关系：

```text
resume
  ↓ 一对多
resume_version
```

含义：

```text
resume 表：表示一份简历资产
resume_version 表：表示这份简历的不同内容版本
```

---

## 5.2 示例

用户上传一份简历：

```text
Java后端实习简历.pdf
```

系统创建：

```text
resume:
  id = 1
  resume_name = Java后端实习简历

resume_version:
  id = 1
  resume_id = 1
  version_name = 原始版本
  version_type = ORIGINAL
  content = 解析出的简历文本
  is_current = 1
```

之后用户根据腾讯 Java 岗位生成 AI 优化版本：

```text
resume_version:
  id = 2
  resume_id = 1
  version_name = 腾讯Java后端定制版
  version_type = AI_OPTIMIZED
  target_job_id = 3
  content = AI优化后的简历文本
  is_current = 0
```

---

# 6. 版本类型设计

## 6.1 版本类型枚举

| 类型           | 含义      |
| ------------ | ------- |
| ORIGINAL     | 原始版本    |
| MANUAL       | 手动编辑版本  |
| AI_OPTIMIZED | AI 优化版本 |
| JOB_TARGETED | 岗位定制版本  |
| IMPORTED     | 导入版本    |

---

## 6.2 ResumeVersionTypeEnum

路径：

```text
src/main/java/com/internpilot/enums/ResumeVersionTypeEnum.java
```

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ResumeVersionTypeEnum {

    ORIGINAL("ORIGINAL", "原始版本"),
    MANUAL("MANUAL", "手动编辑版本"),
    AI_OPTIMIZED("AI_OPTIMIZED", "AI优化版本"),
    JOB_TARGETED("JOB_TARGETED", "岗位定制版本"),
    IMPORTED("IMPORTED", "导入版本");

    private final String code;
    private final String description;

    ResumeVersionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (ResumeVersionTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
```

---

# 7. 数据库设计

## 7.1 resume_version 表

```sql
CREATE TABLE IF NOT EXISTS resume_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '简历版本ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历主表ID',
    version_name VARCHAR(200) NOT NULL COMMENT '版本名称',
    version_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL' COMMENT '版本类型',
    content LONGTEXT NOT NULL COMMENT '简历版本文本内容',
    content_summary VARCHAR(500) DEFAULT NULL COMMENT '内容摘要',
    target_job_id BIGINT DEFAULT NULL COMMENT '目标岗位ID',
    source_version_id BIGINT DEFAULT NULL COMMENT '来源版本ID',
    ai_report_id BIGINT DEFAULT NULL COMMENT '关联AI分析报告ID',
    optimize_prompt LONGTEXT DEFAULT NULL COMMENT 'AI优化提示词',
    ai_raw_response LONGTEXT DEFAULT NULL COMMENT 'AI原始返回',
    is_current TINYINT NOT NULL DEFAULT 0 COMMENT '是否当前版本：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_rv_user_id (user_id),
    KEY idx_rv_resume_id (resume_id),
    KEY idx_rv_target_job_id (target_job_id),
    KEY idx_rv_source_version_id (source_version_id),
    KEY idx_rv_ai_report_id (ai_report_id),
    KEY idx_rv_version_type (version_type),
    KEY idx_rv_is_current (is_current),
    KEY idx_rv_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历版本表';
```

---

## 7.2 字段说明

| 字段                | 说明           |
| ----------------- | ------------ |
| id                | 简历版本 ID      |
| user_id           | 用户 ID        |
| resume_id         | 所属简历         |
| version_name      | 版本名称         |
| version_type      | 版本类型         |
| content           | 当前版本简历文本内容   |
| content_summary   | 内容摘要         |
| target_job_id     | 该版本针对的岗位     |
| source_version_id | 来源版本         |
| ai_report_id      | 关联 AI 分析报告   |
| optimize_prompt   | AI 优化 Prompt |
| ai_raw_response   | AI 原始返回      |
| is_current        | 是否当前版本       |
| deleted           | 逻辑删除         |

---

## 7.3 是否修改 resume 表

第一阶段可以保留 resume 表中的：

```text
parsed_text
```

同时新增版本表。

推荐做法：

```text
resume.parsed_text：保留原始解析文本，兼容旧逻辑
resume_version.content：用于新版本体系
```

后续稳定后，可以让 AI 分析优先使用当前版本：

```text
优先使用 resume_version.is_current = 1 的 content
没有版本时使用 resume.parsed_text
```

---

# 8. Entity 与 Mapper 设计

## 8.1 ResumeVersion Entity

路径：

```text
src/main/java/com/internpilot/entity/ResumeVersion.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_version")
public class ResumeVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private String versionName;

    private String versionType;

    private String content;

    private String contentSummary;

    private Long targetJobId;

    private Long sourceVersionId;

    private Long aiReportId;

    private String optimizePrompt;

    private String aiRawResponse;

    private Integer isCurrent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

---

## 8.2 ResumeVersionMapper

路径：

```text
src/main/java/com/internpilot/mapper/ResumeVersionMapper.java
```

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.ResumeVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeVersionMapper extends BaseMapper<ResumeVersion> {
}
```

---

# 9. DTO 设计

## 9.1 ResumeVersionCreateRequest

路径：

```text
src/main/java/com/internpilot/dto/resume/ResumeVersionCreateRequest.java
```

```java
package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建简历版本请求")
public class ResumeVersionCreateRequest {

    @Schema(description = "版本名称", example = "Java后端实习优化版")
    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    @Schema(description = "版本类型", example = "MANUAL")
    private String versionType;

    @Schema(description = "版本内容")
    @NotBlank(message = "版本内容不能为空")
    private String content;

    @Schema(description = "目标岗位ID")
    private Long targetJobId;

    @Schema(description = "来源版本ID")
    private Long sourceVersionId;
}
```

---

## 9.2 ResumeVersionUpdateRequest

路径：

```text
src/main/java/com/internpilot/dto/resume/ResumeVersionUpdateRequest.java
```

```java
package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改简历版本请求")
public class ResumeVersionUpdateRequest {

    @Schema(description = "版本名称")
    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    @Schema(description = "版本内容")
    @NotBlank(message = "版本内容不能为空")
    private String content;
}
```

---

## 9.3 ResumeVersionOptimizeRequest

路径：

```text
src/main/java/com/internpilot/dto/resume/ResumeVersionOptimizeRequest.java
```

```java
package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "AI优化简历版本请求")
public class ResumeVersionOptimizeRequest {

    @Schema(description = "来源版本ID", example = "1")
    @NotNull(message = "来源版本ID不能为空")
    private Long sourceVersionId;

    @Schema(description = "目标岗位ID", example = "1")
    @NotNull(message = "目标岗位ID不能为空")
    private Long targetJobId;

    @Schema(description = "AI分析报告ID", example = "1")
    private Long aiReportId;

    @Schema(description = "新版本名称", example = "腾讯Java后端定制版")
    private String versionName;

    @Schema(description = "额外优化要求", example = "突出Spring Boot和Redis项目经验")
    private String extraRequirement;
}
```

---

## 9.4 ResumeVersionCompareRequest

路径：

```text
src/main/java/com/internpilot/dto/resume/ResumeVersionCompareRequest.java
```

```java
package com.internpilot.dto.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "简历版本对比请求")
public class ResumeVersionCompareRequest {

    @Schema(description = "旧版本ID")
    @NotNull(message = "旧版本ID不能为空")
    private Long oldVersionId;

    @Schema(description = "新版本ID")
    @NotNull(message = "新版本ID不能为空")
    private Long newVersionId;
}
```

---

# 10. VO 设计

## 10.1 ResumeVersionListResponse

路径：

```text
src/main/java/com/internpilot/vo/resume/ResumeVersionListResponse.java
```

```java
package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历版本列表响应")
public class ResumeVersionListResponse {

    private Long versionId;

    private Long resumeId;

    private String versionName;

    private String versionType;

    private String contentSummary;

    private Long targetJobId;

    private String targetCompanyName;

    private String targetJobTitle;

    private Integer isCurrent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

---

## 10.2 ResumeVersionDetailResponse

路径：

```text
src/main/java/com/internpilot/vo/resume/ResumeVersionDetailResponse.java
```

```java
package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历版本详情响应")
public class ResumeVersionDetailResponse {

    private Long versionId;

    private Long resumeId;

    private String versionName;

    private String versionType;

    private String content;

    private String contentSummary;

    private Long targetJobId;

    private String targetCompanyName;

    private String targetJobTitle;

    private Long sourceVersionId;

    private Long aiReportId;

    private Integer isCurrent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

---

## 10.3 ResumeVersionCreateResponse

路径：

```text
src/main/java/com/internpilot/vo/resume/ResumeVersionCreateResponse.java
```

```java
package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建简历版本响应")
public class ResumeVersionCreateResponse {

    private Long versionId;

    private Long resumeId;

    private String versionName;

    private String versionType;

    private Integer isCurrent;

    private LocalDateTime createdAt;
}
```

---

## 10.4 ResumeVersionCompareResponse

路径：

```text
src/main/java/com/internpilot/vo/resume/ResumeVersionCompareResponse.java
```

```java
package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "简历版本对比响应")
public class ResumeVersionCompareResponse {

    private Long oldVersionId;

    private String oldVersionName;

    private Long newVersionId;

    private String newVersionName;

    private String oldContent;

    private String newContent;

    private List<String> addedLines;

    private List<String> removedLines;

    private List<String> commonLines;

    private Integer oldLength;

    private Integer newLength;

    private Integer addedCount;

    private Integer removedCount;
}
```

---

# 11. Prompt 设计

## 11.1 AI 优化简历 Prompt 目标

AI 优化版本需要基于：

```text
原简历版本内容
目标岗位 JD
AI 分析报告
用户额外要求
```

生成一份更适合目标岗位的简历文本。

---

## 11.2 Prompt 模板

```text
你是一个资深 Java 后端简历优化导师和技术面试官。

请根据下面的【原始简历内容】、【目标岗位 JD】和【AI 匹配分析报告】，生成一份更适合该岗位投递的简历优化版本。

要求：
1. 不要编造用户没有的经历；
2. 可以优化表达方式，让项目经历更贴合岗位；
3. 突出和岗位相关的技术栈；
4. 对缺失技能只能建议补充，不能假装用户已经掌握；
5. 保持大学生实习简历风格；
6. 输出完整简历文本；
7. 不要返回 Markdown 代码块；
8. 不要添加解释，只返回优化后的简历内容。

【原始简历内容】
{resumeContent}

【目标岗位 JD】
{jobDescription}

【AI 匹配分析报告】
{analysisReport}

【用户额外优化要求】
{extraRequirement}
```

---

## 11.3 PromptUtils 增加方法

```java
public static String buildResumeOptimizePrompt(
        String resumeContent,
        String jobDescription,
        String analysisReport,
        String extraRequirement
) {
    return """
            你是一个资深 Java 后端简历优化导师和技术面试官。

            请根据下面的【原始简历内容】、【目标岗位 JD】和【AI 匹配分析报告】，生成一份更适合该岗位投递的简历优化版本。

            要求：
            1. 不要编造用户没有的经历；
            2. 可以优化表达方式，让项目经历更贴合岗位；
            3. 突出和岗位相关的技术栈；
            4. 对缺失技能只能建议补充，不能假装用户已经掌握；
            5. 保持大学生实习简历风格；
            6. 输出完整简历文本；
            7. 不要返回 Markdown 代码块；
            8. 不要添加解释，只返回优化后的简历内容。

            【原始简历内容】
            %s

            【目标岗位 JD】
            %s

            【AI 匹配分析报告】
            %s

            【用户额外优化要求】
            %s
            """.formatted(
            resumeContent,
            jobDescription,
            analysisReport,
            extraRequirement == null ? "无" : extraRequirement
    );
}
```

---

# 12. Service 设计

## 12.1 ResumeVersionService

路径：

```text
src/main/java/com/internpilot/service/ResumeVersionService.java
```

```java
package com.internpilot.service;

import com.internpilot.dto.resume.ResumeVersionCreateRequest;
import com.internpilot.dto.resume.ResumeVersionOptimizeRequest;
import com.internpilot.dto.resume.ResumeVersionUpdateRequest;
import com.internpilot.vo.resume.ResumeVersionCompareResponse;
import com.internpilot.vo.resume.ResumeVersionCreateResponse;
import com.internpilot.vo.resume.ResumeVersionDetailResponse;
import com.internpilot.vo.resume.ResumeVersionListResponse;

import java.util.List;

public interface ResumeVersionService {

    ResumeVersionCreateResponse create(Long resumeId, ResumeVersionCreateRequest request);

    List<ResumeVersionListResponse> list(Long resumeId);

    ResumeVersionDetailResponse getDetail(Long resumeId, Long versionId);

    Boolean update(Long resumeId, Long versionId, ResumeVersionUpdateRequest request);

    Boolean setCurrent(Long resumeId, Long versionId);

    Boolean delete(Long resumeId, Long versionId);

    ResumeVersionCreateResponse optimize(Long resumeId, ResumeVersionOptimizeRequest request);

    ResumeVersionCompareResponse compare(Long resumeId, Long oldVersionId, Long newVersionId);
}
```

---

# 13. 业务规则设计

## 13.1 上传简历时自动创建原始版本

当用户上传简历成功后：

```text
保存 resume
  ↓
解析 parsedText
  ↓
创建 resume_version
  ↓
versionType = ORIGINAL
  ↓
isCurrent = 1
```

这样即使用户后续做 AI 优化，也能保留原始版本。

---

## 13.2 一份简历只能有一个当前版本

设置当前版本时：

```text
将该 resume 下所有版本 is_current = 0
  ↓
将目标 version is_current = 1
```

---

## 13.3 删除当前版本的限制

如果用户删除当前版本，有两种策略：

### 方案 A：不允许删除当前版本

返回：

```text
当前版本不能删除，请先设置其他版本为当前版本
```

### 方案 B：删除后自动选择最近版本为当前版本

更智能，但逻辑更复杂。

第一阶段推荐：

```text
不允许删除当前版本
```

---

## 13.4 版本对比规则

第一阶段采用简单行级对比：

```text
按行切分 oldContent 和 newContent
找出新增行
找出删除行
找出相同行
```

这不是最完美的 diff，但实现简单，前端展示也够用。

---

# 14. 版本对比实现思路

## 14.1 简单行级 Diff

示例：

```java
private ResumeVersionCompareResponse compareContent(
        ResumeVersion oldVersion,
        ResumeVersion newVersion
) {
    List<String> oldLines = Arrays.stream(oldVersion.getContent().split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();

    List<String> newLines = Arrays.stream(newVersion.getContent().split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();

    Set<String> oldSet = new LinkedHashSet<>(oldLines);
    Set<String> newSet = new LinkedHashSet<>(newLines);

    List<String> added = newLines.stream()
            .filter(line -> !oldSet.contains(line))
            .toList();

    List<String> removed = oldLines.stream()
            .filter(line -> !newSet.contains(line))
            .toList();

    List<String> common = newLines.stream()
            .filter(oldSet::contains)
            .toList();

    ResumeVersionCompareResponse response = new ResumeVersionCompareResponse();
    response.setOldVersionId(oldVersion.getId());
    response.setOldVersionName(oldVersion.getVersionName());
    response.setNewVersionId(newVersion.getId());
    response.setNewVersionName(newVersion.getVersionName());
    response.setOldContent(oldVersion.getContent());
    response.setNewContent(newVersion.getContent());
    response.setAddedLines(added);
    response.setRemovedLines(removed);
    response.setCommonLines(common);
    response.setOldLength(oldVersion.getContent().length());
    response.setNewLength(newVersion.getContent().length());
    response.setAddedCount(added.size());
    response.setRemovedCount(removed.size());
    return response;
}
```

---

## 14.2 后续更专业的 Diff

后续可以引入：

```text
java-diff-utils
```

实现更专业的文本对比，包括：

```text
新增
删除
修改
上下文
行号
```

第一阶段不必过度复杂。

---

# 15. Controller 设计

## 15.1 ResumeVersionController

路径：

```text
src/main/java/com/internpilot/controller/ResumeVersionController.java
```

```java
package com.internpilot.controller;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.Result;
import com.internpilot.dto.resume.ResumeVersionCreateRequest;
import com.internpilot.dto.resume.ResumeVersionOptimizeRequest;
import com.internpilot.dto.resume.ResumeVersionUpdateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.ResumeVersionService;
import com.internpilot.vo.resume.ResumeVersionCompareResponse;
import com.internpilot.vo.resume.ResumeVersionCreateResponse;
import com.internpilot.vo.resume.ResumeVersionDetailResponse;
import com.internpilot.vo.resume.ResumeVersionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumes/{resumeId}/versions")
@RequiredArgsConstructor
public class ResumeVersionController {

    private final ResumeVersionService resumeVersionService;

    @Operation(summary = "创建简历版本")
    @OperationLog(module = "简历版本", operation = "创建简历版本", type = OperationTypeEnum.CREATE)
    @PreAuthorize("hasAuthority('resume:write')")
    @PostMapping
    public Result<ResumeVersionCreateResponse> create(
            @PathVariable Long resumeId,
            @RequestBody @Valid ResumeVersionCreateRequest request
    ) {
        return Result.success(resumeVersionService.create(resumeId, request));
    }

    @Operation(summary = "查询简历版本列表")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping
    public Result<List<ResumeVersionListResponse>> list(@PathVariable Long resumeId) {
        return Result.success(resumeVersionService.list(resumeId));
    }

    @Operation(summary = "查询简历版本详情")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping("/{versionId}")
    public Result<ResumeVersionDetailResponse> getDetail(
            @PathVariable Long resumeId,
            @PathVariable Long versionId
    ) {
        return Result.success(resumeVersionService.getDetail(resumeId, versionId));
    }

    @Operation(summary = "修改简历版本")
    @OperationLog(module = "简历版本", operation = "修改简历版本", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('resume:write')")
    @PutMapping("/{versionId}")
    public Result<Boolean> update(
            @PathVariable Long resumeId,
            @PathVariable Long versionId,
            @RequestBody @Valid ResumeVersionUpdateRequest request
    ) {
        return Result.success(resumeVersionService.update(resumeId, versionId, request));
    }

    @Operation(summary = "设置当前简历版本")
    @OperationLog(module = "简历版本", operation = "设置当前简历版本", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('resume:write')")
    @PutMapping("/{versionId}/current")
    public Result<Boolean> setCurrent(
            @PathVariable Long resumeId,
            @PathVariable Long versionId
    ) {
        return Result.success(resumeVersionService.setCurrent(resumeId, versionId));
    }

    @Operation(summary = "删除简历版本")
    @OperationLog(module = "简历版本", operation = "删除简历版本", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('resume:delete')")
    @DeleteMapping("/{versionId}")
    public Result<Boolean> delete(
            @PathVariable Long resumeId,
            @PathVariable Long versionId
    ) {
        return Result.success(resumeVersionService.delete(resumeId, versionId));
    }

    @Operation(summary = "AI优化生成简历版本")
    @OperationLog(module = "简历版本", operation = "AI优化简历版本", type = OperationTypeEnum.AI, recordParams = false)
    @PreAuthorize("hasAuthority('resume:write')")
    @PostMapping("/optimize")
    public Result<ResumeVersionCreateResponse> optimize(
            @PathVariable Long resumeId,
            @RequestBody @Valid ResumeVersionOptimizeRequest request
    ) {
        return Result.success(resumeVersionService.optimize(resumeId, request));
    }

    @Operation(summary = "对比两个简历版本")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping("/compare")
    public Result<ResumeVersionCompareResponse> compare(
            @PathVariable Long resumeId,
            @RequestParam Long oldVersionId,
            @RequestParam Long newVersionId
    ) {
        return Result.success(resumeVersionService.compare(resumeId, oldVersionId, newVersionId));
    }
}
```

---

# 16. 接口设计

## 16.1 创建简历版本

```text
POST /api/resumes/{resumeId}/versions
```

请求：

```json
{
  "versionName": "Java后端手动优化版",
  "versionType": "MANUAL",
  "content": "这里是简历文本内容",
  "targetJobId": 1,
  "sourceVersionId": 1
}
```

---

## 16.2 查询版本列表

```text
GET /api/resumes/{resumeId}/versions
```

---

## 16.3 查询版本详情

```text
GET /api/resumes/{resumeId}/versions/{versionId}
```

---

## 16.4 修改版本

```text
PUT /api/resumes/{resumeId}/versions/{versionId}
```

---

## 16.5 设置当前版本

```text
PUT /api/resumes/{resumeId}/versions/{versionId}/current
```

---

## 16.6 删除版本

```text
DELETE /api/resumes/{resumeId}/versions/{versionId}
```

---

## 16.7 AI 优化生成版本

```text
POST /api/resumes/{resumeId}/versions/optimize
```

请求：

```json
{
  "sourceVersionId": 1,
  "targetJobId": 2,
  "aiReportId": 3,
  "versionName": "腾讯Java后端定制版",
  "extraRequirement": "突出 Spring Security、Redis、MySQL 项目经验"
}
```

---

## 16.8 版本对比

```text
GET /api/resumes/{resumeId}/versions/compare?oldVersionId=1&newVersionId=2
```

---

# 17. 与 AI 分析模块的关系

## 17.1 当前 AI 分析输入

当前 AI 分析可能读取：

```text
resume.parsed_text
```

---

## 17.2 改造后 AI 分析输入

推荐改造为：

```text
如果 request.versionId 不为空：
    使用 resume_version.content
否则：
    使用当前版本 content
如果没有当前版本：
    使用 resume.parsed_text
```

---

## 17.3 AnalysisMatchRequest 增加 versionId

```java
@Schema(description = "简历版本ID")
private Long versionId;
```

---

## 17.4 分析报告记录 versionId

建议 `analysis_report` 表后续新增：

```sql
ALTER TABLE analysis_report
ADD COLUMN resume_version_id BIGINT DEFAULT NULL COMMENT '简历版本ID';
```

这样可以追踪：

```text
某次 AI 分析到底基于哪一个简历版本
```

---

# 18. 与 AI 面试题模块的关系

AI 面试题生成时也可以选择版本。

推荐：

```java
private Long resumeVersionId;
```

这样生成面试题时可以基于用户当前最想投递的版本，而不是原始简历。

---

# 19. 前端页面设计

## 19.1 页面入口

在简历管理页中，每份简历增加按钮：

```text
版本管理
```

点击进入：

```text
/resumes/{resumeId}/versions
```

---

## 19.2 简历版本管理页

页面功能：

```text
版本列表
创建版本
AI 优化版本
设置当前版本
查看详情
版本对比
删除版本
```

表格字段：

| 字段             | 说明                |
| -------------- | ----------------- |
| versionName    | 版本名称              |
| versionType    | 版本类型              |
| targetJobTitle | 目标岗位              |
| isCurrent      | 当前版本              |
| createdAt      | 创建时间              |
| 操作             | 详情 / 当前 / 对比 / 删除 |

---

## 19.3 简历版本详情页

展示：

```text
版本名称
版本类型
目标岗位
来源版本
是否当前
完整简历内容
```

可以用：

```text
el-input type="textarea"
```

展示和编辑文本。

---

## 19.4 版本对比页

页面分成左右两栏：

```text
左侧：旧版本
右侧：新版本
```

底部展示：

```text
新增内容
删除内容
字数变化
新增行数
删除行数
```

前端展示建议：

```text
新增行用绿色 tag
删除行用红色 tag
相同行普通显示
```

---

# 20. 前端 API 封装

路径：

```text
src/api/resumeVersion.ts
```

```ts
import request from '@/utils/request'

export function createResumeVersionApi(resumeId: number, data: any) {
  return request.post(`/api/resumes/${resumeId}/versions`, data)
}

export function getResumeVersionListApi(resumeId: number) {
  return request.get(`/api/resumes/${resumeId}/versions`)
}

export function getResumeVersionDetailApi(resumeId: number, versionId: number) {
  return request.get(`/api/resumes/${resumeId}/versions/${versionId}`)
}

export function updateResumeVersionApi(resumeId: number, versionId: number, data: any) {
  return request.put(`/api/resumes/${resumeId}/versions/${versionId}`, data)
}

export function setCurrentResumeVersionApi(resumeId: number, versionId: number) {
  return request.put(`/api/resumes/${resumeId}/versions/${versionId}/current`)
}

export function deleteResumeVersionApi(resumeId: number, versionId: number) {
  return request.delete(`/api/resumes/${resumeId}/versions/${versionId}`)
}

export function optimizeResumeVersionApi(resumeId: number, data: any) {
  return request.post(`/api/resumes/${resumeId}/versions/optimize`, data)
}

export function compareResumeVersionsApi(
  resumeId: number,
  oldVersionId: number,
  newVersionId: number
) {
  return request.get(`/api/resumes/${resumeId}/versions/compare`, {
    params: {
      oldVersionId,
      newVersionId
    }
  })
}
```

---

# 21. 前端路由设计

```ts
{
  path: 'resumes/:resumeId/versions',
  component: () => import('@/views/resume/ResumeVersionList.vue'),
  meta: { title: '简历版本管理', permission: 'resume:read' }
},
{
  path: 'resumes/:resumeId/versions/:versionId',
  component: () => import('@/views/resume/ResumeVersionDetail.vue'),
  meta: { title: '简历版本详情', permission: 'resume:read' }
},
{
  path: 'resumes/:resumeId/versions/compare',
  component: () => import('@/views/resume/ResumeVersionCompare.vue'),
  meta: { title: '简历版本对比', permission: 'resume:read' }
}
```

---

# 22. 权限设计

## 22.1 后端权限

| 操作      | 权限            |
| ------- | ------------- |
| 查询版本    | resume:read   |
| 查看详情    | resume:read   |
| 创建版本    | resume:write  |
| 修改版本    | resume:write  |
| 设置当前版本  | resume:write  |
| AI 优化版本 | resume:write  |
| 删除版本    | resume:delete |
| 版本对比    | resume:read   |

---

## 22.2 数据权限

所有版本操作都必须校验：

```text
resume.user_id = 当前用户
resume_version.user_id = 当前用户
resume_version.resume_id = resumeId
```

不能只通过 versionId 查询。

---

# 23. 测试流程

## 23.1 上传简历自动创建原始版本

上传简历后检查：

```sql
SELECT *
FROM resume_version
WHERE resume_id = 1;
```

期望：

```text
存在一条 ORIGINAL 版本
is_current = 1
content 不为空
```

---

## 23.2 创建手动版本

```http
POST /api/resumes/1/versions
```

请求：

```json
{
  "versionName": "手动优化版",
  "versionType": "MANUAL",
  "content": "新的简历内容"
}
```

期望：

```text
创建成功
version_type = MANUAL
is_current = 0
```

---

## 23.3 设置当前版本

```http
PUT /api/resumes/1/versions/2/current
```

期望：

```text
versionId = 2 的 is_current = 1
其他版本 is_current = 0
```

---

## 23.4 AI 优化版本

```http
POST /api/resumes/1/versions/optimize
```

请求：

```json
{
  "sourceVersionId": 1,
  "targetJobId": 1,
  "aiReportId": 1,
  "versionName": "腾讯Java后端定制版",
  "extraRequirement": "突出 Spring Security 和 Redis"
}
```

期望：

```text
生成 AI_OPTIMIZED 或 JOB_TARGETED 版本
content 是 AI 优化后的简历内容
source_version_id = 1
target_job_id = 1
```

---

## 23.5 版本对比

```http
GET /api/resumes/1/versions/compare?oldVersionId=1&newVersionId=2
```

期望：

```text
返回 addedLines
返回 removedLines
返回 oldContent / newContent
```

---

# 24. 常见问题

## 24.1 版本内容和 resume.parsedText 不一致怎么办？

这是正常的。

```text
resume.parsedText 是原始解析文本
resume_version.content 是用户后续维护和优化的版本内容
```

后续 AI 分析建议优先使用当前版本。

---

## 24.2 AI 优化会不会编造经历？

这是最大风险。

Prompt 中必须明确：

```text
不要编造用户没有的经历
不能假装用户掌握缺失技能
只能优化表达和建议补充
```

前端也可以提示：

```text
AI 优化结果仅供参考，投递前请自行核对真实性。
```

---

## 24.3 版本太多怎么办？

第一阶段不处理。

后续可以：

```text
限制每份简历最多 20 个版本
支持归档版本
支持搜索版本
```

---

## 24.4 删除原始版本怎么办？

建议：

```text
原始版本不允许删除
当前版本不允许删除
```

或者至少不允许删除最后一个版本。

---

# 25. 面试讲解准备

## 25.1 面试官问：为什么要做简历版本管理？

回答：

```text
因为求职时同一份简历往往要针对不同岗位进行优化。比如 Java 后端岗位更关注 Spring Boot、MySQL、Redis，而 AI 应用岗位更关注大模型 API、Prompt 和 RAG。如果系统只保存一份简历，就无法追踪不同岗位的优化版本。

所以我设计了 resume_version 表，让一份简历可以拥有多个版本，包括原始版本、手动编辑版本和 AI 优化版本。AI 分析和面试题生成也可以基于指定版本进行。
```

---

## 25.2 面试官问：怎么实现版本对比？

回答：

```text
第一阶段我采用简单的行级文本对比。系统会把两个版本的简历内容按行拆分，然后比较新增行、删除行和相同行，并返回给前端展示。

这种方式实现简单，适合文本型简历对比。后续如果要做得更专业，可以引入 java-diff-utils，实现带行号和修改块的 Diff。
```

---

## 25.3 面试官问：AI 优化简历怎么避免造假？

回答：

```text
这是 AI 简历优化中很重要的问题。我在 Prompt 中明确要求模型不能编造用户没有的经历，也不能把缺失技能写成已经掌握。AI 只能优化表达方式、突出已有项目中和岗位相关的部分，并对缺失技能提出补充建议。

另外，前端会提示用户 AI 生成内容仅供参考，投递前需要人工确认真实性。
```

---

## 25.4 面试官问：AI 分析使用哪个版本？

回答：

```text
我设计上支持两种方式。用户可以显式选择某个 resumeVersionId 进行 AI 分析；如果没有选择，则系统默认使用该简历的当前版本。如果当前版本不存在，再回退到 resume.parsedText。

这样既兼容旧逻辑，又支持新版本体系。
```

---

# 26. 简历写法

完成该模块后，简历可以写：

```text
- 设计并实现简历版本管理模块，支持原始版本、手动编辑版本和 AI 优化版本，用户可针对不同岗位生成定制化简历版本，并支持版本对比和当前版本切换。
```

更完整写法：

```text
- 基于 resume_version 表实现简历版本体系，支持上传简历自动生成原始版本、AI 根据岗位 JD 和匹配分析报告生成优化版本，并通过行级 Diff 返回新增/删除内容，提升简历优化和岗位定制能力。
```

---

# 27. 开发顺序建议

推荐按以下顺序开发：

```text
1. 创建 resume_version 表；
2. 创建 ResumeVersionTypeEnum；
3. 创建 ResumeVersion Entity；
4. 创建 ResumeVersionMapper；
5. 创建 DTO / VO；
6. 创建 ResumeVersionService；
7. 实现版本列表、详情、创建、修改；
8. 实现设置当前版本；
9. 实现删除版本；
10. 上传简历成功后自动创建 ORIGINAL 版本；
11. PromptUtils 增加简历优化 Prompt；
12. 实现 AI 优化生成版本；
13. 实现版本对比；
14. 改造 AI 分析模块支持 resumeVersionId；
15. 改造 AI 面试题模块支持 resumeVersionId；
16. 前端新增版本管理页；
17. 前端新增版本对比页；
18. 完整测试。
```

---

# 28. 验收标准

## 28.1 后端验收

* [ ] 上传简历后自动创建 ORIGINAL 版本；
* [ ] 可以查询版本列表；
* [ ] 可以查询版本详情；
* [ ] 可以创建手动版本；
* [ ] 可以修改版本；
* [ ] 可以设置当前版本；
* [ ] 同一简历只有一个当前版本；
* [ ] 当前版本不能被删除；
* [ ] 可以 AI 优化生成版本；
* [ ] 可以对比两个版本；
* [ ] 用户不能访问他人的简历版本；
* [ ] AI 分析可以使用当前版本；
* [ ] AI 分析可以指定 resumeVersionId。

---

## 28.2 前端验收

* [ ] 简历列表有“版本管理”按钮；
* [ ] 版本管理页可以加载；
* [ ] 可以查看版本详情；
* [ ] 可以创建版本；
* [ ] 可以 AI 优化版本；
* [ ] 可以设置当前版本；
* [ ] 可以对比两个版本；
* [ ] 新增内容和删除内容展示清楚；
* [ ] 普通用户只能看到自己的版本。

---

# 29. 模块设计结论

简历版本管理模块让 InternPilot 从“简历上传工具”升级为“简历持续优化工具”。

它将原来的：

```text
一份简历 = 一个 parsedText
```

升级为：

```text
一份简历 = 多个版本
  ├── 原始版本
  ├── 手动优化版本
  ├── AI 优化版本
  └── 岗位定制版本
```

完成该模块后，InternPilot 的求职闭环会更完整：

```text
上传简历
  ↓
生成原始版本
  ↓
AI 匹配分析
  ↓
AI 优化简历版本
  ↓
版本对比
  ↓
选择当前版本投递
  ↓
生成面试题
  ↓
投递记录与复盘
```
