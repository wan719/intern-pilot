# InternPilot RAG 岗位知识库模块设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目中 RAG 岗位知识库模块的设计方案，包括模块背景、功能目标、RAG 架构、知识文档管理、文本切分、Embedding、向量检索、Prompt 增强、接口设计、数据库设计、前端页面设计、测试流程、后续优化方向和面试讲解准备。

当前 InternPilot 已经具备：

```text
简历上传解析
简历版本管理
岗位 JD 管理
AI 匹配分析
AI 面试题生成
岗位推荐
投递记录管理
RBAC 权限系统
管理员后台
系统操作日志
````

RAG 岗位知识库模块的目标是让 AI 分析不只依赖用户上传的简历和岗位 JD，还能参考系统内置的岗位知识、技能要求、面试重点和学习建议，从而生成更专业、更稳定的分析结果。

---

# 2. 什么是 RAG

RAG 全称是：

```text
Retrieval-Augmented Generation
检索增强生成
```

简单理解：

```text
先检索相关知识
再把检索结果交给大模型生成答案
```

在 InternPilot 中，RAG 的作用是：

```text
用户简历 + 岗位 JD
  ↓
检索岗位知识库
  ↓
召回相关岗位知识
  ↓
拼接进 Prompt
  ↓
AI 生成更专业的匹配分析 / 面试题 / 简历优化建议
```

---

# 3. 为什么要做 RAG 岗位知识库

当前 AI 分析主要依赖：

```text
简历文本
岗位 JD
Prompt
大语言模型自身知识
```

存在几个问题：

1. 岗位 JD 有时写得很短；
    
2. AI 可能不知道某类实习岗位真实关注点；
    
3. 不同 AI 模型输出不稳定；
    
4. 缺少项目自己可控的岗位知识；
    
5. 面试建议可能偏泛泛；
    
6. 技能缺口和学习建议不够体系化。
    

加入 RAG 后，可以让系统拥有自己的岗位知识库，例如：

```text
Java 后端实习岗位能力模型
Spring Boot 面试重点
Redis 常见实习面试问题
MySQL 索引与事务核心要求
AI 应用开发实习能力要求
自动驾驶感知实习技能要求
前端实习岗位技能清单
```

这样 AI 分析会更稳定，也更像一个真实的 AI 求职助手。

---

# 4. 模块目标

RAG 岗位知识库模块需要完成以下目标：

1. 支持管理员上传岗位知识文档；
    
2. 支持录入岗位方向知识；
    
3. 支持知识文档切分为 chunk；
    
4. 支持为 chunk 生成 Embedding；
    
5. 支持根据简历和岗位 JD 检索相关知识；
    
6. 支持将检索结果拼接到 AI Prompt；
    
7. 支持增强 AI 匹配分析；
    
8. 支持增强 AI 面试题生成；
    
9. 支持增强 AI 简历优化；
    
10. 支持知识文档列表和详情管理；
    
11. 支持知识启用 / 禁用；
    
12. 支持后续接入向量数据库；
    
13. 支持管理员后台维护知识库；
    
14. 支持记录检索命中内容，方便排查。
    

---

# 5. 第一阶段范围控制

RAG 看起来很高级，但不能一开始做太重。

## 5.1 第一阶段建议实现

第一阶段做一个轻量版 RAG：

```text
知识文档管理
  ↓
文本切分
  ↓
Embedding 生成
  ↓
MySQL 存储向量 JSON
  ↓
简单余弦相似度检索
  ↓
增强 AI Prompt
```

---

## 5.2 第一阶段不建议做

第一阶段暂不做：

```text
大规模向量数据库
复杂知识图谱
自动爬虫岗位数据
多路召回
重排序模型
检索效果评估平台
多租户知识库
```

原因：

```text
这些功能成本高，容易拖慢项目进度。
当前项目优先目标是能跑通、能展示、能面试讲清楚。
```

---

## 5.3 推荐技术路线

第一阶段：

```text
MySQL + JSON 向量字段 + Java 余弦相似度
```

第二阶段：

```text
Milvus / Qdrant / Elasticsearch Vector / PostgreSQL pgvector
```

---

# 6. RAG 整体流程

## 6.1 知识入库流程

```text
管理员创建岗位知识文档
  ↓
填写标题、方向、内容
  ↓
系统按段落切分为 chunk
  ↓
调用 Embedding API 生成向量
  ↓
保存 chunk 和 embedding
  ↓
知识库可用于检索
```

---

## 6.2 检索增强流程

```text
用户发起 AI 分析
  ↓
系统读取简历版本内容
  ↓
系统读取岗位 JD
  ↓
构造检索 query
  ↓
生成 query embedding
  ↓
检索相似知识 chunk
  ↓
取 TopK 相关知识
  ↓
拼接进 Prompt
  ↓
