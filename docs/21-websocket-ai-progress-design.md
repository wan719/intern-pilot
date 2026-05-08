# InternPilot WebSocket AI 分析进度实时展示设计与实现文档

## 1. 文档说明

本文档用于描述 InternPilot 项目中 WebSocket AI 分析进度实时展示功能的设计与实现方案，包括业务背景、功能目标、异步任务设计、数据库设计、接口设计、WebSocket 设计、后端实现、前端实现、测试流程、异常处理和面试讲解准备。

当前 InternPilot 的 AI 分析接口已经可以完成：

```text
用户选择简历和岗位
  ↓
后端读取简历 parsedText 和岗位 jdContent
  ↓
调用 AI
  ↓
解析结果
  ↓
保存分析报告
  ↓
返回结果
```

但是当前流程通常是同步等待。用户点击“开始分析”后，需要等接口返回，前端体验不够好。

本阶段目标是将 AI 分析从“同步等待”升级为：

```text
创建 AI 分析任务
  ↓
WebSocket 实时推送进度
  ↓
前端展示进度条和当前步骤
  ↓
分析完成后展示报告
```

---

# 2. 为什么要做 WebSocket AI 进度

AI 分析属于长耗时任务，可能受到以下因素影响：

1. 简历文本较长；
2. 岗位 JD 较长；
3. AI API 响应慢；
4. 网络不稳定；
5. JSON 解析和报告保存需要时间。

如果前端只是一直 loading，用户体验较差。

加入 WebSocket 后，用户可以看到：

```text
10% 正在读取简历
30% 正在读取岗位 JD
50% 正在构造 Prompt
70% 正在调用 AI
85% 正在解析分析结果
100% 分析完成
```

这样项目展示效果更强，也更接近真实企业系统中长任务处理方式。

---

# 3. 功能目标

WebSocket AI 进度模块需要完成以下目标：

1. 支持创建 AI 分析任务；
2. 创建任务后立即返回 taskId；
3. 后端异步执行 AI 分析；
4. 前端通过 WebSocket 订阅任务进度；
5. 后端实时推送任务状态；
6. 支持进度百分比；
7. 支持当前执行步骤描述；
8. 支持任务成功；
9. 支持任务失败；
10. 支持根据 taskId 查询任务状态；
11. 支持任务完成后关联分析报告 reportId；
12. 兼容原有 `/api/analysis/match` 同步接口；
13. 保证用户只能查看自己的分析任务。

---

# 4. 当前同步接口的问题

## 4.1 当前流程

```text
POST /api/analysis/match
  ↓
后端同步执行所有逻辑
  ↓
AI 返回后响应前端
```

优点：

1. 实现简单；
2. 调用方便；
3. 适合 MVP。

缺点：

1. 前端只能 loading；
2. AI 调用慢时用户不知道进度；
3. HTTP 请求可能超时；
4. 不适合后续复杂分析流程；
5. 不方便展示“AI 正在分析”的过程。

---

## 4.2 改造后的流程

```text
POST /api/analysis/tasks
  ↓
后端创建 analysis_task
  ↓
立即返回 taskId
  ↓
后端异步执行 AI 分析
  ↓
WebSocket 推送进度
  ↓
前端根据进度更新 UI
  ↓
任务完成后返回 reportId
```

---

# 5. 整体架构设计

## 5.1 模块关系

```text
前端 AI 分析页
  ↓ HTTP
POST /api/analysis/tasks
  ↓
AnalysisTaskController
  ↓
AnalysisTaskService
  ↓
创建 analysis_task
  ↓
线程池异步执行分析
  ↓
AnalysisService / AiClient
  ↓
生成 analysis_report
  ↓
WebSocket 推送进度
  ↓
前端实时展示
```

---

## 5.2 推荐保留同步接口

不要直接删除原来的：

```text
POST /api/analysis/match
```

推荐同时保留：

```text
POST /api/analysis/match        同步分析接口
POST /api/analysis/tasks        异步分析任务接口
GET  /api/analysis/tasks/{id}   查询任务状态
WebSocket /ws/analysis          进度推送
```

原因：

1. 原同步接口方便 Swagger 测试；
2. 异步接口用于前端正式体验；
3. 兼容旧功能；
4. 避免一次改动过大。

---

# 6. 任务状态设计

## 6.1 状态枚举

AI 分析任务状态：

| 状态      | 含义   |
| ------- | ---- |
| PENDING | 等待执行 |
| RUNNING | 执行中  |
| SUCCESS | 执行成功 |
| FAILED  | 执行失败 |

---

## 6.2 进度设计

| 进度  | 阶段           |
| --- | ------------ |
| 0   | 任务已创建        |
| 10  | 正在校验用户权限     |
| 20  | 正在读取简历       |
| 30  | 正在读取岗位 JD    |
| 40  | 正在检查缓存       |
| 50  | 正在构造 Prompt  |
| 70  | 正在调用 AI      |
| 85  | 正在解析 AI 返回结果 |
| 95  | 正在保存分析报告     |
| 100 | 分析完成         |

失败时：

```text
progress 保持当前进度
status = FAILED
message = 错误原因
```

---

# 7. 数据库设计

## 7.1 analysis_task 表

