下面这份直接保存为：

```text
docs/26-job-recommendation-design.md
```

---

````markdown
# InternPilot 岗位推荐模块设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目中岗位推荐模块的设计方案，包括模块背景、功能目标、推荐策略、数据库设计、接口设计、后端实现思路、前端页面设计、测试流程、后续优化方向和面试讲解准备。

当前 InternPilot 已经支持：

```text
简历上传解析
简历版本管理
岗位 JD 管理
AI 匹配分析
AI 面试题生成
投递记录管理
RBAC 权限系统
管理员后台
系统操作日志
````

下一步增加岗位推荐模块，让系统不只是“用户手动添加岗位后分析”，而是能够基于用户简历、技能、历史分析结果和投递记录，主动推荐更适合用户的岗位。

---

# 2. 模块背景

大学生找实习时常见问题：

```text
不知道自己适合投哪些岗位
不知道哪些岗位和自己的简历更匹配
手动筛选岗位效率低
投递岗位过于随机
不知道哪些技能缺口影响匹配度
```

当前 InternPilot 已经可以做到：

```text
用户保存岗位 JD
  ↓
选择简历
  ↓
AI 匹配分析
  ↓
生成匹配分数和优化建议
```

但当前流程依赖用户自己先找到岗位。

岗位推荐模块的目标是：

```text
系统根据用户已有简历、岗位、AI 分析报告和投递记录，主动推荐更值得投递的岗位。
```

---

# 3. 第一阶段推荐策略

## 3.1 为什么不一开始做复杂算法

岗位推荐听起来可以做得很复杂，例如：

```text
协同过滤
Embedding 相似度
RAG 岗位知识库
用户画像推荐
岗位爬虫
机器学习排序模型
```

但这些开发成本较高，而且需要大量岗位数据。

当前项目第一阶段更适合做：

```text
规则推荐 + AI 分析结果复用 + 用户技能匹配
```

这样既能落地，又能在面试中讲清楚。

---

## 3.2 第一阶段推荐来源

第一阶段岗位推荐基于系统已有数据：

```text
用户上传的简历
用户创建的岗位 JD
AI 分析报告
投递记录
简历版本
```

不依赖外部爬虫。

---

## 3.3 推荐对象

第一阶段推荐的是：

```text
用户已保存岗位池中的岗位
```

也就是：

```text
用户保存了多个岗位 JD
系统自动判断哪些岗位更值得优先投递
```

后续再扩展为：

```text
公共岗位池
外部岗位导入
爬虫岗位
RAG 岗位知识库
```

---

# 4. 模块目标

岗位推荐模块需要完成以下目标：

1. 支持基于当前用户推荐岗位；
    
2. 支持选择简历或简历版本生成推荐；
    
3. 支持根据岗位类型、技能匹配度、AI 分析分数推荐；
    
4. 支持排除已投递岗位；
    
5. 支持推荐理由展示；
    
6. 支持推荐分数展示；
    
7. 支持推荐等级展示；
    
8. 支持一键发起 AI 分析；
    
9. 支持一键加入投递记录；
    
10. 支持保存推荐结果；
    
11. 支持查看推荐历史；
    
12. 支持前端推荐页展示；
    
13. 支持后续扩展为 Embedding / RAG 推荐。
    

---

# 5. 推荐模块整体流程

## 5.1 推荐主流程

```text
用户选择简历 / 简历版本
  ↓
系统读取简历内容
  ↓
系统读取用户岗位池
  ↓
过滤已删除 / 已投递岗位
  ↓
提取简历技能关键词
  ↓
提取岗位技能关键词
  ↓
计算规则匹配分
  ↓
结合已有 AI 分析报告分数
  ↓
生成推荐分数
  ↓
保存推荐结果
  ↓
前端展示推荐岗位
```

---

## 5.2 推荐结果展示

每个推荐岗位展示：

```text
公司名称
岗位名称
岗位类型
工作地点
薪资范围
推荐分数
推荐等级
匹配技能
缺失技能
推荐理由
是否已分析
是否已投递
操作：查看岗位 / 发起分析 / 加入投递
```

---

# 6. 推荐策略设计

## 6.1 推荐分数组成

推荐分数建议由多个维度组成：

|维度|权重|
|---|---|
|技能关键词匹配|40%|
|AI 分析匹配分|30%|
|岗位类型匹配|15%|
|投递状态因素|10%|
|地点 / 薪资偏好|5%|

第一阶段可以简化为：

```text
推荐分数 = 技能匹配分 * 0.5 + AI匹配分 * 0.4 + 岗位类型分 * 0.1
```

---

## 6.2 技能关键词匹配

从简历文本中提取技能关键词，例如：

```text
Java
Spring Boot
Spring Security
MySQL
Redis
Docker
Linux
Vue
JWT
MyBatis
WebSocket
```

从岗位 JD 中提取技能关键词。

然后计算：

```text
匹配技能数 / 岗位要求技能数 * 100
```

示例：

```text
岗位要求：Java、Spring Boot、MySQL、Redis、Docker
简历拥有：Java、Spring Boot、MySQL、Redis

