# InternPilot AI 面试题生成模块设计与实现文档

## 1. 文档说明

本文档用于描述 InternPilot 项目中 AI 面试题生成模块的设计与实现方案，包括模块背景、功能目标、业务流程、数据库设计、Prompt 设计、接口设计、DTO/VO 设计、Entity/Mapper 设计、Service 设计、Controller 设计、前端页面设计、测试流程、异常处理和面试讲解准备。

当前 InternPilot 已经支持：

```text
简历上传解析
岗位 JD 管理
AI 简历岗位匹配分析
分析报告生成
投递记录管理
WebSocket AI 分析进度展示
```

下一步增加 AI 面试题生成模块，让系统不只告诉用户“匹配不匹配”，还可以告诉用户“面试应该准备什么”。

## 2. 模块背景

用户拿到 AI 分析报告后，通常会看到：

匹配分数
简历优势
简历短板
缺失技能
简历优化建议
面试准备建议

但是“面试准备建议”还比较概括。

例如：

准备 Spring Security 过滤链
准备 Redis 缓存问题
准备 MySQL 索引优化

用户还需要进一步知道：

具体会问什么题？
怎么回答？
哪些是基础题？
哪些是项目追问题？
哪些是 HR 问题？

因此可以新增 AI 面试题生成模块，根据：

简历文本
岗位 JD
AI 分析报告
缺失技能
用户项目经历

自动生成一套岗位定制化面试题。

## 3. 模块目标

AI 面试题生成模块需要完成以下目标：

支持根据简历和岗位生成面试题；
支持结合已有 AI 分析报告生成更精准的问题；
支持生成技术基础题；
支持生成框架题；
支持生成数据库题；
支持生成 Redis / 缓存题；
支持生成项目追问题；
支持生成 HR 面试题；
支持生成参考答案；
支持生成答题要点；
支持保存面试题报告；
支持查询历史面试题报告；
支持查看面试题详情；
支持 Redis 缓存或复用已有结果；
支持后续前端展示和导出。

## 4. 模块业务定位

AI 面试题生成模块位于 AI 分析模块之后。

完整业务链路：

用户上传简历
  ↓
用户创建岗位 JD
  ↓
AI 匹配分析
  ↓
生成分析报告
  ↓
AI 面试题生成
  ↓
用户根据题目准备面试
  ↓
投递记录中记录面试复盘

它的作用是把：

AI 分析报告

进一步转化为：

面试准备资料

这让 InternPilot 从“投递管理工具”升级为“实习求职训练工具”。

## 5. 第一阶段功能范围

### 5.1 必须实现

第一阶段建议实现：

创建面试题报告；
根据 resumeId 和 jobId 生成面试题；
可选关联 reportId；
调用 AI 生成结构化 JSON；
保存题目和答案到数据库；
查询面试题报告列表；
查询面试题报告详情；
用户数据权限控制；
AI 异常处理；
前端展示题目列表。

### 5.2 暂不实现

第一阶段暂不做：

在线答题评分；
语音模拟面试；
题目收藏；
题目错题本；
多轮追问；
AI 批改用户答案；
导出 PDF；
题库人工维护后台。

这些可以作为后续增强。

## 6. 题目类型设计

### 6.1 推荐题目分类

AI 面试题可以分为以下类型：

类型	说明
JAVA_BASIC	Java 基础
SPRING_BOOT	Spring Boot
SPRING_SECURITY	Spring Security
MYSQL	MySQL
REDIS	Redis
PROJECT	项目追问
ALGORITHM	算法与数据结构
SYSTEM_DESIGN	系统设计
HR	HR 面试
RESUME	简历深挖
JOB_SKILL	岗位技能专项

### 6.2 难度等级

等级	说明
EASY	简单
MEDIUM	中等
HARD	较难

### 6.3 第一阶段生成数量

建议第一阶段每次生成：

Java 基础题：3 道
框架题：3 道
数据库题：2 道
Redis / 缓存题：2 道
项目追问题：4 道
HR 题：3 道

总计约：

15 - 20 道

不要生成太多，否则 AI 输出太长，不稳定。

## 7. 数据库设计

第一阶段推荐使用两张表：

interview_question_report
interview_question

### 7.1 interview_question_report 表

```sql
CREATE TABLE IF NOT EXISTS interview_question_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '面试题报告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    job_id BIGINT NOT NULL COMMENT '岗位ID',
    analysis_report_id BIGINT DEFAULT NULL COMMENT '关联AI分析报告ID',
    title VARCHAR(200) NOT NULL COMMENT '报告标题',
    question_count INT NOT NULL DEFAULT 0 COMMENT '题目数量',
    ai_provider VARCHAR(50) DEFAULT NULL COMMENT 'AI服务商',
    ai_model VARCHAR(100) DEFAULT NULL COMMENT 'AI模型',
    raw_ai_response LONGTEXT DEFAULT NULL COMMENT 'AI原始返回',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_iqr_user_id (user_id),
    KEY idx_iqr_resume_id (resume_id),
    KEY idx_iqr_job_id (job_id),
    KEY idx_iqr_analysis_report_id (analysis_report_id),
    KEY idx_iqr_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试题报告表';
```

### 7.2 interview_question 表

```sql
CREATE TABLE IF NOT EXISTS interview_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    report_id BIGINT NOT NULL COMMENT '面试题报告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_type VARCHAR(50) NOT NULL COMMENT '题目类型',
    difficulty VARCHAR(30) NOT NULL DEFAULT 'MEDIUM' COMMENT '难度等级',
    question TEXT NOT NULL COMMENT '题目内容',
    answer TEXT DEFAULT NULL COMMENT '参考答案',
    answer_points TEXT DEFAULT NULL COMMENT '答题要点，JSON数组字符串',
    related_skills VARCHAR(500) DEFAULT NULL COMMENT '相关技能，JSON数组字符串',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_iq_report_id (report_id),
    KEY idx_iq_user_id (user_id),
    KEY idx_iq_question_type (question_type),
    KEY idx_iq_difficulty (difficulty),
    KEY idx_iq_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI面试题表';
```