```sql
CREATE TABLE IF NOT EXISTS analysis_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    job_id BIGINT NOT NULL COMMENT '岗位ID',
    report_id BIGINT DEFAULT NULL COMMENT '分析报告ID',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/RUNNING/SUCCESS/FAILED',
    progress INT NOT NULL DEFAULT 0 COMMENT '任务进度：0-100',
    message VARCHAR(500) DEFAULT NULL COMMENT '当前进度描述或错误信息',
    force_refresh TINYINT NOT NULL DEFAULT 0 COMMENT '是否强制刷新：0否，1是',
    error_message TEXT DEFAULT NULL COMMENT '失败原因',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_analysis_task_no (task_no),
    KEY idx_analysis_task_user_id (user_id),
    KEY idx_analysis_task_resume_id (resume_id),
    KEY idx_analysis_task_job_id (job_id),
    KEY idx_analysis_task_status (status),
    KEY idx_analysis_task_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析任务表';
```

---

## 7.2 字段说明

| 字段            | 说明        |
| ------------- | --------- |
| id            | 任务 ID     |
| task_no       | 对外暴露的任务编号 |
| user_id       | 任务所属用户    |
| resume_id     | 关联简历      |
| job_id        | 关联岗位      |
| report_id     | 分析完成后关联报告 |
| status        | 任务状态      |
| progress      | 当前进度      |
| message       | 当前进度说明    |
| force_refresh | 是否强制重新分析  |
| error_message | 失败原因      |
| started_at    | 开始时间      |
| finished_at   | 完成时间      |
| deleted       | 逻辑删除      |

---

## 7.3 为什么需要 task_no

不建议直接把数据库自增 id 暴露给前端作为 WebSocket 订阅标识。

推荐使用：

```text
task_no = UUID / 雪花 ID / 时间戳 + 随机串
```

示例：

```text
TASK_20260507_8f3a2c9d
```

好处：

1. 不暴露数据库自增规律；
2. 前端使用更安全；
3. 方便日志追踪；
4. 后续可用于异步任务查询。

---

# 8. 枚举设计

## 8.1 AnalysisTaskStatusEnum

路径：