技能匹配分 = 4 / 5 * 100 = 80
```

---

## 6.3 AI 分析分数复用

如果用户已经对某个岗位做过 AI 分析，则直接使用：

```text
analysis_report.match_score
```

作为推荐依据之一。

如果没有分析过：

```text
AI 分析分数默认使用 0
或者只用规则分数
```

第一阶段推荐：

```text
有 AI 分析报告：结合 AI 分数
无 AI 分析报告：只用规则分数
```

---

## 6.4 岗位类型匹配

可以根据简历内容判断用户倾向：

```text
Java 后端
AI 应用
算法
测试
前端
```

第一阶段可以简单使用关键词规则：

|简历关键词|用户方向|
|---|---|
|Spring Boot / MySQL / Redis|Java 后端|
|LLM / Prompt / RAG / AI|AI 应用|
|YOLO / PyTorch / 感知 / 点云|算法|
|Vue / React / TypeScript|前端|
|测试 / 自动化 / Postman|测试|

如果岗位类型与用户方向一致，加分。

---

## 6.5 投递状态因素

如果岗位已经投递过，则默认不推荐。

也可以提供参数：

```text
includeApplied = true
```

第一阶段推荐：

```text
默认排除已投递岗位
```

---

## 6.6 推荐等级设计

|推荐分数|等级|
|---|---|
|85 - 100|HIGH|
|70 - 84|MEDIUM_HIGH|
|60 - 69|MEDIUM|
|40 - 59|LOW|
|0 - 39|NOT_RECOMMENDED|

---

# 7. 推荐数据结构设计

第一阶段建议新增两张表：

```text
job_recommendation_batch
job_recommendation_item
```

含义：

```text
job_recommendation_batch：一次推荐任务
job_recommendation_item：该任务下的推荐岗位结果
```

---

# 8. 数据库设计

## 8.1 job_recommendation_batch 表

```sql
CREATE TABLE IF NOT EXISTS job_recommendation_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐批次ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    resume_version_id BIGINT DEFAULT NULL COMMENT '简历版本ID',
    title VARCHAR(200) NOT NULL COMMENT '推荐批次标题',
    job_count INT NOT NULL DEFAULT 0 COMMENT '参与推荐的岗位数量',
    recommended_count INT NOT NULL DEFAULT 0 COMMENT '推荐结果数量',
    strategy VARCHAR(100) NOT NULL DEFAULT 'RULE_BASED' COMMENT '推荐策略',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_jrb_user_id (user_id),
    KEY idx_jrb_resume_id (resume_id),
    KEY idx_jrb_resume_version_id (resume_version_id),
    KEY idx_jrb_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位推荐批次表';