### 7.3 字段说明

interview_question_report

字段	说明
id	报告 ID
user_id	用户 ID
resume_id	简历 ID
job_id	岗位 ID
analysis_report_id	关联分析报告 ID
title	报告标题
question_count	题目数量
ai_provider	AI 服务商
ai_model	AI 模型
raw_ai_response	AI 原始返回
deleted	逻辑删除

interview_question

字段	说明
report_id	所属面试题报告
question_type	题目类型
difficulty	难度
question	题目
answer	参考答案
answer_points	答题要点
related_skills	相关技能
sort_order	排序

## 8. 枚举设计

### 8.1 InterviewQuestionTypeEnum

路径：

src/main/java/com/internpilot/enums/InterviewQuestionTypeEnum.java

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum InterviewQuestionTypeEnum {

    JAVA_BASIC("JAVA_BASIC", "Java基础"),
    SPRING_BOOT("SPRING_BOOT", "Spring Boot"),
    SPRING_SECURITY("SPRING_SECURITY", "Spring Security"),
    MYSQL("MYSQL", "MySQL"),
    REDIS("REDIS", "Redis"),
    PROJECT("PROJECT", "项目追问"),
    ALGORITHM("ALGORITHM", "算法与数据结构"),
    SYSTEM_DESIGN("SYSTEM_DESIGN", "系统设计"),
    HR("HR", "HR面试"),
    RESUME("RESUME", "简历深挖"),
    JOB_SKILL("JOB_SKILL", "岗位技能专项");

    private final String code;
    private final String description;

    InterviewQuestionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (InterviewQuestionTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
```

### 8.2 QuestionDifficultyEnum

路径：

src/main/java/com/internpilot/enums/QuestionDifficultyEnum.java

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum QuestionDifficultyEnum {

    EASY("EASY", "简单"),
    MEDIUM("MEDIUM", "中等"),
    HARD("HARD", "较难");

    private final String code;
    private final String description;

    QuestionDifficultyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (QuestionDifficultyEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
```

## 9. Entity 设计

### 9.1 InterviewQuestionReport

路径：

src/main/java/com/internpilot/entity/InterviewQuestionReport.java

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_question_report")
public class InterviewQuestionReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jobId;

    private Long analysisReportId;

    private String title;

    private Integer questionCount;

    private String aiProvider;

    private String aiModel;

    private String rawAiResponse;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

### 9.2 InterviewQuestion

路径：

src/main/java/com/internpilot/entity/InterviewQuestion.java

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_question")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private Long userId;

    private String questionType;

    private String difficulty;

    private String question;

    private String answer;

    private String answerPoints;

    private String relatedSkills;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

## 10. Mapper 设计

### 10.1 InterviewQuestionReportMapper

路径：

src/main/java/com/internpilot/mapper/InterviewQuestionReportMapper.java

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.InterviewQuestionReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewQuestionReportMapper extends BaseMapper<InterviewQuestionReport> {
}
```

### 10.2 InterviewQuestionMapper

路径：

src/main/java/com/internpilot/mapper/InterviewQuestionMapper.java

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {
}
```

## 11. AI 返回 DTO 设计

### 11.1 AiInterviewQuestionResult

路径：

src/main/java/com/internpilot/dto/interview/AiInterviewQuestionResult.java

```java
package com.internpilot.dto.interview;

import lombok.Data;

import java.util.List;

@Data
public class AiInterviewQuestionResult {

    private String title;

    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {

        private String questionType;

        private String difficulty;

        private String question;

        private String answer;

        private List<String> answerPoints;

        private List<String> relatedSkills;
    }
}
```

## 12. 请求 DTO 设计

### 12.1 InterviewQuestionGenerateRequest

路径：

src/main/java/com/internpilot/dto/interview/InterviewQuestionGenerateRequest.java

```java
package com.internpilot.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "生成AI面试题请求")
public class InterviewQuestionGenerateRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位ID不能为空")
    private Long jobId;

    @Schema(description = "AI分析报告ID", example = "1")
    private Long analysisReportId;

    @Schema(description = "是否强制重新生成", example = "false")
    private Boolean forceRefresh = false;
}
```

## 13. 响应 VO 设计

### 13.1 InterviewQuestionGenerateResponse

路径：

src/main/java/com/internpilot/vo/interview/InterviewQuestionGenerateResponse.java

```java
package com.internpilot.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "生成AI面试题响应")
public class InterviewQuestionGenerateResponse {

    @Schema(description = "面试题报告ID")
    private Long reportId;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "AI分析报告ID")
    private Long analysisReportId;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 13.2 InterviewQuestionListResponse

路径：

src/main/java/com/internpilot/vo/interview/InterviewQuestionListResponse.java

```java
package com.internpilot.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "面试题报告列表响应")
public class InterviewQuestionListResponse {

    @Schema(description = "面试题报告ID")
    private Long reportId;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 13.3 InterviewQuestionItemResponse

路径：

src/main/java/com/internpilot/vo/interview/InterviewQuestionItemResponse.java

```java
package com.internpilot.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "面试题详情项")
public class InterviewQuestionItemResponse {

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "题目类型")
    private String questionType;

    @Schema(description = "难度")
    private String difficulty;

    @Schema(description = "题目")
    private String question;

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "答题要点")
    private List<String> answerPoints;

    @Schema(description = "相关技能")
    private List<String> relatedSkills;

    @Schema(description = "排序")
    private Integer sortOrder;
}
```

### 13.4 InterviewQuestionDetailResponse

路径：

src/main/java/com/internpilot/vo/interview/InterviewQuestionDetailResponse.java

```java
package com.internpilot.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "面试题报告详情响应")
public class InterviewQuestionDetailResponse {

    @Schema(description = "面试题报告ID")
    private Long reportId;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "AI分析报告ID")
    private Long analysisReportId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "题目列表")
    private List<InterviewQuestionItemResponse> questions;
}
```

## 14. Prompt 设计

### 14.1 Prompt 目标

Prompt 需要让 AI 根据简历、岗位和分析报告生成：

岗位定制化面试题；
问题分类；
难度等级；
参考答案；
答题要点；
相关技能；
严格 JSON 格式。

### 14.2 Prompt 模板

你是一个资深 Java 后端面试官、技术招聘官和大学生实习求职导师。

请根据下面的【学生简历文本】、【目标岗位 JD】和【AI 匹配分析报告】，为该学生生成一套适合该岗位的实习面试准备题。

要求：
1. 题目要贴合岗位 JD；
2. 题目要结合学生简历中的项目经历；
3. 项目追问题要针对简历中的具体项目；
4. 如果分析报告中提到缺失技能，需要生成对应补强题；
5. 题目难度要符合大学生暑期实习面试；
6. 答案要简洁但有要点；
7. 不要生成过于宽泛的问题；
8. 必须严格返回 JSON；
9. 不要返回 Markdown；
10. 不要使用 ```json 代码块包裹。

请生成 15 到 20 道题，题目类型包含：
- JAVA_BASIC
- SPRING_BOOT
- SPRING_SECURITY
- MYSQL
- REDIS
- PROJECT
- HR
- RESUME
- JOB_SKILL

难度只能是：
- EASY
- MEDIUM
- HARD

返回格式如下：

{
  "title": "腾讯 Java后端开发实习生 面试题准备",
  "questions": [
    {
      "questionType": "SPRING_SECURITY",
      "difficulty": "MEDIUM",
      "question": "请介绍 Spring Security 的过滤器链执行流程。",
      "answer": "Spring Security 通过一组过滤器对请求进行认证和授权处理...",
      "answerPoints": [
        "请求先经过 SecurityFilterChain",
        "JWT 项目中会经过自定义 JwtAuthenticationFilter",
        "认证成功后将 Authentication 放入 SecurityContext"
      ],
      "relatedSkills": [
        "Spring Security",
        "JWT",
        "Filter"
      ]
    }
  ]
}

【学生简历文本】
{resumeText}

【目标岗位 JD】
{jobDescription}

【AI 匹配分析报告】
{analysisReport}

### 14.3 PromptUtils 增加方法

路径：

src/main/java/com/internpilot/util/PromptUtils.java

```java
public static String buildInterviewQuestionPrompt(
        String resumeText,
        String jobDescription,
        String analysisReport
) {
    return """
            你是一个资深 Java 后端面试官、技术招聘官和大学生实习求职导师。

            请根据下面的【学生简历文本】、【目标岗位 JD】和【AI 匹配分析报告】，为该学生生成一套适合该岗位的实习面试准备题。

            要求：
            1. 题目要贴合岗位 JD；
            2. 题目要结合学生简历中的项目经历；
            3. 项目追问题要针对简历中的具体项目；
            4. 如果分析报告中提到缺失技能，需要生成对应补强题；
            5. 题目难度要符合大学生暑期实习面试；
            6. 答案要简洁但有要点；
            7. 不要生成过于宽泛的问题；
            8. 必须严格返回 JSON；
            9. 不要返回 Markdown；
            10. 不要使用 ```json 代码块包裹。

            请生成 15 到 20 道题，题目类型包含：
            - JAVA_BASIC
            - SPRING_BOOT
            - SPRING_SECURITY
            - MYSQL
            - REDIS
            - PROJECT
            - HR
            - RESUME
            - JOB_SKILL

            难度只能是：
            - EASY
            - MEDIUM
            - HARD

            返回格式如下：

            {
              "title": "腾讯 Java后端开发实习生 面试题准备",
              "questions": [
                {
                  "questionType": "SPRING_SECURITY",
                  "difficulty": "MEDIUM",
                  "question": "请介绍 Spring Security 的过滤器链执行流程。",
                  "answer": "Spring Security 通过一组过滤器对请求进行认证和授权处理...",
                  "answerPoints": [
                    "请求先经过 SecurityFilterChain",
                    "JWT 项目中会经过自定义 JwtAuthenticationFilter",
                    "认证成功后将 Authentication 放入 SecurityContext"
                  ],
                  "relatedSkills": [
                    "Spring Security",
                    "JWT",
                    "Filter"
                  ]
                }
              ]
            }

            【学生简历文本】
            %s

            【目标岗位 JD】
            %s

            【AI 匹配分析报告】
            %s
            """.formatted(resumeText, jobDescription, analysisReport);
}
```

## 15. Service 设计

### 15.1 InterviewQuestionService

路径：

src/main/java/com/internpilot/service/InterviewQuestionService.java

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;

public interface InterviewQuestionService {

    InterviewQuestionGenerateResponse generate(InterviewQuestionGenerateRequest request);

    PageResult<InterviewQuestionListResponse> list(
            Long resumeId,
            Long jobId,
            Integer pageNum,
            Integer pageSize
    );

    InterviewQuestionDetailResponse getDetail(Long reportId);

    Boolean delete(Long reportId);
}
```

### 15.2 InterviewQuestionServiceImpl

路径：

src/main/java/com/internpilot/service/impl/InterviewQuestionServiceImpl.java

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.config.AiProperties;
import com.internpilot.dto.interview.AiInterviewQuestionResult;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.InterviewQuestion;
import com.internpilot.entity.InterviewQuestionReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.enums.InterviewQuestionTypeEnum;
import com.internpilot.enums.QuestionDifficultyEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.InterviewQuestionMapper;
import com.internpilot.mapper.InterviewQuestionReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.service.AiClient;
import com.internpilot.service.InterviewQuestionService;
import com.internpilot.util.JsonUtils;
import com.internpilot.util.PromptUtils;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionItemResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private final ResumeMapper resumeMapper;

    private final JobDescriptionMapper jobDescriptionMapper;

    private final AnalysisReportMapper analysisReportMapper;

    private final InterviewQuestionReportMapper interviewQuestionReportMapper;

    private final InterviewQuestionMapper interviewQuestionMapper;

    private final AiClient aiClient;

    private final AiProperties aiProperties;

    @Override
    @Transactional
    public InterviewQuestionGenerateResponse generate(InterviewQuestionGenerateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Resume resume = getUserResumeOrThrow(request.getResumeId(), currentUserId);
        JobDescription job = getUserJobOrThrow(request.getJobId(), currentUserId);

        AnalysisReport analysisReport = null;
        if (request.getAnalysisReportId() != null) {
            analysisReport = getUserAnalysisReportOrThrow(request.getAnalysisReportId(), currentUserId);
        }

        if (!Boolean.TRUE.equals(request.getForceRefresh())) {
            InterviewQuestionReport existing = findExistingReport(
                    currentUserId,
                    request.getResumeId(),
                    request.getJobId(),
                    request.getAnalysisReportId()
            );

            if (existing != null) {
                return toGenerateResponse(existing);
            }
        }

        if (!StringUtils.hasText(resume.getParsedText())) {
            throw new BusinessException("简历解析文本为空，无法生成面试题");
        }

        if (!StringUtils.hasText(job.getJdContent())) {
            throw new BusinessException("岗位JD为空，无法生成面试题");
        }

        String analysisReportText = buildAnalysisReportText(analysisReport);

        String prompt = PromptUtils.buildInterviewQuestionPrompt(
                resume.getParsedText(),
                job.getJdContent(),
                analysisReportText
        );

        String rawResponse = aiClient.chat(prompt);

        AiInterviewQuestionResult aiResult = JsonUtils.parseAiJson(
                rawResponse,
                AiInterviewQuestionResult.class
        );

        if (aiResult.getQuestions() == null || aiResult.getQuestions().isEmpty()) {
            throw new BusinessException("AI 未生成有效面试题");
        }

        String title = StringUtils.hasText(aiResult.getTitle())
                ? aiResult.getTitle()
                : job.getCompanyName() + " " + job.getJobTitle() + " 面试题准备";

        InterviewQuestionReport report = new InterviewQuestionReport();
        report.setUserId(currentUserId);
        report.setResumeId(resume.getId());
        report.setJobId(job.getId());
        report.setAnalysisReportId(request.getAnalysisReportId());
        report.setTitle(title);
        report.setQuestionCount(aiResult.getQuestions().size());
        report.setAiProvider(aiProperties.getProvider());
        report.setAiModel(aiProperties.getModel());
        report.setRawAiResponse(rawResponse);

        interviewQuestionReportMapper.insert(report);

        int sortOrder = 1;
        for (AiInterviewQuestionResult.QuestionItem item : aiResult.getQuestions()) {
            InterviewQuestion question = new InterviewQuestion();
            question.setReportId(report.getId());
            question.setUserId(currentUserId);
            question.setQuestionType(normalizeQuestionType(item.getQuestionType()));
            question.setDifficulty(normalizeDifficulty(item.getDifficulty()));
            question.setQuestion(item.getQuestion());
            question.setAnswer(item.getAnswer());
            question.setAnswerPoints(JsonUtils.toJsonString(nullToEmpty(item.getAnswerPoints())));
            question.setRelatedSkills(JsonUtils.toJsonString(nullToEmpty(item.getRelatedSkills())));
            question.setSortOrder(sortOrder++);

            interviewQuestionMapper.insert(question);
        }

        return toGenerateResponse(report);
    }

    @Override
    public PageResult<InterviewQuestionListResponse> list(
            Long resumeId,
            Long jobId,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<InterviewQuestionReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQuestionReport::getUserId, currentUserId)
                .eq(InterviewQuestionReport::getDeleted, 0);

        if (resumeId != null) {
            wrapper.eq(InterviewQuestionReport::getResumeId, resumeId);
        }

        if (jobId != null) {
            wrapper.eq(InterviewQuestionReport::getJobId, jobId);
        }

        wrapper.orderByDesc(InterviewQuestionReport::getCreatedAt);

        Page<InterviewQuestionReport> page = new Page<>(pageNum, pageSize);
        Page<InterviewQuestionReport> resultPage = interviewQuestionReportMapper.selectPage(page, wrapper);

        List<InterviewQuestionListResponse> records = resultPage.getRecords()
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
    public InterviewQuestionDetailResponse getDetail(Long reportId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        InterviewQuestionReport report = getUserQuestionReportOrThrow(reportId, currentUserId);

        List<InterviewQuestion> questions = interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getReportId, report.getId())
                        .eq(InterviewQuestion::getUserId, currentUserId)
                        .eq(InterviewQuestion::getDeleted, 0)
                        .orderByAsc(InterviewQuestion::getSortOrder)
        );

        return toDetailResponse(report, questions);
    }

    @Override
    @Transactional
    public Boolean delete(Long reportId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        InterviewQuestionReport report = getUserQuestionReportOrThrow(reportId, currentUserId);

        report.setDeleted(1);
        interviewQuestionReportMapper.updateById(report);

        List<InterviewQuestion> questions = interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getReportId, report.getId())
                        .eq(InterviewQuestion::getUserId, currentUserId)
                        .eq(InterviewQuestion::getDeleted, 0)
        );

        for (InterviewQuestion question : questions) {
            question.setDeleted(1);
            interviewQuestionMapper.updateById(question);
        }

        return true;
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

    private AnalysisReport getUserAnalysisReportOrThrow(Long reportId, Long userId) {
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

    private InterviewQuestionReport getUserQuestionReportOrThrow(Long reportId, Long userId) {
        InterviewQuestionReport report = interviewQuestionReportMapper.selectOne(
                new LambdaQueryWrapper<InterviewQuestionReport>()
                        .eq(InterviewQuestionReport::getId, reportId)
                        .eq(InterviewQuestionReport::getUserId, userId)
                        .eq(InterviewQuestionReport::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (report == null) {
            throw new BusinessException("面试题报告不存在或无权限访问");
        }

        return report;
    }

    private InterviewQuestionReport findExistingReport(
            Long userId,
            Long resumeId,
            Long jobId,
            Long analysisReportId
    ) {
        LambdaQueryWrapper<InterviewQuestionReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQuestionReport::getUserId, userId)
                .eq(InterviewQuestionReport::getResumeId, resumeId)
                .eq(InterviewQuestionReport::getJobId, jobId)
                .eq(InterviewQuestionReport::getDeleted, 0);

        if (analysisReportId != null) {
            wrapper.eq(InterviewQuestionReport::getAnalysisReportId, analysisReportId);
        } else {
            wrapper.isNull(InterviewQuestionReport::getAnalysisReportId);
        }

        wrapper.orderByDesc(InterviewQuestionReport::getCreatedAt)
                .last("LIMIT 1");

        return interviewQuestionReportMapper.selectOne(wrapper);
    }

    private String buildAnalysisReportText(AnalysisReport report) {
        if (report == null) {
            return "暂无 AI 匹配分析报告，请主要根据简历和岗位 JD 生成面试题。";
        }

        return """
                匹配分数：%s
                匹配等级：%s
                简历优势：%s
                简历短板：%s
                缺失技能：%s
                优化建议：%s
                面试准备建议：%s
                """.formatted(
                report.getMatchScore(),
                report.getMatchLevel(),
                report.getStrengths(),
                report.getWeaknesses(),
                report.getMissingSkills(),
                report.getSuggestions(),
                report.getInterviewTips()
        );
    }

    private String normalizeQuestionType(String type) {
        if (InterviewQuestionTypeEnum.isValid(type)) {
            return type;
        }
        return InterviewQuestionTypeEnum.JOB_SKILL.getCode();
    }

    private String normalizeDifficulty(String difficulty) {
        if (QuestionDifficultyEnum.isValid(difficulty)) {
            return difficulty;
        }
        return QuestionDifficultyEnum.MEDIUM.getCode();
    }

    private List<String> nullToEmpty(List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private InterviewQuestionGenerateResponse toGenerateResponse(InterviewQuestionReport report) {
        InterviewQuestionGenerateResponse response = new InterviewQuestionGenerateResponse();
        response.setReportId(report.getId());
        response.setTitle(report.getTitle());
        response.setResumeId(report.getResumeId());
        response.setJobId(report.getJobId());
        response.setAnalysisReportId(report.getAnalysisReportId());
        response.setQuestionCount(report.getQuestionCount());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private InterviewQuestionListResponse toListResponse(InterviewQuestionReport report) {
        InterviewQuestionListResponse response = new InterviewQuestionListResponse();
        response.setReportId(report.getId());
        response.setTitle(report.getTitle());
        response.setResumeId(report.getResumeId());
        response.setJobId(report.getJobId());
        response.setQuestionCount(report.getQuestionCount());
        response.setCreatedAt(report.getCreatedAt());

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        return response;
    }

    private InterviewQuestionDetailResponse toDetailResponse(
            InterviewQuestionReport report,
            List<InterviewQuestion> questions
    ) {
        InterviewQuestionDetailResponse response = new InterviewQuestionDetailResponse();
        response.setReportId(report.getId());
        response.setTitle(report.getTitle());
        response.setResumeId(report.getResumeId());
        response.setJobId(report.getJobId());
        response.setAnalysisReportId(report.getAnalysisReportId());
        response.setQuestionCount(report.getQuestionCount());
        response.setCreatedAt(report.getCreatedAt());

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        List<InterviewQuestionItemResponse> items = questions.stream()
                .filter(Objects::nonNull)
                .map(this::toQuestionItemResponse)
                .toList();

        response.setQuestions(items);

        return response;
    }

    private InterviewQuestionItemResponse toQuestionItemResponse(InterviewQuestion question) {
        InterviewQuestionItemResponse response = new InterviewQuestionItemResponse();
        response.setQuestionId(question.getId());
        response.setQuestionType(question.getQuestionType());
        response.setDifficulty(question.getDifficulty());
        response.setQuestion(question.getQuestion());
        response.setAnswer(question.getAnswer());
        response.setAnswerPoints(JsonUtils.fromJsonString(question.getAnswerPoints(), List.class));
        response.setRelatedSkills(JsonUtils.fromJsonString(question.getRelatedSkills(), List.class));
        response.setSortOrder(question.getSortOrder());
        return response;
    }
}
```

## 16. Controller 设计

### 16.1 InterviewQuestionController

路径：

src/main/java/com/internpilot/controller/InterviewQuestionController.java

```java
package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.service.InterviewQuestionService;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 面试题生成接口")
@RestController
@RequestMapping("/api/interview-questions")
@RequiredArgsConstructor
public class InterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @Operation(summary = "生成 AI 面试题", description = "根据简历、岗位和分析报告生成岗位定制化面试题")
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping("/generate")
    public Result<InterviewQuestionGenerateResponse> generate(
            @RequestBody @Valid InterviewQuestionGenerateRequest request
    ) {
        return Result.success(interviewQuestionService.generate(request));
    }

    @Operation(summary = "查询面试题报告列表", description = "分页查询当前用户生成过的面试题报告")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping
    public Result<PageResult<InterviewQuestionListResponse>> list(
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(interviewQuestionService.list(resumeId, jobId, pageNum, pageSize));
    }

    @Operation(summary = "查询面试题报告详情", description = "查询某份面试题报告的完整题目和参考答案")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/{reportId}")
    public Result<InterviewQuestionDetailResponse> getDetail(@PathVariable Long reportId) {
        return Result.success(interviewQuestionService.getDetail(reportId));
    }

    @Operation(summary = "删除面试题报告", description = "逻辑删除某份面试题报告")
    @PreAuthorize("hasAuthority('analysis:delete')")
    @DeleteMapping("/{reportId}")
    public Result<Boolean> delete(@PathVariable Long reportId) {
        return Result.success(interviewQuestionService.delete(reportId));
    }
}
```

## 17. 接口设计

### 17.1 生成面试题

基本信息

项目	内容
URL	/api/interview-questions/generate
Method	POST
权限	analysis:write
Content-Type	application/json

请求参数

```json
{
  "resumeId": 1,
  "jobId": 1,
  "analysisReportId": 1,
  "forceRefresh": false
}
```

响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 1,
    "title": "腾讯 Java后端开发实习生 面试题准备",
    "resumeId": 1,
    "jobId": 1,
    "analysisReportId": 1,
    "questionCount": 18,
    "createdAt": "2026-05-07T20:00:00"
  }
}
```

### 17.2 查询面试题报告列表

基本信息

项目	内容
URL	/api/interview-questions
Method	GET
权限	analysis:read

查询参数

参数	类型	必填	默认值	说明
resumeId	Long	否	无	按简历筛选
jobId	Long	否	无	按岗位筛选
pageNum	Integer	否	1	页码
pageSize	Integer	否	10	每页数量

### 17.3 查询面试题报告详情

基本信息

项目	内容
URL	/api/interview-questions/{reportId}
Method	GET
权限	analysis:read

响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 1,
    "title": "腾讯 Java后端开发实习生 面试题准备",
    "resumeId": 1,
    "jobId": 1,
    "analysisReportId": 1,
    "companyName": "腾讯",
    "jobTitle": "Java后端开发实习生",
    "questionCount": 18,
    "createdAt": "2026-05-07T20:00:00",
    "questions": [
      {
        "questionId": 1,
        "questionType": "SPRING_SECURITY",
        "difficulty": "MEDIUM",
        "question": "请介绍 Spring Security 的过滤器链执行流程。",
        "answer": "Spring Security 通过一组过滤器完成认证与授权...",
        "answerPoints": [
          "请求会经过 SecurityFilterChain",
          "JWT 项目中会经过自定义 JwtAuthenticationFilter",
          "认证成功后将 Authentication 放入 SecurityContext"
        ],
        "relatedSkills": [
          "Spring Security",
          "JWT",
          "Filter"
        ],
        "sortOrder": 1
      }
    ]
  }
}
```

## 18. 前端页面设计

### 18.1 页面规划

新增两个页面：

AI 面试题生成页
AI 面试题报告详情页

路由建议：

/interview-questions
/interview-questions/:id

### 18.2 菜单新增

左侧菜单新增：

AI 面试题

建议放在：

AI 匹配分析
分析报告
AI 面试题
投递记录

### 18.3 页面功能

AI 面试题页面需要：

选择简历；
选择岗位；
可选选择分析报告；
点击生成；
展示生成中的 loading；
成功后跳转详情页；
列表展示历史面试题报告。

### 18.4 前端 API 封装

路径：

src/api/interviewQuestion.ts

```ts
import request from '@/utils/request'