```text
src/main/java/com/internpilot/enums/AnalysisTaskStatusEnum.java
```

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum AnalysisTaskStatusEnum {

    PENDING("PENDING", "等待执行"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败");

    private final String code;
    private final String description;

    AnalysisTaskStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

---

# 9. Entity 与 Mapper 设计

## 9.1 AnalysisTask Entity

路径：

```text
src/main/java/com/internpilot/entity/AnalysisTask.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_task")
public class AnalysisTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private Long userId;

    private Long resumeId;

    private Long jobId;

    private Long reportId;

    private String status;

    private Integer progress;

    private String message;

    private Integer forceRefresh;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

---

## 9.2 AnalysisTaskMapper

路径：

```text
src/main/java/com/internpilot/mapper/AnalysisTaskMapper.java
```

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.AnalysisTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisTaskMapper extends BaseMapper<AnalysisTask> {
}
```

---

# 10. DTO 与 VO 设计

## 10.1 AnalysisTaskCreateRequest

路径：

```text
src/main/java/com/internpilot/dto/analysis/AnalysisTaskCreateRequest.java
```

```java
package com.internpilot.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建AI分析任务请求")
public class AnalysisTaskCreateRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位ID不能为空")
    private Long jobId;

    @Schema(description = "是否强制重新分析", example = "false")
    private Boolean forceRefresh = false;
}
```

---

## 10.2 AnalysisTaskCreateResponse

路径：

```text
src/main/java/com/internpilot/vo/analysis/AnalysisTaskCreateResponse.java
```

```java
package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建AI分析任务响应")
public class AnalysisTaskCreateResponse {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "当前进度")
    private Integer progress;

    @Schema(description = "提示信息")
    private String message;
}
```

---

## 10.3 AnalysisTaskDetailResponse

路径：

```text
src/main/java/com/internpilot/vo/analysis/AnalysisTaskDetailResponse.java
```

```java
package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "AI分析任务详情响应")
public class AnalysisTaskDetailResponse {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "报告ID")
    private Long reportId;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "当前进度")
    private Integer progress;

    @Schema(description = "当前消息")
    private String message;

    @Schema(description = "失败原因")
    private String errorMessage;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

---

## 10.4 AnalysisProgressMessage

WebSocket 推送消息对象。

路径：

```text
src/main/java/com/internpilot/vo/analysis/AnalysisProgressMessage.java
```

```java
package com.internpilot.vo.analysis;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalysisProgressMessage {

    private String taskNo;

    private String status;

    private Integer progress;

    private String message;

    private Long reportId;

    private String errorMessage;

    private LocalDateTime time;

    public static AnalysisProgressMessage of(
            String taskNo,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisProgressMessage progressMessage = new AnalysisProgressMessage();
        progressMessage.setTaskNo(taskNo);
        progressMessage.setStatus(status);
        progressMessage.setProgress(progress);
        progressMessage.setMessage(message);
        progressMessage.setReportId(reportId);
        progressMessage.setErrorMessage(errorMessage);
        progressMessage.setTime(LocalDateTime.now());
        return progressMessage;
    }
}
```

---

# 11. WebSocket 技术选型

## 11.1 推荐方案

Spring Boot 中有两种常见方式：

### 方案 A：原生 WebSocket

使用：

```text
jakarta.websocket
```

优点：

1. 轻量；
2. 简单；
3. 容易理解。

缺点：

1. 鉴权需要自己处理；
2. 主题订阅能力弱；
3. 多用户多任务管理稍麻烦。

### 方案 B：Spring WebSocket + STOMP

使用：

```text
spring-boot-starter-websocket
STOMP
SimpMessagingTemplate
```

优点：

1. 支持订阅主题；
2. 适合任务进度推送；
3. 前端可以订阅 `/topic/analysis/{taskNo}`；
4. 后端可以用 `SimpMessagingTemplate` 推送；
5. 后续扩展更好。

推荐使用：

```text
Spring WebSocket + STOMP
```

---

# 12. 依赖配置

## 12.1 build.gradle

加入：

```groovy
implementation 'org.springframework.boot:spring-boot-starter-websocket'
```

如果前端使用 STOMP：

```bash
npm install @stomp/stompjs sockjs-client
```

---

# 13. WebSocket 配置

## 13.1 WebSocketConfig

路径：

```text
src/main/java/com/internpilot/config/WebSocketConfig.java
```

```java
package com.internpilot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/analysis")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

---

## 13.2 推送路径设计

后端推送到：

```text
/topic/analysis/{taskNo}
```

前端订阅：

```text
/topic/analysis/TASK_20260507_xxx
```

---

# 14. WebSocket 推送服务

## 14.1 AnalysisProgressPublisher

路径：

```text
src/main/java/com/internpilot/service/AnalysisProgressPublisher.java
```

```java
package com.internpilot.service;

public interface AnalysisProgressPublisher {

    void publish(
            String taskNo,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    );
}
```

---

## 14.2 AnalysisProgressPublisherImpl

路径：

```text
src/main/java/com/internpilot/service/impl/AnalysisProgressPublisherImpl.java
```

```java
package com.internpilot.service.impl;

import com.internpilot.service.AnalysisProgressPublisher;
import com.internpilot.vo.analysis.AnalysisProgressMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisProgressPublisherImpl implements AnalysisProgressPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(
            String taskNo,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisProgressMessage progressMessage = AnalysisProgressMessage.of(
                taskNo,
                status,
                progress,
                message,
                reportId,
                errorMessage
        );

        messagingTemplate.convertAndSend(
                "/topic/analysis/" + taskNo,
                progressMessage
        );
    }
}
```

---

# 15. 异步线程池设计

## 15.1 为什么需要线程池

创建任务接口不能阻塞等待 AI 分析完成。

所以流程应该是：

```text
Controller 创建任务
  ↓
提交线程池
  ↓
立即返回 taskId
  ↓
线程池后台执行 AI 分析
```

---

## 15.2 AsyncConfig

路径：

```text
src/main/java/com/internpilot/config/AsyncConfig.java
```

```java
package com.internpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean("analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
```

---

## 15.3 企业级优化方向

后续可以换成：

```text
ThreadPoolTaskExecutor
```

支持：

1. 核心线程数；
2. 最大线程数；
3. 队列容量；
4. 拒绝策略；
5. 线程名前缀。

示例：

```java
@Bean("analysisTaskExecutor")
public ThreadPoolTaskExecutor analysisTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("analysis-task-");
    executor.initialize();
    return executor;
}
```

---

# 16. Service 设计

## 16.1 AnalysisTaskService

路径：

```text
src/main/java/com/internpilot/service/AnalysisTaskService.java
```

```java
package com.internpilot.service;

import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;

public interface AnalysisTaskService {

    AnalysisTaskCreateResponse createTask(AnalysisTaskCreateRequest request);

    AnalysisTaskDetailResponse getTaskDetail(String taskNo);
}
```

---

## 16.2 AnalysisTaskServiceImpl

路径：

```text
src/main/java/com/internpilot/service/impl/AnalysisTaskServiceImpl.java
```

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.entity.AnalysisTask;
import com.internpilot.enums.AnalysisTaskStatusEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisTaskMapper;
import com.internpilot.service.AnalysisProgressPublisher;
import com.internpilot.service.AnalysisService;
import com.internpilot.service.AnalysisTaskService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class AnalysisTaskServiceImpl implements AnalysisTaskService {

    private final AnalysisTaskMapper analysisTaskMapper;

    private final AnalysisService analysisService;

    private final AnalysisProgressPublisher progressPublisher;

    @Qualifier("analysisTaskExecutor")
    private final Executor analysisTaskExecutor;

    @Override
    @Transactional
    public AnalysisTaskCreateResponse createTask(AnalysisTaskCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        AnalysisTask task = new AnalysisTask();
        task.setTaskNo(generateTaskNo());
        task.setUserId(currentUserId);
        task.setResumeId(request.getResumeId());
        task.setJobId(request.getJobId());
        task.setStatus(AnalysisTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setMessage("任务已创建");
        task.setForceRefresh(Boolean.TRUE.equals(request.getForceRefresh()) ? 1 : 0);

        analysisTaskMapper.insert(task);

        progressPublisher.publish(
                task.getTaskNo(),
                task.getStatus(),
                task.getProgress(),
                task.getMessage(),
                null,
                null
        );

        analysisTaskExecutor.execute(() -> executeTask(task.getTaskNo(), currentUserId));

        return toCreateResponse(task);
    }

    @Override
    public AnalysisTaskDetailResponse getTaskDetail(String taskNo) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        AnalysisTask task = analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getTaskNo, taskNo)
                        .eq(AnalysisTask::getUserId, currentUserId)
                        .eq(AnalysisTask::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (task == null) {
            throw new BusinessException("分析任务不存在或无权限访问");
        }

        return toDetailResponse(task);
    }

    private void executeTask(String taskNo, Long userId) {
        AnalysisTask task = analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getTaskNo, taskNo)
                        .eq(AnalysisTask::getUserId, userId)
                        .eq(AnalysisTask::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (task == null) {
            return;
        }

        try {
            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 10, "正在校验用户权限", null, null);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 20, "正在读取简历", null, null);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 30, "正在读取岗位 JD", null, null);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 40, "正在检查缓存", null, null);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 50, "正在构造 Prompt", null, null);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 70, "正在调用 AI 分析", null, null);

            AnalysisMatchRequest matchRequest = new AnalysisMatchRequest();
            matchRequest.setResumeId(task.getResumeId());
            matchRequest.setJobId(task.getJobId());
            matchRequest.setForceRefresh(task.getForceRefresh() != null && task.getForceRefresh() == 1);

            AnalysisResultResponse result = analysisService.matchForUser(matchRequest, userId);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 85, "正在解析 AI 返回结果", result.getReportId(), null);

            updateProgress(task, AnalysisTaskStatusEnum.RUNNING.getCode(), 95, "正在保存分析报告", result.getReportId(), null);

            updateProgress(task, AnalysisTaskStatusEnum.SUCCESS.getCode(), 100, "分析完成", result.getReportId(), null);

        } catch (Exception e) {
            updateProgress(
                    task,
                    AnalysisTaskStatusEnum.FAILED.getCode(),
                    task.getProgress() == null ? 0 : task.getProgress(),
                    "分析失败",
                    null,
                    e.getMessage()
            );
        }
    }

    private void updateProgress(
            AnalysisTask task,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisTask update = new AnalysisTask();
        update.setId(task.getId());
        update.setStatus(status);
        update.setProgress(progress);
        update.setMessage(message);
        update.setReportId(reportId);

        if (AnalysisTaskStatusEnum.RUNNING.getCode().equals(status) && task.getStartedAt() == null) {
            update.setStartedAt(LocalDateTime.now());
        }

        if (AnalysisTaskStatusEnum.SUCCESS.getCode().equals(status)
                || AnalysisTaskStatusEnum.FAILED.getCode().equals(status)) {
            update.setFinishedAt(LocalDateTime.now());
        }

        if (errorMessage != null) {
            update.setErrorMessage(errorMessage);
        }

        analysisTaskMapper.updateById(update);

        task.setStatus(status);
        task.setProgress(progress);
        task.setMessage(message);
        task.setReportId(reportId);

        progressPublisher.publish(
                task.getTaskNo(),
                status,
                progress,
                message,
                reportId,
                errorMessage
        );
    }

    private String generateTaskNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "TASK_" + date + "_" + random;
    }

    private AnalysisTaskCreateResponse toCreateResponse(AnalysisTask task) {
        AnalysisTaskCreateResponse response = new AnalysisTaskCreateResponse();
        response.setTaskId(task.getId());
        response.setTaskNo(task.getTaskNo());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setMessage(task.getMessage());
        return response;
    }

    private AnalysisTaskDetailResponse toDetailResponse(AnalysisTask task) {
        AnalysisTaskDetailResponse response = new AnalysisTaskDetailResponse();
        response.setTaskId(task.getId());
        response.setTaskNo(task.getTaskNo());
        response.setResumeId(task.getResumeId());
        response.setJobId(task.getJobId());
        response.setReportId(task.getReportId());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setMessage(task.getMessage());
        response.setErrorMessage(task.getErrorMessage());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }
}
```

---

# 17. 重要改造：AnalysisService 支持指定用户执行

## 17.1 为什么要改造

异步线程中无法直接从 `SecurityContextHolder` 获取当前用户。

因为原来同步接口中可以这样：

```java
Long currentUserId = SecurityUtils.getCurrentUserId();
```

但异步线程可能没有 SecurityContext。

所以需要增加一个方法：

```java
matchForUser(AnalysisMatchRequest request, Long userId)
```

---

## 17.2 AnalysisService 增加方法

```java
AnalysisResultResponse matchForUser(AnalysisMatchRequest request, Long userId);
```

完整接口：

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;

public interface AnalysisService {

    AnalysisResultResponse match(AnalysisMatchRequest request);

    AnalysisResultResponse matchForUser(AnalysisMatchRequest request, Long userId);

    PageResult<AnalysisReportListResponse> listReports(
            Long resumeId,
            Long jobId,
            Integer minScore,
            Integer pageNum,
            Integer pageSize
    );

    AnalysisReportDetailResponse getReportDetail(Long id);
}
```