```

---

## 8.2 job_recommendation_item 表

```sql
CREATE TABLE IF NOT EXISTS job_recommendation_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐项ID',
    batch_id BIGINT NOT NULL COMMENT '推荐批次ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    job_id BIGINT NOT NULL COMMENT '岗位ID',
    analysis_report_id BIGINT DEFAULT NULL COMMENT '关联AI分析报告ID',
    recommendation_score INT NOT NULL DEFAULT 0 COMMENT '推荐分数',
    recommendation_level VARCHAR(50) NOT NULL COMMENT '推荐等级',
    skill_match_score INT DEFAULT NULL COMMENT '技能匹配分',
    ai_match_score INT DEFAULT NULL COMMENT 'AI匹配分',
    job_type_score INT DEFAULT NULL COMMENT '岗位类型匹配分',
    matched_skills TEXT DEFAULT NULL COMMENT '匹配技能，JSON数组字符串',
    missing_skills TEXT DEFAULT NULL COMMENT '缺失技能，JSON数组字符串',
    reasons TEXT DEFAULT NULL COMMENT '推荐理由，JSON数组字符串',
    is_applied TINYINT NOT NULL DEFAULT 0 COMMENT '是否已投递：0否，1是',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_jri_batch_id (batch_id),
    KEY idx_jri_user_id (user_id),
    KEY idx_jri_job_id (job_id),
    KEY idx_jri_score (recommendation_score),
    KEY idx_jri_level (recommendation_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位推荐结果表';
```

---

# 9. 枚举设计

## 9.1 RecommendationLevelEnum

路径：

```text
src/main/java/com/internpilot/enums/RecommendationLevelEnum.java
```

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum RecommendationLevelEnum {

    HIGH("HIGH", "强烈推荐"),
    MEDIUM_HIGH("MEDIUM_HIGH", "较推荐"),
    MEDIUM("MEDIUM", "一般推荐"),
    LOW("LOW", "低推荐"),
    NOT_RECOMMENDED("NOT_RECOMMENDED", "不推荐");

    private final String code;
    private final String description;

    RecommendationLevelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String ofScore(Integer score) {
        if (score == null) {
            return NOT_RECOMMENDED.code;
        }

        if (score >= 85) {
            return HIGH.code;
        }

        if (score >= 70) {
            return MEDIUM_HIGH.code;
        }

        if (score >= 60) {
            return MEDIUM.code;
        }

        if (score >= 40) {
            return LOW.code;
        }

        return NOT_RECOMMENDED.code;
    }
}
```

---

## 9.2 RecommendationStrategyEnum

路径：

```text
src/main/java/com/internpilot/enums/RecommendationStrategyEnum.java
```

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum RecommendationStrategyEnum {

    RULE_BASED("RULE_BASED", "规则推荐"),
    AI_SCORE_BASED("AI_SCORE_BASED", "AI分数推荐"),
    HYBRID("HYBRID", "混合推荐"),
    EMBEDDING("EMBEDDING", "向量相似度推荐");

    private final String code;
    private final String description;

    RecommendationStrategyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

---

# 10. Entity 设计

## 10.1 JobRecommendationBatch

路径：

```text
src/main/java/com/internpilot/entity/JobRecommendationBatch.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_recommendation_batch")
public class JobRecommendationBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long resumeVersionId;

    private String title;

    private Integer jobCount;

    private Integer recommendedCount;

    private String strategy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

---

## 10.2 JobRecommendationItem

路径：

```text
src/main/java/com/internpilot/entity/JobRecommendationItem.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_recommendation_item")
public class JobRecommendationItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Long userId;

    private Long jobId;

    private Long analysisReportId;

    private Integer recommendationScore;

    private String recommendationLevel;

    private Integer skillMatchScore;

    private Integer aiMatchScore;

    private Integer jobTypeScore;

    private String matchedSkills;

    private String missingSkills;

    private String reasons;

    private Integer isApplied;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

---

# 11. Mapper 设计

## 11.1 JobRecommendationBatchMapper

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.JobRecommendationBatch;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobRecommendationBatchMapper extends BaseMapper<JobRecommendationBatch> {
}
```

---

## 11.2 JobRecommendationItemMapper

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.JobRecommendationItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobRecommendationItemMapper extends BaseMapper<JobRecommendationItem> {
}
```

---

# 12. DTO 设计

## 12.1 JobRecommendationGenerateRequest

路径：

```text
src/main/java/com/internpilot/dto/recommendation/JobRecommendationGenerateRequest.java
```

```java
package com.internpilot.dto.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "生成岗位推荐请求")
public class JobRecommendationGenerateRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @Schema(description = "简历版本ID", example = "1")
    private Long resumeVersionId;

    @Schema(description = "是否包含已投递岗位", example = "false")
    private Boolean includeApplied = false;

    @Schema(description = "最多推荐数量", example = "10")
    private Integer limit = 10;
}
```

---

# 13. VO 设计

## 13.1 JobRecommendationGenerateResponse

路径：

```text
src/main/java/com/internpilot/vo/recommendation/JobRecommendationGenerateResponse.java
```

```java
package com.internpilot.vo.recommendation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobRecommendationGenerateResponse {

    private Long batchId;

    private Long resumeId;

    private Long resumeVersionId;

    private String title;

    private Integer jobCount;

    private Integer recommendedCount;

    private String strategy;

    private LocalDateTime createdAt;
}
```

---

## 13.2 JobRecommendationItemResponse

路径：

```text
src/main/java/com/internpilot/vo/recommendation/JobRecommendationItemResponse.java
```

```java
package com.internpilot.vo.recommendation;

import lombok.Data;

import java.util.List;

@Data
public class JobRecommendationItemResponse {

    private Long itemId;

    private Long jobId;

    private Long analysisReportId;

    private String companyName;

    private String jobTitle;

    private String jobType;

    private String location;

    private String salaryRange;

    private String sourcePlatform;

    private Integer recommendationScore;

    private String recommendationLevel;

    private Integer skillMatchScore;

    private Integer aiMatchScore;

    private Integer jobTypeScore;

    private List<String> matchedSkills;

    private List<String> missingSkills;

    private List<String> reasons;

    private Integer isApplied;

    private Integer sortOrder;
}
```

---

## 13.3 JobRecommendationBatchDetailResponse

路径：

```text
src/main/java/com/internpilot/vo/recommendation/JobRecommendationBatchDetailResponse.java
```

```java
package com.internpilot.vo.recommendation;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobRecommendationBatchDetailResponse {

    private Long batchId;

    private Long resumeId;

    private Long resumeVersionId;

    private String title;

    private Integer jobCount;

    private Integer recommendedCount;

    private String strategy;

    private LocalDateTime createdAt;

    private List<JobRecommendationItemResponse> items;
}
```

---

## 13.4 JobRecommendationBatchListResponse

路径：

```text
src/main/java/com/internpilot/vo/recommendation/JobRecommendationBatchListResponse.java
```

```java
package com.internpilot.vo.recommendation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobRecommendationBatchListResponse {

    private Long batchId;

    private Long resumeId;

    private Long resumeVersionId;

    private String title;

    private Integer jobCount;

    private Integer recommendedCount;

    private String strategy;

    private LocalDateTime createdAt;
}
```

---

# 14. 技能关键词工具设计

## 14.1 SkillKeywordUtils

路径：

```text
src/main/java/com/internpilot/util/SkillKeywordUtils.java
```

```java
package com.internpilot.util;

import java.util.ArrayList;
import java.util.List;

public class SkillKeywordUtils {

    private static final List<String> SKILL_KEYWORDS = List.of(
            "Java",
            "Spring Boot",
            "Spring Security",
            "Spring MVC",
            "MyBatis",
            "MyBatis Plus",
            "MySQL",
            "Redis",
            "JWT",
            "Docker",
            "Linux",
            "Git",
            "Vue",
            "TypeScript",
            "WebSocket",
            "RabbitMQ",
            "Kafka",
            "Nginx",
            "JUnit",
            "Mockito",
            "RESTful",
            "Swagger",
            "Knife4j",
            "Python",
            "PyTorch",
            "OpenCV",
            "YOLO",
            "LLM",
            "Prompt",
            "RAG",
            "向量数据库",
            "Embedding"
    );

    public static List<String> extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String lowerText = text.toLowerCase();
        List<String> result = new ArrayList<>();

        for (String keyword : SKILL_KEYWORDS) {
            if (lowerText.contains(keyword.toLowerCase())) {
                result.add(keyword);
            }
        }

        return result;
    }

    public static List<String> matchedSkills(List<String> resumeSkills, List<String> jobSkills) {
        return jobSkills.stream()
                .filter(resumeSkills::contains)
                .toList();
    }

    public static List<String> missingSkills(List<String> resumeSkills, List<String> jobSkills) {
        return jobSkills.stream()
                .filter(skill -> !resumeSkills.contains(skill))
                .toList();
    }

    public static int calculateSkillScore(List<String> matchedSkills, List<String> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) {
            return 60;
        }

        return (int) Math.round(matchedSkills.size() * 100.0 / jobSkills.size());
    }
}
```

---

# 15. 推荐分数计算设计

## 15.1 RecommendationScoreCalculator

路径：

```text
src/main/java/com/internpilot/util/RecommendationScoreCalculator.java
```

```java
package com.internpilot.util;

public class RecommendationScoreCalculator {

    public static int calculateFinalScore(
            Integer skillMatchScore,
            Integer aiMatchScore,
            Integer jobTypeScore
    ) {
        int skill = skillMatchScore == null ? 0 : skillMatchScore;
        int ai = aiMatchScore == null ? 0 : aiMatchScore;
        int jobType = jobTypeScore == null ? 60 : jobTypeScore;

        if (ai > 0) {
            return clamp((int) Math.round(skill * 0.5 + ai * 0.4 + jobType * 0.1));
        }

        return clamp((int) Math.round(skill * 0.8 + jobType * 0.2));
    }

    private static int clamp(int score) {
        if (score < 0) {
            return 0;
        }

        if (score > 100) {
            return 100;
        }

        return score;
    }
}
```

---

# 16. 推荐理由生成设计

## 16.1 推荐理由示例

推荐理由可以基于规则生成：

```text
你的简历中包含 Java、Spring Boot、MySQL，与该岗位核心要求匹配度较高。
该岗位要求 Redis，你的简历中已有相关项目经验。
你曾对该岗位进行 AI 分析，匹配分数为 82。
该岗位尚未投递，可以优先考虑。
```

---

## 16.2 推荐理由生成方法

```java
private List<String> buildReasons(
        List<String> matchedSkills,
        List<String> missingSkills,
        Integer aiScore,
        Integer isApplied
) {
    List<String> reasons = new ArrayList<>();

    if (!matchedSkills.isEmpty()) {
        reasons.add("你的简历中包含 " + String.join("、", matchedSkills) + "，与岗位要求较匹配。");
    }

    if (!missingSkills.isEmpty()) {
        reasons.add("该岗位还要求 " + String.join("、", missingSkills) + "，建议投递前重点补强。");
    }

    if (aiScore != null && aiScore > 0) {
        reasons.add("该岗位已有 AI 分析记录，匹配分数为 " + aiScore + "。");
    }

    if (isApplied != null && isApplied == 0) {
        reasons.add("该岗位尚未投递，可以作为优先候选。");
    }

    return reasons;
}
```

---

# 17. Service 设计

## 17.1 JobRecommendationService

路径：

```text
src/main/java/com/internpilot/service/JobRecommendationService.java
```

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.recommendation.JobRecommendationGenerateRequest;
import com.internpilot.vo.recommendation.JobRecommendationBatchDetailResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchListResponse;
import com.internpilot.vo.recommendation.JobRecommendationGenerateResponse;

public interface JobRecommendationService {

    JobRecommendationGenerateResponse generate(JobRecommendationGenerateRequest request);

    PageResult<JobRecommendationBatchListResponse> list(Integer pageNum, Integer pageSize);

    JobRecommendationBatchDetailResponse getDetail(Long batchId);

    Boolean delete(Long batchId);
}
```

---

## 17.2 Service 实现核心思路

```text
获取当前用户
  ↓
校验 resumeId 属于当前用户
  ↓
如果传入 resumeVersionId，校验版本属于当前用户
  ↓
读取简历内容
  ↓
查询当前用户所有岗位
  ↓
查询已投递岗位集合
  ↓
遍历岗位计算推荐分数
  ↓
排序取前 limit 个
  ↓
保存 batch
  ↓
保存 item
  ↓
返回 batchId
```

---

# 18. Controller 设计

## 18.1 JobRecommendationController

路径：

```text
src/main/java/com/internpilot/controller/JobRecommendationController.java
```

```java
package com.internpilot.controller;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.recommendation.JobRecommendationGenerateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.JobRecommendationService;
import com.internpilot.vo.recommendation.JobRecommendationBatchDetailResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchListResponse;
import com.internpilot.vo.recommendation.JobRecommendationGenerateResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-recommendations")
@RequiredArgsConstructor
public class JobRecommendationController {

    private final JobRecommendationService jobRecommendationService;

    @Operation(summary = "生成岗位推荐")
    @OperationLog(module = "岗位推荐", operation = "生成岗位推荐", type = OperationTypeEnum.AI, recordParams = false)
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping("/generate")
    public Result<JobRecommendationGenerateResponse> generate(
            @RequestBody @Valid JobRecommendationGenerateRequest request
    ) {
        return Result.success(jobRecommendationService.generate(request));
    }

    @Operation(summary = "查询岗位推荐历史")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping
    public Result<PageResult<JobRecommendationBatchListResponse>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(jobRecommendationService.list(pageNum, pageSize));
    }

    @Operation(summary = "查询岗位推荐详情")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/{batchId}")
    public Result<JobRecommendationBatchDetailResponse> getDetail(@PathVariable Long batchId) {
        return Result.success(jobRecommendationService.getDetail(batchId));
    }

    @Operation(summary = "删除岗位推荐记录")
    @OperationLog(module = "岗位推荐", operation = "删除岗位推荐记录", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('analysis:delete')")
    @DeleteMapping("/{batchId}")
    public Result<Boolean> delete(@PathVariable Long batchId) {
        return Result.success(jobRecommendationService.delete(batchId));
    }
}
```

---

# 19. 接口设计

## 19.1 生成岗位推荐

```text
POST /api/job-recommendations/generate
```

请求：

```json
{
  "resumeId": 1,
  "resumeVersionId": 2,
  "includeApplied": false,
  "limit": 10
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "batchId": 1,
    "resumeId": 1,
    "resumeVersionId": 2,
    "title": "基于 Java后端实习简历 的岗位推荐",
    "jobCount": 12,
    "recommendedCount": 8,
    "strategy": "RULE_BASED",
    "createdAt": "2026-05-08T12:00:00"
  }
}
```

---

## 19.2 查询推荐历史

```text
GET /api/job-recommendations?pageNum=1&pageSize=10
```

---

## 19.3 查询推荐详情

```text
GET /api/job-recommendations/{batchId}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "batchId": 1,
    "resumeId": 1,
    "resumeVersionId": 2,
    "title": "基于 Java后端实习简历 的岗位推荐",
    "jobCount": 12,
    "recommendedCount": 8,
    "strategy": "RULE_BASED",
    "createdAt": "2026-05-08T12:00:00",
    "items": [
      {
        "itemId": 1,
        "jobId": 3,
        "companyName": "腾讯",
        "jobTitle": "Java后端开发实习生",
        "jobType": "Java后端",
        "location": "深圳",
        "salaryRange": "200-400元/天",
        "recommendationScore": 86,
        "recommendationLevel": "HIGH",
        "skillMatchScore": 80,
        "aiMatchScore": 88,
        "jobTypeScore": 90,
        "matchedSkills": [
          "Java",
          "Spring Boot",
          "MySQL",
          "Redis"
        ],
        "missingSkills": [
          "Docker"
        ],
        "reasons": [
          "你的简历中包含 Java、Spring Boot、MySQL、Redis，与岗位要求较匹配。",
          "该岗位已有 AI 分析记录，匹配分数为 88。"
        ],
        "isApplied": 0,
        "sortOrder": 1
      }
    ]
  }
}
```

---

# 20. 前端页面设计

## 20.1 页面规划

新增页面：

```text
岗位推荐页
岗位推荐详情页
```

路由：

```text
/job-recommendations
/job-recommendations/:batchId
```

---

## 20.2 菜单设计

左侧菜单新增：

```text
岗位推荐
```

建议放在：

```text
岗位管理
岗位推荐
AI 匹配分析
分析报告
AI 面试题
投递记录
```

---

## 20.3 岗位推荐页功能

页面包含：

```text
选择简历
选择简历版本
是否包含已投递岗位
推荐数量
生成推荐按钮
推荐历史列表
```

---

## 20.4 推荐详情页功能

展示：

```text
推荐批次信息
推荐岗位卡片
推荐分数
推荐等级
匹配技能
缺失技能
推荐理由
操作按钮
```

操作按钮：

```text
查看岗位
发起 AI 分析
加入投递记录
```

---

# 21. 前端 API 封装

路径：

```text
src/api/jobRecommendation.ts
```

```ts
import request from '@/utils/request'