调用大模型生成分析结果
```

---

## 6.3 在 InternPilot 中的使用位置

RAG 可增强以下模块：

|模块|RAG 作用|
|---|---|
|AI 匹配分析|提供岗位能力模型和技能要求|
|AI 面试题生成|提供岗位面试重点|
|简历 AI 优化|提供岗位简历优化方向|
|岗位推荐|提供岗位方向知识和技能匹配依据|
|学习建议|提供缺失技能学习路径|

---

# 7. 岗位知识类型设计

## 7.1 知识类型

|类型|说明|
|---|---|
|JOB_DIRECTION|岗位方向介绍|
|SKILL_REQUIREMENT|技能要求|
|INTERVIEW_POINT|面试重点|
|RESUME_ADVICE|简历优化建议|
|LEARNING_PATH|学习路线|
|PROJECT_SUGGESTION|项目建议|
|OTHER|其他|

---

## 7.2 岗位方向

建议支持：

```text
Java 后端
AI 应用开发
前端开发
软件测试
自动驾驶感知
算法实习
数据分析
运维开发
```

---

# 8. 数据库设计

第一阶段建议新增两张表：

```text
rag_knowledge_document
rag_knowledge_chunk
```

---

## 8.1 rag_knowledge_document 表

```sql
CREATE TABLE IF NOT EXISTS rag_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识文档ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    direction VARCHAR(100) NOT NULL COMMENT '岗位方向，如Java后端',
    knowledge_type VARCHAR(50) NOT NULL COMMENT '知识类型',
    content LONGTEXT NOT NULL COMMENT '原始文档内容',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文档摘要',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_rkd_direction (direction),
    KEY idx_rkd_knowledge_type (knowledge_type),
    KEY idx_rkd_enabled (enabled),
    KEY idx_rkd_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识文档表';