---

## 17.3 AnalysisServiceImpl 改造思路

原来的：

```java
public AnalysisResultResponse match(AnalysisMatchRequest request) {
    Long currentUserId = SecurityUtils.getCurrentUserId();
    ...
}
```

改成：

```java
@Override
public AnalysisResultResponse match(AnalysisMatchRequest request) {
    Long currentUserId = SecurityUtils.getCurrentUserId();
    return matchForUser(request, currentUserId);
}

@Override
@Transactional
public AnalysisResultResponse matchForUser(AnalysisMatchRequest request, Long currentUserId) {
    // 把原 match 方法中的主体逻辑搬到这里
}
```

这样同步接口和异步任务都能复用同一套分析逻辑。

---

# 18. Controller 设计

## 18.1 AnalysisTaskController

路径：

```text
src/main/java/com/internpilot/controller/AnalysisTaskController.java
```

```java
package com.internpilot.controller;

import com.internpilot.common.Result;
import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.service.AnalysisTaskService;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 分析任务接口")
@RestController
@RequestMapping("/api/analysis/tasks")
@RequiredArgsConstructor
public class AnalysisTaskController {

    private final AnalysisTaskService analysisTaskService;

    @Operation(summary = "创建 AI 分析任务", description = "创建异步 AI 分析任务，并通过 WebSocket 推送进度")
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping
    public Result<AnalysisTaskCreateResponse> createTask(
            @RequestBody @Valid AnalysisTaskCreateRequest request
    ) {
        return Result.success(analysisTaskService.createTask(request));
    }

    @Operation(summary = "查询 AI 分析任务详情", description = "根据 taskNo 查询任务状态和进度")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/{taskNo}")
    public Result<AnalysisTaskDetailResponse> getTaskDetail(@PathVariable String taskNo) {
        return Result.success(analysisTaskService.getTaskDetail(taskNo));
    }
}
```