export function generateJobRecommendationApi(data: any) {
  return request.post('/api/job-recommendations/generate', data)
}

export function getJobRecommendationListApi(params: any) {
  return request.get('/api/job-recommendations', { params })
}

export function getJobRecommendationDetailApi(batchId: number) {
  return request.get(`/api/job-recommendations/${batchId}`)
}

export function deleteJobRecommendationApi(batchId: number) {
  return request.delete(`/api/job-recommendations/${batchId}`)
}
```

---

# 22. 前端详情展示建议

推荐岗位卡片可以这样展示：

```text
腾讯 - Java后端开发实习生
推荐分数：86
推荐等级：强烈推荐

匹配技能：
Java / Spring Boot / MySQL / Redis

缺失技能：
Docker

推荐理由：
1. 你的简历中包含 Java、Spring Boot、MySQL、Redis，与岗位要求较匹配。
2. 该岗位已有 AI 分析记录，匹配分数为 88。

操作：
查看岗位 | 发起分析 | 加入投递
```

---

# 23. 权限设计

## 23.1 后端权限

|操作|权限|
|---|---|
|生成推荐|analysis:write|
|查询推荐|analysis:read|
|删除推荐|analysis:delete|
|查看岗位详情|job:read|
|发起 AI 分析|analysis:write|
|加入投递记录|application:write|

---

## 23.2 数据权限

所有查询必须带：

```text
user_id = 当前用户ID
deleted = 0
```

不能访问他人的推荐批次。

---

# 24. 与 AI 分析模块的关系

岗位推荐模块可以作为 AI 分析的前置入口。

推荐详情页中每个岗位提供：

```text
发起 AI 分析
```

流程：

```text
用户看到推荐岗位
  ↓