export function generateInterviewQuestionsApi(data: any) {
  return request.post('/api/interview-questions/generate', data)
}

export function getInterviewQuestionReportsApi(params: any) {
  return request.get('/api/interview-questions', { params })
}

export function getInterviewQuestionDetailApi(reportId: number) {
  return request.get(`/api/interview-questions/${reportId}`)
}

export function deleteInterviewQuestionReportApi(reportId: number) {
  return request.delete(`/api/interview-questions/${reportId}`)
}
```

### 18.5 InterviewQuestionList.vue 核心示例

```vue
<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>AI 面试题生成</span>
        <el-button type="primary" @click="generateVisible = true">
          生成面试题
        </el-button>
      </div>
    </template>

    <el-table :data="list">
      <el-table-column prop="title" label="报告标题" />
      <el-table-column prop="companyName" label="公司" width="120" />
      <el-table-column prop="jobTitle" label="岗位" width="180" />
      <el-table-column prop="questionCount" label="题目数量" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="goDetail(row)">详情</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="generateVisible" title="生成 AI 面试题" width="520px">
    <el-form label-width="100px">
      <el-form-item label="选择简历">
        <el-select v-model="form.resumeId" style="width: 100%">
          <el-option
            v-for="item in resumes"
            :key="item.resumeId"
            :label="item.resumeName || item.originalFileName"
            :value="item.resumeId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="选择岗位">
        <el-select v-model="form.jobId" style="width: 100%">
          <el-option
            v-for="item in jobs"
            :key="item.jobId"
            :label="`${item.companyName} - ${item.jobTitle}`"
            :value="item.jobId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="重新生成">
        <el-switch v-model="form.forceRefresh" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="generateVisible = false">取消</el-button>
      <el-button type="primary" :loading="generating" @click="handleGenerate">
        生成
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getResumeListApi } from '@/api/resume'
import { getJobListApi } from '@/api/job'
import {
  generateInterviewQuestionsApi,
  getInterviewQuestionReportsApi,
  deleteInterviewQuestionReportApi
} from '@/api/interviewQuestion'