---

# 19. 接口设计

## 19.1 创建异步分析任务

### 基本信息

| 项目           | 内容                  |
| ------------ | ------------------- |
| URL          | /api/analysis/tasks |
| Method       | POST                |
| 权限           | analysis:write      |
| Content-Type | application/json    |

### 请求参数

```json
{
  "resumeId": 1,
  "jobId": 1,
  "forceRefresh": false
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "taskNo": "TASK_20260507123000_a1b2c3d4",
    "status": "PENDING",
    "progress": 0,
    "message": "任务已创建"
  }
}
```

---

## 19.2 查询任务详情

### 基本信息

| 项目     | 内容                           |
| ------ | ---------------------------- |
| URL    | /api/analysis/tasks/{taskNo} |
| Method | GET                          |
| 权限     | analysis:read                |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "taskNo": "TASK_20260507123000_a1b2c3d4",
    "resumeId": 1,
    "jobId": 1,
    "reportId": 3,
    "status": "SUCCESS",
    "progress": 100,
    "message": "分析完成",
    "errorMessage": null,
    "startedAt": "2026-05-07T12:30:01",
    "finishedAt": "2026-05-07T12:30:15",
    "createdAt": "2026-05-07T12:30:00"
  }
}
```

---

## 19.3 WebSocket 推送消息

订阅地址：

```text
/topic/analysis/{taskNo}
```

消息示例：

```json
{
  "taskNo": "TASK_20260507123000_a1b2c3d4",
  "status": "RUNNING",
  "progress": 70,
  "message": "正在调用 AI 分析",
  "reportId": null,
  "errorMessage": null,
  "time": "2026-05-07T12:30:08"
}
```

成功消息：

```json
{
  "taskNo": "TASK_20260507123000_a1b2c3d4",
  "status": "SUCCESS",
  "progress": 100,
  "message": "分析完成",
  "reportId": 3,
  "errorMessage": null,
  "time": "2026-05-07T12:30:15"
}
```

失败消息：

```json
{
  "taskNo": "TASK_20260507123000_a1b2c3d4",
  "status": "FAILED",
  "progress": 70,
  "message": "分析失败",
  "reportId": null,
  "errorMessage": "AI 返回结果解析失败",
  "time": "2026-05-07T12:30:15"
}
```

---

# 20. 前端依赖安装

进入前端目录：

```bash
cd frontend/intern-pilot-frontend
```

安装：

```bash
npm install @stomp/stompjs sockjs-client
```

TypeScript 类型如果报错，可以安装：

```bash
npm install -D @types/sockjs-client
```

---

# 21. 前端 API 封装

## 21.1 api/analysisTask.ts

路径：

```text
src/api/analysisTask.ts
```

```ts
import request from '@/utils/request'

export function createAnalysisTaskApi(data: any) {
  return request.post('/api/analysis/tasks', data)
}

export function getAnalysisTaskDetailApi(taskNo: string) {
  return request.get(`/api/analysis/tasks/${taskNo}`)
}
```

---

# 22. 前端 WebSocket 封装

## 22.1 utils/analysisSocket.ts

路径：

```text
src/utils/analysisSocket.ts
```

```ts
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export interface AnalysisProgressMessage {
  taskNo: string
  status: string
  progress: number
  message: string
  reportId?: number
  errorMessage?: string
  time?: string
}

export function createAnalysisSocket(
  taskNo: string,
  onMessage: (message: AnalysisProgressMessage) => void,
  onError?: (error: any) => void
) {
  const socketUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/ws/analysis`

  const client = new Client({
    webSocketFactory: () => new SockJS(socketUrl),
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/analysis/${taskNo}`, (frame) => {
        const body = JSON.parse(frame.body)
        onMessage(body)
      })
    },
    onStompError: (frame) => {
      onError?.(frame)
    },
    onWebSocketError: (event) => {
      onError?.(event)
    }
  })

  client.activate()

  return client
}
```

---

# 23. 前端 AI 分析页改造

## 23.1 原来的流程

```text
点击开始分析
  ↓
调用 /api/analysis/match
  ↓
等待返回
  ↓
展示结果
```

---

## 23.2 新流程

```text
点击开始分析
  ↓
调用 /api/analysis/tasks
  ↓
拿到 taskNo
  ↓
建立 WebSocket 订阅
  ↓
实时更新 progress
  ↓