点击发起 AI 分析
  ↓
跳转 AI 分析页
  ↓
自动填充 resumeId / resumeVersionId / jobId
  ↓
执行 WebSocket AI 分析任务
```

---

# 25. 与投递记录模块的关系

推荐详情页中每个岗位提供：

```text
加入投递
```

流程：

```text
用户看到推荐岗位
  ↓
点击加入投递
  ↓
创建 application_record
  ↓
status = TO_APPLY
  ↓
跳转投递记录页
```

如果岗位已经投递：

```text
按钮显示：已投递
不可重复创建
```

---

# 26. 与简历版本模块的关系

推荐可以基于：

```text
resume.parsedText
```

也可以基于：

```text
resume_version.content
```

推荐优先级：

```text
如果传入 resumeVersionId：
    使用指定版本内容
否则：
    使用当前版本
如果没有当前版本：
    使用 resume.parsedText
```

---

# 27. 测试流程

## 27.1 前置条件

需要准备：

```text
用户已登录
至少 1 份简历
至少 1 个简历版本
至少 3 个岗位 JD
最好已有部分 AI 分析报告
```

---

## 27.2 生成岗位推荐测试

```http
POST /api/job-recommendations/generate
```

请求：

```json
{
  "resumeId": 1,
  "resumeVersionId": 1,
  "includeApplied": false,
  "limit": 10
}
```

期望：

```text
返回 batchId
job_recommendation_batch 新增记录
job_recommendation_item 新增多条记录
推荐分数从高到低排序
```

---

## 27.3 查询推荐详情测试

```http
GET /api/job-recommendations/{batchId}
```

期望：

```text
返回推荐岗位列表
包含推荐分数
包含推荐理由
包含匹配技能和缺失技能
```

---

## 27.4 已投递过滤测试

准备一个已投递岗位，然后生成推荐：

```json
{
  "resumeId": 1,
  "includeApplied": false
}
```

期望：

```text
已投递岗位不出现在推荐结果中
```

---

## 27.5 越权访问测试

用户 A 生成推荐，用户 B 访问：

```http
GET /api/job-recommendations/{batchId}
```

期望：

```text
返回 400 / 403
提示推荐记录不存在或无权限访问
```

---

# 28. 常见问题

## 28.1 推荐结果不准怎么办？

第一阶段是规则推荐，不会非常智能。

优化方向：

```text
增加更多技能关键词
结合 AI 分析分数
结合用户投递结果
引入 Embedding 相似度
接入 RAG 岗位知识库
```

---

## 28.2 岗位技能提取不准怎么办？

第一阶段用关键词匹配。

后续可以改为：

```text
AI 提取岗位技能
结构化保存 job_skills
Embedding 语义匹配
```

---

## 28.3 没有岗位数据怎么办？

第一阶段推荐基于用户已保存岗位。

如果用户没有岗位：

```text
提示用户先添加岗位 JD
```

后续可以做：

```text
公共岗位池
岗位导入
岗位爬虫
岗位知识库
```

---

# 29. 后续增强方向

## 29.1 AI 推荐理由生成

当前推荐理由是规则生成。

后续可以调用 AI：

```text
根据简历、岗位 JD、匹配技能、缺失技能，生成更自然的推荐理由。
```

---

## 29.2 Embedding 相似度推荐

流程：

```text
简历内容 → Embedding
岗位 JD → Embedding
计算向量相似度
按相似度排序推荐
```

需要新增：

```text
resume_embedding
job_embedding
```

或直接存向量数据库。

---

## 29.3 RAG 岗位知识库推荐

后续结合：

```text
岗位知识库
技能图谱
岗位方向说明
用户简历
```

生成更专业推荐。

---

## 29.4 推荐反馈

用户可以对推荐结果反馈：

```text
感兴趣
不感兴趣
已投递
不适合
```

后续可用于优化推荐。

---

# 30. 面试讲解准备

## 30.1 面试官问：岗位推荐模块怎么做的？

回答：

```text
我第一阶段没有直接上复杂推荐算法，而是基于项目已有数据做规则推荐。系统会读取用户简历或简历版本内容，提取技能关键词，再读取用户保存的岗位 JD，提取岗位要求技能，然后计算技能匹配分。