```

---

## 8.2 rag_knowledge_chunk 表

```sql
CREATE TABLE IF NOT EXISTS rag_knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识片段ID',
    document_id BIGINT NOT NULL COMMENT '知识文档ID',
    direction VARCHAR(100) NOT NULL COMMENT '岗位方向',
    knowledge_type VARCHAR(50) NOT NULL COMMENT '知识类型',
    chunk_index INT NOT NULL DEFAULT 0 COMMENT '片段序号',
    content TEXT NOT NULL COMMENT '片段内容',
    embedding LONGTEXT DEFAULT NULL COMMENT '向量JSON字符串',
    embedding_model VARCHAR(100) DEFAULT NULL COMMENT 'Embedding模型',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_rkc_document_id (document_id),
    KEY idx_rkc_direction (direction),
    KEY idx_rkc_knowledge_type (knowledge_type),
    KEY idx_rkc_enabled (enabled),
    KEY idx_rkc_chunk_index (chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识片段表';
```

---

# 9. 枚举设计

## 9.1 RagKnowledgeTypeEnum

路径：

```text
src/main/java/com/internpilot/enums/RagKnowledgeTypeEnum.java
```

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum RagKnowledgeTypeEnum {

    JOB_DIRECTION("JOB_DIRECTION", "岗位方向介绍"),
    SKILL_REQUIREMENT("SKILL_REQUIREMENT", "技能要求"),
    INTERVIEW_POINT("INTERVIEW_POINT", "面试重点"),
    RESUME_ADVICE("RESUME_ADVICE", "简历优化建议"),
    LEARNING_PATH("LEARNING_PATH", "学习路线"),
    PROJECT_SUGGESTION("PROJECT_SUGGESTION", "项目建议"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    RagKnowledgeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (RagKnowledgeTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
```

---

# 10. Entity 设计

## 10.1 RagKnowledgeDocument

路径：

```text
src/main/java/com/internpilot/entity/RagKnowledgeDocument.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_document")
public class RagKnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String direction;

    private String knowledgeType;

    private String content;

    private String summary;

    private Integer enabled;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

---

## 10.2 RagKnowledgeChunk

路径：

```text
src/main/java/com/internpilot/entity/RagKnowledgeChunk.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_chunk")
public class RagKnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String direction;

    private String knowledgeType;

    private Integer chunkIndex;

    private String content;

    private String embedding;

    private String embeddingModel;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

---

# 11. Mapper 设计

## 11.1 RagKnowledgeDocumentMapper

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.RagKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagKnowledgeDocumentMapper extends BaseMapper<RagKnowledgeDocument> {
}
```

---

## 11.2 RagKnowledgeChunkMapper

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.RagKnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagKnowledgeChunkMapper extends BaseMapper<RagKnowledgeChunk> {
}
```

---

# 12. DTO 设计

## 12.1 RagKnowledgeCreateRequest

路径：

```text
src/main/java/com/internpilot/dto/rag/RagKnowledgeCreateRequest.java
```

```java
package com.internpilot.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建RAG知识文档请求")
public class RagKnowledgeCreateRequest {

    @Schema(description = "标题", example = "Java后端实习岗位能力模型")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "岗位方向", example = "Java后端")
    @NotBlank(message = "岗位方向不能为空")
    private String direction;

    @Schema(description = "知识类型", example = "SKILL_REQUIREMENT")
    @NotBlank(message = "知识类型不能为空")
    private String knowledgeType;

    @Schema(description = "知识内容")
    @NotBlank(message = "知识内容不能为空")
    private String content;

    @Schema(description = "摘要")
    private String summary;
}
```

---

## 12.2 RagKnowledgeUpdateRequest

```java
package com.internpilot.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改RAG知识文档请求")
public class RagKnowledgeUpdateRequest {

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "岗位方向")
    @NotBlank(message = "岗位方向不能为空")
    private String direction;

    @Schema(description = "知识类型")
    @NotBlank(message = "知识类型不能为空")
    private String knowledgeType;

    @Schema(description = "知识内容")
    @NotBlank(message = "知识内容不能为空")
    private String content;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "是否启用")
    private Integer enabled;
}
```

---

## 12.3 RagSearchRequest

```java
package com.internpilot.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "RAG检索请求")
public class RagSearchRequest {

    @Schema(description = "检索文本")
    @NotBlank(message = "检索文本不能为空")
    private String query;

    @Schema(description = "岗位方向")
    private String direction;

    @Schema(description = "知识类型")
    private String knowledgeType;

    @Schema(description = "返回数量")
    private Integer topK = 5;
}
```

---

# 13. VO 设计

## 13.1 RagKnowledgeListResponse

```java
package com.internpilot.vo.rag;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagKnowledgeListResponse {

    private Long documentId;

    private String title;

    private String direction;

    private String knowledgeType;

    private String summary;

    private Integer enabled;

    private Integer chunkCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

---

## 13.2 RagKnowledgeDetailResponse

```java
package com.internpilot.vo.rag;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RagKnowledgeDetailResponse {

    private Long documentId;

    private String title;

    private String direction;

    private String knowledgeType;

    private String content;

    private String summary;

    private Integer enabled;

    private List<RagKnowledgeChunkResponse> chunks;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

---

## 13.3 RagKnowledgeChunkResponse

```java
package com.internpilot.vo.rag;

import lombok.Data;

@Data
public class RagKnowledgeChunkResponse {

    private Long chunkId;

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    private String embeddingModel;

    private Integer enabled;
}
```

---

## 13.4 RagSearchResultResponse

```java
package com.internpilot.vo.rag;

import lombok.Data;

@Data
public class RagSearchResultResponse {

    private Long chunkId;

    private Long documentId;

    private String title;

    private String direction;

    private String knowledgeType;

    private String content;

    private Double similarity;
}
```

---

# 14. 文本切分设计

## 14.1 为什么要切分

RAG 不适合把整篇文档全部放进 Prompt。

原因：

1. 文档太长；
    
2. Prompt 成本高；
    
3. 无关内容会干扰模型；
    
4. 检索粒度太粗。
    

所以需要切分为多个 chunk。

---

## 14.2 第一阶段切分策略

第一阶段采用简单段落切分：

```text
按空行切分
每个 chunk 控制在 300 - 800 字
太短的段落合并
太长的段落截断或二次切分
```

---

## 14.3 TextChunkUtils

路径：

```text
src/main/java/com/internpilot/util/TextChunkUtils.java
```

```java
package com.internpilot.util;

import java.util.ArrayList;
import java.util.List;

public class TextChunkUtils {

    private static final int MAX_CHUNK_LENGTH = 800;
    private static final int MIN_CHUNK_LENGTH = 100;

    public static List<String> splitToChunks(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String[] paragraphs = content.split("\\n\\s*\\n");
        List<String> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String clean = paragraph.trim();

            if (clean.isBlank()) {
                continue;
            }

            if (current.length() + clean.length() > MAX_CHUNK_LENGTH) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }

                if (clean.length() > MAX_CHUNK_LENGTH) {
                    chunks.addAll(splitLongText(clean));
                } else {
                    current.append(clean).append("\n");
                }
            } else {
                current.append(clean).append("\n");
            }
        }

        if (current.length() >= MIN_CHUNK_LENGTH) {
            chunks.add(current.toString().trim());
        } else if (current.length() > 0 && !chunks.isEmpty()) {
            int lastIndex = chunks.size() - 1;
            chunks.set(lastIndex, chunks.get(lastIndex) + "\n" + current.toString().trim());
        } else if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    private static List<String> splitLongText(String text) {
        List<String> result = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, text.length());
            result.add(text.substring(start, end));
            start = end;
        }

        return result;
    }
}
```

---

# 15. Embedding 设计

## 15.1 Embedding 接口抽象

不要把 Embedding 逻辑写死到某个模型。

建议抽象：

```text
EmbeddingClient
```

这样后续可以切换：

```text
OpenAI Embedding
Qwen Embedding
DeepSeek 可用模型
本地 Embedding 模型
Mock Embedding
```

---

## 15.2 EmbeddingClient

路径：

```text
src/main/java/com/internpilot/service/EmbeddingClient.java
```

```java
package com.internpilot.service;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embed(String text);

    String getModel();
}
```

---

## 15.3 MockEmbeddingClient

开发阶段可以先用 Mock，避免真实 API 成本。

路径：

```text
src/main/java/com/internpilot/service/impl/MockEmbeddingClient.java
```

```java
package com.internpilot.service.impl;

import com.internpilot.service.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MockEmbeddingClient implements EmbeddingClient {

    @Override
    public List<Double> embed(String text) {
        List<Double> vector = new ArrayList<>();

        int dimension = 64;
        int hash = text == null ? 0 : text.hashCode();

        for (int i = 0; i < dimension; i++) {
            double value = ((hash >> (i % 16)) & 1) == 1 ? 1.0 : 0.0;
            vector.add(value);
        }

        return vector;
    }

    @Override
    public String getModel() {
        return "mock-embedding-64";
    }
}
```

说明：

```text
MockEmbeddingClient 只能用于开发流程打通，不代表真实语义效果。
正式效果需要接入真实 Embedding 模型。
```

---

# 16. 向量工具设计

## 16.1 VectorUtils

路径：

```text
src/main/java/com/internpilot/util/VectorUtils.java
```

```java
package com.internpilot.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class VectorUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        int size = Math.min(a.size(), b.size());

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < size; i++) {
            double x = a.get(i);
            double y = b.get(i);

            dot += x * y;
            normA += x * x;
            normB += y * y;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static String toJson(List<Double> vector) {
        try {
            return OBJECT_MAPPER.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("向量序列化失败", e);
        }
    }

    public static List<Double> fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
```

---

# 17. Service 设计

## 17.1 RagKnowledgeService

路径：

```text
src/main/java/com/internpilot/service/RagKnowledgeService.java
```

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.rag.RagKnowledgeCreateRequest;
import com.internpilot.dto.rag.RagKnowledgeUpdateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
import com.internpilot.vo.rag.RagKnowledgeDetailResponse;
import com.internpilot.vo.rag.RagKnowledgeListResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;

import java.util.List;

public interface RagKnowledgeService {

    Long create(RagKnowledgeCreateRequest request);

    Boolean update(Long documentId, RagKnowledgeUpdateRequest request);

    Boolean delete(Long documentId);

    Boolean rebuildChunks(Long documentId);

    PageResult<RagKnowledgeListResponse> list(
            String direction,
            String knowledgeType,
            Integer enabled,
            Integer pageNum,
            Integer pageSize
    );

    RagKnowledgeDetailResponse getDetail(Long documentId);

    List<RagSearchResultResponse> search(RagSearchRequest request);
}
```

---

## 17.2 Service 核心实现思路

创建知识文档：

```text
校验权限
  ↓
保存 rag_knowledge_document
  ↓
切分 content 为 chunks
  ↓
每个 chunk 生成 embedding
  ↓
保存 rag_knowledge_chunk
```

检索知识：

```text
生成 query embedding
  ↓
查询启用的 chunks
  ↓
按 direction / knowledgeType 过滤
  ↓
计算 cosine similarity
  ↓
按相似度排序
  ↓
返回 TopK
```

---

# 18. RAG 检索服务设计

## 18.1 RagRetrieveService

路径：

```text
src/main/java/com/internpilot/service/RagRetrieveService.java
```

```java
package com.internpilot.service;

import com.internpilot.vo.rag.RagSearchResultResponse;

import java.util.List;

public interface RagRetrieveService {

    List<RagSearchResultResponse> retrieveForAnalysis(
            String resumeText,
            String jobDescription,
            Integer topK
    );

    String buildRagContext(List<RagSearchResultResponse> results);
}
```

---

## 18.2 检索 Query 构造

用于 AI 分析的检索 Query 可以是：

```text
目标岗位 JD + 简历技能关键词 + 岗位类型
```

示例：

```text
Java 后端开发实习生，要求 Spring Boot、MySQL、Redis、JWT。学生简历包含校园搭子匹配系统、Spring Security、MyBatis Plus。
```

---

## 18.3 RAG Context 拼接格式

```text
以下是系统检索到的岗位知识库内容，请结合这些内容进行分析：

【知识1】
方向：Java后端
类型：SKILL_REQUIREMENT
内容：Java 后端实习岗位通常关注 Java 基础、Spring Boot、数据库、Redis、接口设计和项目经验...

【知识2】
方向：Java后端
类型：INTERVIEW_POINT
内容：Java 后端面试常问 Spring IOC/AOP、事务、MySQL 索引、Redis 缓存穿透和 JWT 鉴权...
```

---

# 19. Prompt 增强设计

## 19.1 AI 匹配分析 Prompt 增强

原 Prompt：

```text
简历文本 + 岗位 JD
```

增强后：

```text
简历文本 + 岗位 JD + RAG 岗位知识
```

---

## 19.2 PromptUtils 增加 RAG 参数

```java
public static String buildAnalysisPrompt(
        String resumeText,
        String jobDescription,
        String ragContext
) {
    return """
            你是一个资深 Java 后端面试官和实习招聘导师。

            请根据【学生简历】、【目标岗位JD】和【岗位知识库参考内容】，
            分析该学生与岗位的匹配度。

            要求：
            1. 输出匹配分数；
            2. 输出匹配等级；
            3. 输出简历优势；
            4. 输出简历短板；
            5. 输出缺失技能；
            6. 输出简历优化建议；
            7. 输出面试准备建议；
            8. 必须严格返回 JSON；
            9. 不要返回 Markdown 代码块。

            【岗位知识库参考内容】
            %s

            【学生简历】
            %s

            【目标岗位JD】
            %s
            """.formatted(
            ragContext == null ? "暂无相关知识库内容。" : ragContext,
            resumeText,
            jobDescription
    );
}
```

---

## 19.3 面试题 Prompt 增强

AI 面试题生成时加入：

```text
岗位知识库中的面试重点
```

这样生成的问题会更接近真实面试。

---

# 20. Controller 设计

## 20.1 AdminRagKnowledgeController

RAG 知识库建议由管理员维护。

路径：

```text
src/main/java/com/internpilot/controller/admin/AdminRagKnowledgeController.java
```

```java
package com.internpilot.controller.admin;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.rag.RagKnowledgeCreateRequest;
import com.internpilot.dto.rag.RagKnowledgeUpdateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.RagKnowledgeService;
import com.internpilot.vo.rag.RagKnowledgeDetailResponse;
import com.internpilot.vo.rag.RagKnowledgeListResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rag/knowledge")
@RequiredArgsConstructor
public class AdminRagKnowledgeController {

    private final RagKnowledgeService ragKnowledgeService;

    @Operation(summary = "创建RAG知识文档")
    @OperationLog(module = "RAG知识库", operation = "创建知识文档", type = OperationTypeEnum.CREATE, recordParams = false)
    @PreAuthorize("hasAuthority('rag:knowledge:write')")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid RagKnowledgeCreateRequest request) {
        return Result.success(ragKnowledgeService.create(request));
    }

    @Operation(summary = "修改RAG知识文档")
    @OperationLog(module = "RAG知识库", operation = "修改知识文档", type = OperationTypeEnum.UPDATE, recordParams = false)
    @PreAuthorize("hasAuthority('rag:knowledge:write')")
    @PutMapping("/{documentId}")
    public Result<Boolean> update(
            @PathVariable Long documentId,
            @RequestBody @Valid RagKnowledgeUpdateRequest request
    ) {
        return Result.success(ragKnowledgeService.update(documentId, request));
    }

    @Operation(summary = "删除RAG知识文档")
    @OperationLog(module = "RAG知识库", operation = "删除知识文档", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('rag:knowledge:delete')")
    @DeleteMapping("/{documentId}")
    public Result<Boolean> delete(@PathVariable Long documentId) {
        return Result.success(ragKnowledgeService.delete(documentId));
    }

    @Operation(summary = "重建知识文档切片和向量")
    @OperationLog(module = "RAG知识库", operation = "重建知识向量", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('rag:knowledge:write')")
    @PostMapping("/{documentId}/rebuild")
    public Result<Boolean> rebuild(@PathVariable Long documentId) {
        return Result.success(ragKnowledgeService.rebuildChunks(documentId));
    }

    @Operation(summary = "查询RAG知识文档列表")
    @PreAuthorize("hasAuthority('rag:knowledge:read')")
    @GetMapping
    public Result<PageResult<RagKnowledgeListResponse>> list(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String knowledgeType,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(
                ragKnowledgeService.list(direction, knowledgeType, enabled, pageNum, pageSize)
        );
    }

    @Operation(summary = "查询RAG知识文档详情")
    @PreAuthorize("hasAuthority('rag:knowledge:read')")
    @GetMapping("/{documentId}")
    public Result<RagKnowledgeDetailResponse> getDetail(@PathVariable Long documentId) {
        return Result.success(ragKnowledgeService.getDetail(documentId));
    }

    @Operation(summary = "测试RAG知识检索")
    @PreAuthorize("hasAuthority('rag:knowledge:read')")
    @PostMapping("/search")
    public Result<List<RagSearchResultResponse>> search(@RequestBody @Valid RagSearchRequest request) {
        return Result.success(ragKnowledgeService.search(request));
    }
}
```

---

# 21. 权限设计

## 21.1 新增权限

```sql
INSERT INTO permission (permission_code, permission_name, resource_type, description, enabled)
VALUES
('rag:knowledge:read', '查看RAG知识库', 'RAG', '查看RAG岗位知识库文档和检索结果', 1),
('rag:knowledge:write', '编辑RAG知识库', 'RAG', '创建、修改、重建RAG知识库文档', 1),
('rag:knowledge:delete', '删除RAG知识库', 'RAG', '删除RAG知识库文档', 1);
```

---

## 21.2 给 ADMIN 分配权限

```sql
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN (
      'rag:knowledge:read',
      'rag:knowledge:write',
      'rag:knowledge:delete'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
        AND rp.deleted = 0
  );
```

---

# 22. 接口设计

## 22.1 创建知识文档

```text
POST /api/admin/rag/knowledge
```

请求：

```json
{
  "title": "Java后端实习岗位能力模型",
  "direction": "Java后端",
  "knowledgeType": "SKILL_REQUIREMENT",
  "summary": "Java后端实习岗位常见技能要求",
  "content": "Java后端实习岗位通常要求掌握Java基础、Spring Boot、MySQL、Redis、RESTful API、Git等..."
}
```

---

## 22.2 查询知识文档列表

```text
GET /api/admin/rag/knowledge?direction=Java后端&knowledgeType=SKILL_REQUIREMENT&pageNum=1&pageSize=10
```

---

## 22.3 查询知识文档详情

```text
GET /api/admin/rag/knowledge/{documentId}
```

---

## 22.4 重建知识切片

```text
POST /api/admin/rag/knowledge/{documentId}/rebuild
```

---

## 22.5 测试检索

```text
POST /api/admin/rag/knowledge/search
```

请求：

```json
{
  "query": "Java后端实习岗位 Spring Boot MySQL Redis 面试重点",
  "direction": "Java后端",
  "knowledgeType": "INTERVIEW_POINT",
  "topK": 5
}
```

---

# 23. 与 AI 分析模块集成

## 23.1 AnalysisService 改造

AI 分析前增加：

```text
RAG 检索
```

流程：

```text
读取简历内容
  ↓
读取岗位 JD
  ↓
RagRetrieveService.retrieveForAnalysis(resumeText, jobDescription, 5)
  ↓
RagRetrieveService.buildRagContext(results)
  ↓
PromptUtils.buildAnalysisPrompt(resumeText, jobDescription, ragContext)
```

---

## 23.2 是否强制启用 RAG

建议配置开关：

```yaml
rag:
  enabled: true
  top-k: 5
```

如果 RAG 检索失败，不应该影响主流程。

推荐策略：

```text
RAG 成功：使用增强 Prompt
RAG 失败：降级为普通 Prompt
```

---

# 24. application 配置设计

```yaml
rag:
  enabled: true
  top-k: 5
  embedding:
    provider: mock
    model: mock-embedding-64
```

后续真实模型：

```yaml
rag:
  enabled: true
  top-k: 5
  embedding:
    provider: openai
    model: text-embedding-3-small
```

---

# 25. 前端页面设计

## 25.1 管理员菜单

在管理员后台新增：

```text
RAG 知识库
```

路径：

```text
/admin/rag/knowledge
```

---

## 25.2 页面功能

RAG 知识库页面需要：

1. 知识文档列表；
    
2. 创建知识文档；
    
3. 修改知识文档；
    
4. 删除知识文档；
    
5. 启用 / 禁用知识；
    
6. 查看切片 chunk；
    
7. 重建切片和向量；
    
8. 测试检索。
    

---

## 25.3 表格字段

|字段|说明|
|---|---|
|title|标题|
|direction|岗位方向|
|knowledgeType|知识类型|
|summary|摘要|
|chunkCount|切片数量|
|enabled|是否启用|
|createdAt|创建时间|
|操作|详情 / 编辑 / 重建 / 删除|

---

# 26. 前端 API 封装

路径：

```text
src/api/adminRagKnowledge.ts
```

```ts
import request from '@/utils/request'

export function createRagKnowledgeApi(data: any) {
  return request.post('/api/admin/rag/knowledge', data)
}

export function updateRagKnowledgeApi(documentId: number, data: any) {
  return request.put(`/api/admin/rag/knowledge/${documentId}`, data)
}

export function deleteRagKnowledgeApi(documentId: number) {
  return request.delete(`/api/admin/rag/knowledge/${documentId}`)
}

export function rebuildRagKnowledgeApi(documentId: number) {
  return request.post(`/api/admin/rag/knowledge/${documentId}/rebuild`)
}

export function getRagKnowledgeListApi(params: any) {
  return request.get('/api/admin/rag/knowledge', { params })
}

export function getRagKnowledgeDetailApi(documentId: number) {
  return request.get(`/api/admin/rag/knowledge/${documentId}`)
}

export function searchRagKnowledgeApi(data: any) {
  return request.post('/api/admin/rag/knowledge/search', data)
}
```

---

# 27. 初始化知识样例

## 27.1 Java 后端技能要求

```text
标题：Java后端实习岗位能力模型
方向：Java后端
类型：SKILL_REQUIREMENT

内容：
Java 后端实习岗位通常关注 Java 基础、面向对象、集合、异常处理、泛型、JVM 基础等语言能力。
框架方面重点关注 Spring Boot、Spring MVC、Spring Security、MyBatis 或 MyBatis Plus。
数据库方面重点关注 MySQL 表设计、索引、事务、慢查询优化。
缓存方面常见 Redis 基础数据结构、缓存穿透、缓存击穿、缓存雪崩。
工程方面关注 Git、接口文档、RESTful API、日志、异常处理和基础部署能力。
```

---

## 27.2 Java 后端面试重点

```text
标题：Java后端实习面试重点
方向：Java后端
类型：INTERVIEW_POINT

内容：
Java 后端实习面试常问项目经历，尤其关注项目中自己负责的模块、数据库设计、接口设计和权限设计。
Spring Boot 项目中常问 IOC、AOP、自动配置、Controller-Service-Mapper 分层。
Spring Security 项目中常问过滤器链、JWT 认证流程、401 和 403 区别、RBAC 权限模型。
MySQL 常问索引、事务、隔离级别、联合索引、慢查询。
Redis 常问数据结构、缓存一致性、缓存穿透、击穿和雪崩。
```

---

## 27.3 AI 应用开发岗位能力模型

```text
标题：AI应用开发实习岗位能力模型
方向：AI应用开发
类型：SKILL_REQUIREMENT

内容：
AI 应用开发实习岗位通常关注大语言模型 API 调用、Prompt Engineering、RAG、向量检索、Embedding、工具调用和前后端集成能力。
工程方面通常要求熟悉 Python 或 Java 后端，能够将 AI 能力集成到实际业务系统中。
项目经历中，如果有 AI 总结、AI 分析、AI 推荐、AI 问答或知识库项目，会比较加分。
```

---

# 28. 测试流程

## 28.1 创建知识文档

管理员调用：

```http
POST /api/admin/rag/knowledge
```

期望：

```text
rag_knowledge_document 新增记录
rag_knowledge_chunk 自动生成多条记录
embedding 字段不为空
```

---

## 28.2 测试检索

调用：

```http
POST /api/admin/rag/knowledge/search
```

请求：

```json
{
  "query": "Spring Security JWT RBAC 权限系统 Java后端面试",
  "direction": "Java后端",
  "topK": 3
}
```

期望：

```text
返回 Java 后端、Spring Security、面试重点相关 chunk
similarity 从高到低排序
```

---

## 28.3 AI 分析增强测试

发起 AI 分析：

```text
POST /api/analysis/match
```

检查日志或调试输出：

```text
RAG context 不为空
Prompt 中包含岗位知识库参考内容
AI 分析结果更贴合岗位方向
```

---

## 28.4 权限测试

普通用户访问：

```http
GET /api/admin/rag/knowledge
```

期望：

```text
403 Forbidden
```

管理员访问成功。

---

# 29. 常见问题

## 29.1 Mock Embedding 检索效果不好

这是正常现象。

Mock Embedding 只是为了打通流程，不代表真实语义检索效果。

正式效果需要接入真实 Embedding 模型。

---

## 29.2 MySQL 存向量性能差

第一阶段数据量小可以接受。

如果知识库变大，应升级到：

```text
Milvus
Qdrant
pgvector
Elasticsearch dense_vector
```

---

## 29.3 RAG 检索内容不相关

可能原因：

1. 知识文档太短；
    
2. chunk 切分不合理；
    
3. Embedding 模型效果差；
    
4. query 构造不好；
    
5. 没有按 direction 过滤；
    
6. topK 太大导致引入噪声。
    

---

## 29.4 RAG 让 Prompt 变长

解决方案：

```text
限制 topK
限制每个 chunk 长度
只保留高相似度 chunk
压缩 RAG context
```

---

# 30. 后续增强方向

## 30.1 接入真实向量数据库

推荐优先级：

```text
pgvector：适合和 PostgreSQL 结合
Qdrant：轻量好用
Milvus：功能强大但较重
Elasticsearch：适合已有 ES 场景
```

---

## 30.2 AI 自动生成知识文档

管理员输入岗位方向：

```text
Java后端实习
```

AI 自动生成：

```text
技能要求
面试重点
简历优化建议
学习路线
项目建议
```

再由管理员确认入库。

---

## 30.3 检索效果评估

记录每次检索：

```text
query
召回 chunk
similarity
最终 AI 是否使用
用户反馈
```

后续可以评估 RAG 效果。

---

## 30.4 与岗位推荐结合

岗位推荐可以使用 RAG 增强：

```text
用户简历
  ↓
检索岗位方向知识
  ↓
判断适合方向
  ↓
推荐岗位
```

---

# 31. 面试讲解准备

## 31.1 面试官问：你这个 RAG 是怎么做的？

回答：

```text
我在 InternPilot 中设计了一个轻量级 RAG 岗位知识库。管理员可以维护不同岗位方向的知识文档，比如 Java 后端实习岗位能力模型、面试重点和简历优化建议。

系统会把知识文档切分成 chunk，并为每个 chunk 生成 Embedding。用户发起 AI 分析时，后端会根据简历和岗位 JD 构造检索 query，检索相似知识片段，并把召回内容拼接到 Prompt 中，让大模型结合岗位知识库生成更专业的分析结果。
```

---

## 31.2 面试官问：为什么不用向量数据库？

回答：

```text
第一阶段项目数据量比较小，我先用 MySQL 存储 chunk 和 embedding JSON，然后在 Java 中计算余弦相似度，这样可以快速打通 RAG 流程，降低项目复杂度。

如果知识库规模扩大，MySQL 扫描向量性能会不够，我会升级为 pgvector、Qdrant 或 Milvus 这类专业向量数据库。
```

---

## 31.3 面试官问：RAG 对 AI 分析有什么帮助？

回答：

```text
没有 RAG 时，AI 分析主要依赖简历和岗位 JD，如果 JD 写得很短，分析容易泛泛。加入 RAG 后，系统可以检索到岗位知识库中的技能要求、面试重点和简历优化建议，让 AI 输出更贴近岗位实际要求。

比如 Java 后端岗位，RAG 可以补充 Spring Security、MySQL、Redis、RBAC、接口设计等实习面试重点。
```

---

## 31.4 面试官问：怎么处理 RAG 检索失败？

回答：

```text
RAG 是增强能力，不应该影响主流程。所以我会把 RAG 设计成可降级。如果检索失败或没有召回内容，系统就使用普通 Prompt 继续完成 AI 分析，而不是直接报错。
```

---

# 32. 简历写法

完成该模块后，简历可以写：

```text
- 设计并实现轻量级 RAG 岗位知识库，支持岗位知识文档管理、文本切分、Embedding 生成、相似度检索和 Prompt 增强，用于提升 AI 简历岗位匹配分析与面试题生成质量。
```

更完整写法：

```text
- 基于 RAG 架构构建岗位知识库模块，管理员可维护 Java 后端、AI 应用等岗位方向知识，系统将文档切分为 chunk 并生成 Embedding，AI 分析时检索 TopK 相关知识片段拼接到 Prompt 中，增强匹配分析、简历优化和面试题生成的专业性。
```

---

# 33. 开发顺序建议

推荐按以下顺序开发：

```text
1. 创建 rag_knowledge_document 表；
2. 创建 rag_knowledge_chunk 表；
3. 创建 RagKnowledgeTypeEnum；
4. 创建 Entity 和 Mapper；
5. 创建 DTO / VO；
6. 创建 TextChunkUtils；
7. 创建 EmbeddingClient；
8. 创建 MockEmbeddingClient；
9. 创建 VectorUtils；
10. 创建 RagKnowledgeService；
11. 实现创建知识文档和自动切分；
12. 实现重建 chunk；
13. 实现检索 search；
14. 创建 AdminRagKnowledgeController；
15. 新增 RBAC 权限；
16. 初始化 Java 后端岗位知识样例；
17. 改造 AnalysisService 接入 RagRetrieveService；
18. 改造面试题生成接入 RAG context；
19. 前端新增 RAG 知识库管理页；
20. 测试检索和 AI 分析增强效果。
```

---

# 34. 验收标准

## 34.1 后端验收

-  可以创建 RAG 知识文档；
    
-  创建后自动生成 chunk；
    
-  chunk 中 embedding 不为空；
    
-  可以查询知识文档列表；
    
-  可以查询知识文档详情；
    
-  可以重建 chunk 和 embedding；
    
-  可以根据 query 检索 TopK chunk；
    
-  检索结果包含 similarity；
    
-  AI 分析可以使用 RAG context；
    
-  RAG 失败时 AI 分析可降级；
    
-  普通用户不能访问 RAG 管理接口；
    
-  管理员可以访问 RAG 管理接口。
    

---

## 34.2 前端验收

-  管理员菜单中有 RAG 知识库；
    
-  可以查看知识文档列表；
    
-  可以创建知识文档；
    
-  可以编辑知识文档；
    
-  可以查看 chunk 列表；
    
-  可以重建向量；
    
-  可以测试检索；
    
-  普通用户看不到 RAG 管理菜单。
    

---

# 35. 模块设计结论

RAG 岗位知识库模块让 InternPilot 从“普通 AI 调用项目”升级为“有知识增强能力的 AI 应用项目”。

它将原来的 AI 流程：

```text
简历 + JD + Prompt
  ↓
大模型生成
```

升级为：

```text
简历 + JD
  ↓
检索岗位知识库
  ↓
拼接 RAG Context
  ↓
大模型生成
```

第一阶段采用：

```text
MySQL 存储知识文档和 chunk
Mock / 真实 Embedding 生成向量
Java 余弦相似度检索
Prompt 增强
```

优点是：

```text
实现成本可控
RAG 链路完整
适合项目展示
适合面试讲解
后续可平滑升级向量数据库
```

完成本模块后，InternPilot 的 AI 能力链路将变成：

```text
简历解析
  ↓
岗位 JD 管理
  ↓
RAG 岗位知识检索
  ↓
AI 匹配分析
  ↓
AI 简历优化
  ↓
AI 面试题生成
  ↓
岗位推荐
  ↓
投递记录与复盘
```