const list = ref<any[]>([])
const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const generateVisible = ref(false)
const generating = ref(false)

const form = reactive({
  resumeId: undefined as number | undefined,
  jobId: undefined as number | undefined,
  analysisReportId: undefined as number | undefined,
  forceRefresh: false
})

async function loadList() {
  const res: any = await getInterviewQuestionReportsApi({
    pageNum: 1,
    pageSize: 10
  })
  list.value = res.records
}

async function loadOptions() {
  const resumeRes: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
  resumes.value = resumeRes.records

  const jobRes: any = await getJobListApi({ pageNum: 1, pageSize: 100 })
  jobs.value = jobRes.records
}

async function handleGenerate() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }

  generating.value = true
  try {
    const res: any = await generateInterviewQuestionsApi(form)
    ElMessage.success('生成成功')
    generateVisible.value = false
    router.push(`/interview-questions/${res.reportId}`)
  } finally {
    generating.value = false
  }
}

function goDetail(row: any) {
  router.push(`/interview-questions/${row.reportId}`)
}

async function remove(row: any) {
  await deleteInterviewQuestionReportApi(row.reportId)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => {
  loadList()
  loadOptions()
})
</script>
```

### 18.6 InterviewQuestionDetail.vue 展示建议

详情页展示：

报告标题
公司名称
岗位名称
题目数量
按 questionType 分组展示
每道题支持展开参考答案
答题要点用列表展示
相关技能用 tag 展示

推荐组件：

el-collapse
el-collapse-item
el-tag
el-card
el-descriptions

## 19. 权限设计

### 19.1 后端权限

生成面试题：

@PreAuthorize("hasAuthority('analysis:write')")

查询面试题：

@PreAuthorize("hasAuthority('analysis:read')")

删除面试题：

@PreAuthorize("hasAuthority('analysis:delete')")

### 19.2 前端权限

菜单显示：

analysis:read

生成按钮：

analysis:write

删除按钮：

analysis:delete

## 20. 缓存设计

### 20.1 第一阶段策略

第一阶段不一定要接 Redis。

可以用数据库复用：

如果相同 userId + resumeId + jobId + analysisReportId 已经生成过报告
并且 forceRefresh = false
则直接返回已有报告

### 20.2 后续 Redis 缓存

Key：

internpilot:interview:questions:{userId}:{resumeId}:{jobId}:{analysisReportId}

TTL：

24 小时

## 21. 异常处理设计

### 21.1 常见异常

场景	错误信息
resumeId 为空	简历ID不能为空
jobId 为空	岗位ID不能为空
简历不存在	简历不存在或无权限访问
岗位不存在	岗位不存在或无权限访问
分析报告不存在	分析报告不存在或无权限访问
简历 parsedText 为空	简历解析文本为空，无法生成面试题
岗位 jdContent 为空	岗位JD为空，无法生成面试题
AI 返回非 JSON	AI 返回结果解析失败
AI 没有生成题目	AI 未生成有效面试题

## 22. 测试流程

### 22.1 前置条件

需要：

用户已登录
已有简历
已有岗位 JD
最好已有 AI 分析报告
AI Client 可用或 MockAiClient 可用

### 22.2 Swagger 测试

生成面试题
POST /api/interview-questions/generate

请求：

```json
{
  "resumeId": 1,
  "jobId": 1,
  "analysisReportId": 1,
  "forceRefresh": false
}
```

期望：

返回 reportId
数据库 interview_question_report 新增记录
数据库 interview_question 新增多条题目

### 22.3 查询列表

GET /api/interview-questions?pageNum=1&pageSize=10

期望：

返回面试题报告列表

### 22.4 查询详情

GET /api/interview-questions/{reportId}

期望：

返回题目列表
每道题有 question / answer / answerPoints / relatedSkills

### 22.5 删除报告

DELETE /api/interview-questions/{reportId}

期望：

报告 deleted = 1
题目 deleted = 1
列表不再显示

## 23. PowerShell 测试示例

### 23.1 生成面试题

```powershell
$body = @{
  resumeId = 1
  jobId = 1
  analysisReportId = 1
  forceRefresh = $false
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/interview-questions/generate" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body

$response
$questionReportId = $response.data.reportId
```

### 23.2 查询详情

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/interview-questions/$questionReportId" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

## 24. Mock AI 返回示例

开发初期可以让 MockAiClient 返回：

```json
{
  "title": "腾讯 Java后端开发实习生 面试题准备",
  "questions": [
    {
      "questionType": "SPRING_SECURITY",
      "difficulty": "MEDIUM",
      "question": "请介绍 Spring Security 的过滤器链执行流程。",
      "answer": "Spring Security 通过 SecurityFilterChain 中的一系列过滤器处理请求，完成认证和授权。在 JWT 项目中，通常会加入自定义 JwtAuthenticationFilter，从请求头中解析 Token，校验成功后将 Authentication 放入 SecurityContext。",
      "answerPoints": [
        "SecurityFilterChain",
        "JwtAuthenticationFilter",
        "SecurityContext",
        "认证和授权"
      ],
      "relatedSkills": [
        "Spring Security",
        "JWT",
        "Filter"
      ]
    },
    {
      "questionType": "MYSQL",
      "difficulty": "MEDIUM",
      "question": "MySQL 索引为什么能提高查询效率？",
      "answer": "MySQL 常用 B+Tree 索引，能够减少磁盘 IO，并通过有序结构快速定位数据。",
      "answerPoints": [
        "B+Tree",
        "减少 IO",
        "范围查询",
        "索引失效场景"
      ],
      "relatedSkills": [
        "MySQL",
        "索引",
        "B+Tree"
      ]
    }
  ]
}
```

## 25. 常见问题

### 25.1 AI 返回字段名不一致

例如返回：

type
question_type
questionType

解决：

Prompt 中严格规定字段名；
后端可以增加兼容解析；
或在 DTO 上使用 Jackson 注解。

### 25.2 answerPoints 解析失败

原因：

AI 可能返回字符串而不是数组。

解决：

Prompt 中强调：

answerPoints 必须是字符串数组
relatedSkills 必须是字符串数组

### 25.3 题目过多导致响应慢

解决：

限制生成 15 - 20 道题；
减少参考答案长度；
后续改成异步任务；
复用 WebSocket 进度模块。

### 25.4 面试题质量泛泛

优化 Prompt：

要求项目追问题必须结合简历中的项目
要求岗位技能题必须来自 JD
要求缺失技能题必须来自分析报告 missingSkills

## 26. 后续增强方向

### 26.1 异步生成面试题

当前第一阶段可以同步生成。

后续可以复用 WebSocket 任务模型：

interview_question_task

流程：

创建面试题生成任务
  ↓
WebSocket 推送进度
  ↓
生成完成后跳转题目报告

### 26.2 在线答题与 AI 批改

后续可以增加：

用户输入答案
  ↓
AI 批改
  ↓
给出得分
  ↓
指出遗漏点
  ↓
生成改进答案

表设计：

interview_answer_record

### 26.3 面试题收藏

增加：

favorite_question

### 26.4 导出 PDF

支持：

导出面试题报告 PDF

### 26.5 模拟面试

后续可以增加：

AI 面试官
  ↓
一题一答
  ↓
追问
  ↓
评价

## 27. 开发顺序建议

推荐按以下顺序开发：

1. 创建 interview_question_report 表；
2. 创建 interview_question 表；
3. 创建 InterviewQuestionTypeEnum；
4. 创建 QuestionDifficultyEnum；
5. 创建 InterviewQuestionReport Entity；
6. 创建 InterviewQuestion Entity；
7. 创建 Mapper；
8. 创建 AiInterviewQuestionResult；
9. 创建请求 DTO；
10. 创建响应 VO；
11. PromptUtils 增加 buildInterviewQuestionPrompt；
12. 创建 InterviewQuestionService；
13. 实现 InterviewQuestionServiceImpl；
14. 创建 InterviewQuestionController；
15. Swagger 测试生成；
16. 前端新增 api/interviewQuestion.ts；
17. 前端新增菜单；
18. 前端新增面试题列表页；
19. 前端新增面试题详情页；
20. 测试生成、查询、删除完整流程。

## 28. 验收标准

### 28.1 后端验收

可以生成面试题报告；
可以保存报告；
可以保存题目；
可以查询报告列表；
可以查询报告详情；
可以删除报告；
只能查看自己的报告；
简历不存在时返回错误；
岗位不存在时返回错误；
AI 返回异常时能处理；
权限不足返回 403。

### 28.2 前端验收

菜单中有 AI 面试题；
可以选择简历；
可以选择岗位；
可以点击生成；
生成中有 loading；
生成成功后跳转详情；
详情页按类型展示题目；
可以展开查看答案；
可以看到答题要点；
可以看到相关技能 tag。

## 29. 面试讲解准备

### 29.1 面试官问：AI 面试题模块怎么做的？

回答：

这个模块是在 AI 匹配分析之后做的能力扩展。用户选择简历和岗位后，系统会读取简历解析文本、岗位 JD，以及可选的 AI 分析报告，构造 Prompt 调用大语言模型生成岗位定制化面试题。

AI 返回结构化 JSON，包括题目类型、难度、问题、参考答案、答题要点和相关技能。后端解析后保存到 interview_question_report 和 interview_question 两张表中，前端可以查看历史面试题报告和题目详情。

### 29.2 面试官问：为什么要结合 AI 分析报告？

回答：

因为 AI 分析报告里已经有匹配分数、简历短板和缺失技能。生成面试题时结合这些信息，可以让题目更有针对性。

比如分析报告指出用户缺少 Docker 或 Redis 实战经验，那么面试题模块就可以生成对应的补强题，而不是只生成泛泛的 Java 八股题。

### 29.3 面试官问：如何保证 AI 返回格式稳定？

回答：

我在 Prompt 中明确要求模型必须返回 JSON，并固定字段名，比如 questionType、difficulty、question、answer、answerPoints 和 relatedSkills。

后端会先清洗 AI 返回内容，比如去掉 Markdown 代码块，再用 Jackson 解析成 DTO。如果解析失败，会抛出统一的 AI 服务异常，并保存 raw_ai_response 方便排查。

### 29.4 面试官问：这个模块的亮点是什么？

回答：

它把 AI 分析结果进一步转化成实际求职准备材料。系统不只是告诉用户哪里不足，还能根据简历和岗位生成具体面试题、参考答案和答题要点。

从技术上看，这个模块体现了 Prompt Engineering、结构化 JSON 输出、AI 结果落库、用户数据隔离和前后端展示能力。

## 30. 简历写法

完成该模块后，简历可以新增：

- 基于简历解析文本、岗位 JD 和 AI 匹配分析报告设计 AI 面试题生成模块，自动生成岗位定制化技术题、项目追问题、HR 题、参考答案和答题要点。

更完整写法：

- 设计并实现 AI 面试题生成模块，通过 Prompt Engineering 将简历文本、岗位 JD 和匹配分析报告输入大语言模型，生成结构化面试题 JSON，并落库保存题目类型、难度、参考答案、答题要点和相关技能。

## 31. 模块设计结论

AI 面试题生成模块是 InternPilot 在 AI 分析能力上的自然扩展。

它将原来的：

简历岗位匹配分析

进一步扩展为：

面试准备与训练

完成后，InternPilot 的核心 AI 链路变成：

上传简历
  ↓
创建岗位 JD
  ↓
AI 匹配分析
  ↓
AI 面试题生成
  ↓
投递记录与面试复盘

这会让项目更贴近真实实习求职场景，也更适合在简历和面试中展示。