如果该岗位已有 AI 匹配分析报告，还会结合 analysis_report 中的 matchScore 作为推荐依据。最终按照技能匹配分、AI 分析分数和岗位类型匹配分计算推荐分数，并生成推荐理由。
```

---

## 30.2 面试官问：为什么不用协同过滤？

回答：

```text
协同过滤需要大量用户行为数据，例如很多用户的浏览、投递、收藏和反馈记录。当前项目第一阶段数据量不够，所以直接使用协同过滤效果不会好。

因此我先使用规则推荐和 AI 分析分数复用，这种方式更适合冷启动阶段。后续如果系统有更多用户行为数据，可以再引入协同过滤或学习排序模型。
```

---

## 30.3 面试官问：推荐分数怎么算？

回答：

```text
推荐分数主要由技能匹配分、AI 分析匹配分和岗位类型分组成。技能匹配分来自简历技能和岗位技能的关键词匹配；AI 分析分来自已有的 AI 匹配报告；岗位类型分判断岗位方向是否和用户简历方向一致。

如果有 AI 分析报告，就使用混合分数；如果没有，就主要依赖规则匹配分。
```

---

## 30.4 面试官问：这个推荐模块有什么不足？

回答：

```text
当前第一阶段是基于关键词和规则的推荐，优点是简单可解释，缺点是语义理解能力有限。例如“缓存”和“Redis”可能语义相关，但简单关键词匹配可能识别不到。