status = SUCCESS 后跳转报告详情
```

---

## 23.3 AnalysisMatch.vue 核心改造示例

```vue
<template>
  <el-card>
    <template #header>AI 匹配分析</template>

    <el-form label-width="100px">
      <el-form-item label="选择简历">
        <el-select v-model="form.resumeId" placeholder="请选择简历" style="width: 320px">
          <el-option
            v-for="item in resumes"
            :key="item.resumeId"
            :label="item.resumeName || item.originalFileName"
            :value="item.resumeId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="选择岗位">
        <el-select v-model="form.jobId" placeholder="请选择岗位" style="width: 320px">
          <el-option
            v-for="item in jobs"
            :key="item.jobId"
            :label="`${item.companyName} - ${item.jobTitle}`"
            :value="item.jobId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="重新分析">
        <el-switch v-model="form.forceRefresh" />
      </el-form-item>

      <el-button type="primary" :loading="running" @click="handleStartTask">
        开始 AI 分析
      </el-button>
    </el-form>
  </el-card>

  <el-card v-if="task.taskNo" style="margin-top: 20px">
    <template #header>
      <div class="progress-header">
        <span>AI 分析进度</span>
        <el-tag>{{ task.status }}</el-tag>
      </div>
    </template>

    <el-progress
      :percentage="task.progress"
      :status="task.status === 'FAILED' ? 'exception' : task.status === 'SUCCESS' ? 'success' : undefined"
    />

    <p class="progress-message">{{ task.message }}</p>

    <el-alert
      v-if="task.status === 'FAILED'"
      type="error"
      :title="task.errorMessage || '分析失败'"
      show-icon
    />

    <el-button
      v-if="task.status === 'SUCCESS' && task.reportId"
      type="success"
      @click="goReportDetail"
    >
      查看分析报告
    </el-button>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Client } from '@stomp/stompjs'
import router from '@/router'

import { getResumeListApi } from '@/api/resume'
import { getJobListApi } from '@/api/job'
import { createAnalysisTaskApi } from '@/api/analysisTask'
import { createAnalysisSocket } from '@/utils/analysisSocket'

const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const running = ref(false)
let stompClient: Client | null = null

const form = reactive({
  resumeId: undefined as number | undefined,
  jobId: undefined as number | undefined,
  forceRefresh: false
})

const task = reactive({
  taskNo: '',
  status: '',
  progress: 0,
  message: '',
  reportId: undefined as number | undefined,
  errorMessage: ''
})

async function loadOptions() {
  const resumeRes: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
  resumes.value = resumeRes.records

  const jobRes: any = await getJobListApi({ pageNum: 1, pageSize: 100 })
  jobs.value = jobRes.records
}

async function handleStartTask() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }

  running.value = true

  const res: any = await createAnalysisTaskApi(form)

  task.taskNo = res.taskNo
  task.status = res.status
  task.progress = res.progress
  task.message = res.message

  connectSocket(res.taskNo)
}

function connectSocket(taskNo: string) {
  if (stompClient) {
    stompClient.deactivate()
  }

  stompClient = createAnalysisSocket(
    taskNo,
    (message) => {
      task.status = message.status
      task.progress = message.progress
      task.message = message.message
      task.reportId = message.reportId
      task.errorMessage = message.errorMessage || ''

      if (message.status === 'SUCCESS') {
        running.value = false
        ElMessage.success('AI 分析完成')
      }

      if (message.status === 'FAILED') {
        running.value = false
        ElMessage.error(message.errorMessage || 'AI 分析失败')
      }
    },
    () => {
      running.value = false
      ElMessage.error('WebSocket 连接失败')
    }
  )
}

function goReportDetail() {
  if (task.reportId) {
    router.push(`/analysis/reports/${task.reportId}`)
  }
}

onMounted(loadOptions)