后续可以使用 Embedding 计算简历和岗位 JD 的语义相似度，也可以结合 RAG 岗位知识库，让推荐理由和岗位理解更准确。
```

---

# 31. 简历写法

完成该模块后，简历可以写：

```text
- 设计并实现岗位推荐模块，基于简历版本内容、岗位 JD、技能关键词匹配和历史 AI 分析分数计算推荐分数，生成推荐等级、匹配技能、缺失技能和推荐理由。
```

更完整写法：

```text
- 基于规则推荐实现岗位推荐功能，提取简历与岗位 JD 中的技术关键词，结合 AI 分析报告 matchScore 和岗位类型匹配度进行加权评分，支持推荐结果落库、历史查询和一键发起 AI 分析 / 加入投递记录。
```

---

# 32. 开发顺序建议

推荐按以下顺序开发：

```text
1. 创建 job_recommendation_batch 表；
2. 创建 job_recommendation_item 表；
3. 创建 RecommendationLevelEnum；
4. 创建 RecommendationStrategyEnum；
5. 创建 Entity 和 Mapper；
6. 创建 DTO / VO；
7. 创建 SkillKeywordUtils；
8. 创建 RecommendationScoreCalculator；
9. 创建 JobRecommendationService；
10. 实现推荐生成逻辑；
11. 实现推荐历史查询；
12. 实现推荐详情查询；
13. 实现删除推荐记录；
14. 创建 JobRecommendationController；
15. Swagger 测试生成推荐；
16. 前端新增 jobRecommendation.ts；
17. 前端新增岗位推荐页；
18. 前端新增推荐详情页；
19. 增加一键 AI 分析跳转；
20. 增加一键加入投递记录；
21. 完整联调。
```

---

# 33. 验收标准

## 33.1 后端验收

-  可以生成岗位推荐批次；
    
-  可以生成推荐结果项；
    
-  推荐结果包含推荐分数；
    
-  推荐结果包含推荐等级；
    
-  推荐结果包含匹配技能；
    
-  推荐结果包含缺失技能；
    
-  推荐结果包含推荐理由；
    
-  推荐结果按分数降序排列；
    
-  已投递岗位默认不推荐；
    
-  可以查询推荐历史；
    
-  可以查询推荐详情；
    
-  不能访问他人的推荐记录。
    

---

## 33.2 前端验收

-  菜单中有岗位推荐；
    
-  可以选择简历；
    
-  可以选择简历版本；
    
-  可以生成推荐；
    
-  可以查看推荐历史；
    
-  可以查看推荐详情；
    
-  推荐岗位卡片展示清楚；
    
-  可以一键跳转 AI 分析；
    
-  可以一键加入投递记录。
    

---

# 34. 模块设计结论

岗位推荐模块让 InternPilot 从“被动分析工具”升级为“主动求职辅助工具”。

它将原来的流程：

```text
用户自己找岗位
  ↓
手动保存岗位
  ↓
手动分析
```

升级为：

```text
用户维护简历
  ↓
系统分析岗位池
  ↓
主动推荐适合岗位
  ↓
一键 AI 分析
  ↓
一键加入投递
```

第一阶段采用：

```text
规则推荐 + 技能关键词匹配 + AI 分析分数复用
```

优点是：

```text
实现成本低
可解释性强
适合冷启动
容易面试讲清楚
```

后续可以继续升级到：

```text
Embedding 相似度推荐
RAG 岗位知识库推荐
用户行为反馈推荐
```

推荐完成本模块后，继续做：

```text
docs/27-rag-job-knowledge-base-design.md
```

也就是 RAG 岗位知识库模块。