onBeforeUnmount(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>

<style scoped>
.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-message {
  margin-top: 16px;
  color: #666;
}
</style>
```

---

# 24. 前端体验优化建议

## 24.1 进度展示

建议展示：

```text
进度条
当前步骤文字
状态标签
失败错误提示
完成后查看报告按钮
```

---

## 24.2 推荐 UI

使用 Element Plus：

```text
el-progress
el-steps
el-alert
el-tag
el-card
```

---

## 24.3 更好看的步骤条

可以使用：

```vue
<el-steps :active="activeStep" finish-status="success">
  <el-step title="读取简历" />
  <el-step title="读取岗位" />
  <el-step title="AI 分析" />
  <el-step title="生成报告" />
</el-steps>
```

根据进度映射：

```ts
function getActiveStep(progress: number) {
  if (progress < 30) return 1
  if (progress < 70) return 2
  if (progress < 95) return 3
  return 4
}
```

---

# 25. 权限与安全设计

## 25.1 创建任务权限

创建任务接口需要：

```java
@PreAuthorize("hasAuthority('analysis:write')")
```

---

## 25.2 查询任务权限

查询任务详情需要：

```java
@PreAuthorize("hasAuthority('analysis:read')")
```

并且查询条件必须带：

```text
task_no = ?
user_id = 当前登录用户ID
deleted = 0
```

---

## 25.3 WebSocket 订阅安全问题

当前简单方案中：

```text
/topic/analysis/{taskNo}
```

如果别人知道 taskNo，理论上可能订阅任务进度。

第一阶段由于 taskNo 是随机 UUID，风险较低。

更安全的后续方案：

```text
/topic/user/{userId}/analysis/{taskNo}
```

或者使用 Spring Security WebSocket 鉴权。

---

## 25.4 更安全的推送路径

后续可改成：

```text
/user/queue/analysis-progress
```

前端登录后订阅自己的队列。

MVP 阶段先使用：

```text
/topic/analysis/{taskNo}
```

实现简单，演示效果好。

---

# 26. 事务与异步注意事项

## 26.1 异步线程中事务

异步执行 AI 分析时，数据库更新应该独立提交。

`updateProgress()` 每次调用都应该更新数据库。

如果出现事务没有提交的问题，可以给更新方法单独加：

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

---

## 26.2 SecurityContext 不在线程中传播

异步线程中不要依赖：

```java
SecurityUtils.getCurrentUserId()
```

而应该在创建任务时获取 userId，并传入异步方法：

```java
executeTask(taskNo, currentUserId)
```

---

## 26.3 AI 分析方法复用

推荐将原同步分析逻辑抽成：

```java
matchForUser(request, userId)
```

这样同步接口和异步任务都可以复用。

---

# 27. 测试流程

## 27.1 数据库准备

执行：

```sql
SELECT * FROM analysis_task;
```

确认表存在。

---

## 27.2 后端启动

```powershell
cd backend\intern-pilot-backend
.\gradlew.bat bootRun
```

---

## 27.3 前端启动

```powershell
cd frontend\intern-pilot-frontend
npm run dev
```

---

## 27.4 创建任务接口测试

```powershell
$body = @{
  resumeId = 1
  jobId = 1
  forceRefresh = $false
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/analysis/tasks" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body
```

期望返回：

```json
{
  "taskNo": "TASK_20260507123000_a1b2c3d4",
  "status": "PENDING",
  "progress": 0
}
```

---

## 27.5 查询任务详情

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/analysis/tasks/TASK_20260507123000_a1b2c3d4" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

---

## 27.6 前端测试

进入：

```text
http://localhost:5173/analysis/match
```

点击开始分析。

观察：

```text
进度条是否变化
状态是否从 PENDING 到 RUNNING
最后是否 SUCCESS
是否出现查看报告按钮
```

---

# 28. 常见问题

## 28.1 WebSocket 连接失败

可能原因：

1. 后端没有引入 `spring-boot-starter-websocket`；
2. WebSocketConfig 没配置；
3. 前端 socket 地址错误；
4. CORS 没放行；
5. 后端端口错误。

检查浏览器 Network：

```text
ws / sockjs 请求是否成功
```

---

## 28.2 前端没有收到消息

可能原因：

1. 后端推送 topic 和前端订阅 topic 不一致；
2. taskNo 不一致；
3. 后端任务执行太快，前端还没订阅就推送完了；
4. WebSocket 连接还没建立就开始推送。

解决：

1. 前端创建任务后立即连接；
2. 后端可以在任务执行前延迟 300ms；
3. 前端也可以轮询 `GET /api/analysis/tasks/{taskNo}` 兜底。

---

## 28.3 异步任务中获取不到当前用户

原因：

```text
SecurityContext 不会自动传到新线程
```

解决：

```text
创建任务时保存 userId
异步方法参数传入 userId
不要在异步线程中调用 SecurityUtils.getCurrentUserId()
```

---

## 28.4 分析失败但前端一直 loading

原因：

1. 后端 catch 后没有推送 FAILED；
2. 前端没有处理 FAILED；
3. WebSocket 断开。

解决：

后端 catch 中必须：

```text
更新 status = FAILED
推送 FAILED 消息
```

前端收到 FAILED 后：

```text
running = false
展示错误提示
```

---

## 28.5 任务完成但 reportId 为空

可能原因：

1. `analysisService.matchForUser()` 没有返回 reportId；
2. 分析报告没有插入成功；
3. `updateProgress()` 没有保存 reportId；
4. Redis 缓存命中时返回的 reportId 为空。

需要检查：

```sql
SELECT id, report_id, status, progress
FROM analysis_task
ORDER BY id DESC;
```

---

# 29. 兜底方案：WebSocket + 轮询

为了更稳，可以同时做轮询兜底。

前端创建任务后：

```text
WebSocket 接收实时消息
每 3 秒 GET /api/analysis/tasks/{taskNo}
```

如果 WebSocket 丢消息，轮询还能拿到最终状态。

---

## 29.1 前端轮询示例

```ts
let timer: number | undefined

function startPolling(taskNo: string) {
  timer = window.setInterval(async () => {
    const detail: any = await getAnalysisTaskDetailApi(taskNo)

    task.status = detail.status
    task.progress = detail.progress
    task.message = detail.message
    task.reportId = detail.reportId
    task.errorMessage = detail.errorMessage

    if (detail.status === 'SUCCESS' || detail.status === 'FAILED') {
      running.value = false
      clearInterval(timer)
    }
  }, 3000)
}
```

---

# 30. 面试讲解准备

## 30.1 面试官问：为什么要引入 WebSocket？

回答：

```text
AI 分析属于长耗时任务，如果直接使用同步 HTTP 接口，用户只能看到 loading，不知道系统执行到哪一步。为了优化体验，我把 AI 分析改造成异步任务模型。用户提交分析请求后，后端立即返回 taskId，然后后台线程执行分析流程，并通过 WebSocket 实时推送任务进度，前端展示进度条和当前步骤。
```

---

## 30.2 面试官问：WebSocket 推送流程是怎样的？

回答：

```text
用户点击开始分析后，前端先调用 POST /api/analysis/tasks 创建任务，后端保存 analysis_task 记录并返回 taskNo。前端拿到 taskNo 后订阅 /topic/analysis/{taskNo}。

后端在线程池中异步执行 AI 分析，每完成一个阶段就更新 analysis_task 表，并通过 SimpMessagingTemplate 向 /topic/analysis/{taskNo} 推送进度消息。当前端收到 SUCCESS 状态时，就展示查看分析报告按钮。
```

---

## 30.3 面试官问：异步线程里怎么获取当前用户？

回答：

```text
异步线程中不能直接依赖 SecurityContext，因为 SecurityContext 默认不会自动传播到新线程。所以我在创建任务时从当前请求中获取 userId，并保存到 analysis_task 表，同时把 userId 作为参数传入异步执行方法。后续所有资源校验都使用这个 userId。
```

---

## 30.4 面试官问：为什么不只用轮询？

回答：

```text
轮询实现简单，但会产生无效请求，而且实时性较差。WebSocket 可以由后端主动推送进度，用户体验更好。

不过为了稳定性，也可以使用 WebSocket + 轮询兜底。WebSocket 负责实时体验，轮询负责防止消息丢失或连接断开。
```

---

## 30.5 面试官问：这个设计有什么不足？

回答：

```text
当前第一阶段使用的是内存线程池和简单 WebSocket topic 推送，如果系统重启，正在执行的任务会中断；如果部署多实例，WebSocket 推送也需要考虑会话所在节点。

后续可以使用消息队列来执行 AI 分析任务，使用 Redis 保存任务状态，并通过更完善的 WebSocket 鉴权或消息网关支持多实例部署。
```

---

# 31. 后续优化方向

## 31.1 消息队列

后续可以使用：

```text
RabbitMQ
Kafka
Redis Stream
```

流程：

```text
创建任务
  ↓
发送消息到队列
  ↓
消费者执行 AI 分析
  ↓
更新任务状态
  ↓
WebSocket 推送
```

---

## 31.2 多实例支持

如果后端多实例部署，需要考虑：

1. WebSocket 连接在哪个实例；
2. 任务在哪个实例执行；
3. 推送消息如何广播；
4. 是否需要 Redis Pub/Sub。

---

## 31.3 任务取消

后续可以增加：

```text
PUT /api/analysis/tasks/{taskNo}/cancel
```

状态：

```text
CANCELLED
```

---

## 31.4 任务历史

后续可以增加：

```text
GET /api/analysis/tasks
```

展示用户历史 AI 分析任务。

---

# 32. 开发顺序建议

推荐按以下顺序开发：

```text
1. 创建 analysis_task 表；
2. 创建 AnalysisTaskStatusEnum；
3. 创建 AnalysisTask Entity；
4. 创建 AnalysisTaskMapper；
5. 创建 AnalysisTaskCreateRequest；
6. 创建 AnalysisTaskCreateResponse；
7. 创建 AnalysisTaskDetailResponse；
8. 创建 AnalysisProgressMessage；
9. 添加 WebSocket 依赖；
10. 创建 WebSocketConfig；
11. 创建 AnalysisProgressPublisher；
12. 创建 AsyncConfig；
13. 改造 AnalysisService，增加 matchForUser；
14. 创建 AnalysisTaskService；
15. 创建 AnalysisTaskServiceImpl；
16. 创建 AnalysisTaskController；
17. Swagger 测试创建任务；
18. 前端安装 STOMP / SockJS；
19. 封装 analysisTask.ts；
20. 封装 analysisSocket.ts；
21. 改造 AnalysisMatch.vue；
22. 前端联调进度条；
23. 测试 SUCCESS；
24. 测试 FAILED；
25. 增加轮询兜底。
```

---

# 33. 验收标准

## 33.1 后端验收

* [ ] analysis_task 表创建成功；
* [ ] POST /api/analysis/tasks 可以创建任务；
* [ ] 创建任务后立即返回 taskNo；
* [ ] 后端异步执行 AI 分析；
* [ ] 任务状态从 PENDING 变为 RUNNING；
* [ ] 任务进度逐步更新；
* [ ] 任务成功后 status = SUCCESS；
* [ ] 任务成功后 progress = 100；
* [ ] 任务成功后 reportId 不为空；
* [ ] 任务失败后 status = FAILED；
* [ ] 任务失败后 errorMessage 不为空；
* [ ] GET /api/analysis/tasks/{taskNo} 可以查询任务；
* [ ] 用户不能查询别人的任务。

---

## 33.2 WebSocket 验收

* [ ] 前端可以连接 `/ws/analysis`；
* [ ] 前端可以订阅 `/topic/analysis/{taskNo}`；
* [ ] 后端可以推送 PENDING；
* [ ] 后端可以推送 RUNNING；
* [ ] 后端可以推送 SUCCESS；
* [ ] 后端可以推送 FAILED；
* [ ] 前端收到消息后进度条实时变化。

---

## 33.3 前端验收

* [ ] AI 分析页可以创建任务；
* [ ] 创建任务后显示进度卡片；
* [ ] 进度条从 0 到 100；
* [ ] 当前步骤文字正常变化；
* [ ] 成功后显示“查看分析报告”按钮；
* [ ] 失败后显示错误提示；
* [ ] 页面离开时关闭 WebSocket；
* [ ] Token 失效时仍能跳转登录。

---

# 34. 简历写法

完成该功能后，简历可以增加一条：

```text
- 将 AI 分析流程改造为异步任务模型，基于 Spring WebSocket + STOMP 实现分析进度实时推送，前端通过进度条展示任务执行状态，优化长耗时 AI 调用的用户体验。
```

更完整版本：

```text
- 设计 AI 分析任务表 analysis_task，将同步 AI 分析接口升级为异步任务模型，使用线程池后台执行分析流程，并通过 WebSocket 向前端实时推送 PENDING/RUNNING/SUCCESS/FAILED 状态和进度百分比。
```

---

# 35. 模块设计结论

WebSocket AI 分析进度模块是 InternPilot 从“功能可用”升级到“体验更好、工程更完整”的关键增强。

它将原来的：

```text
同步 AI 分析接口
```

升级为：

```text
异步任务 + WebSocket 实时进度 + 前端进度条展示
```

完成后，项目具备更强的展示效果和面试表达价值。
