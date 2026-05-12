# InternPilot 单元测试与集成测试增强文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的测试增强方案，包括测试目标、测试分层、后端单元测试、Controller 测试、Service 测试、Spring Security 权限测试、AI Mock 测试、WebSocket 测试、前端测试、Docker 测试和 CI/CD 前置测试。

当前 InternPilot 已经具备较完整的业务模块：

```text
用户认证与 JWT 鉴权
RBAC 权限系统
简历上传与解析
简历版本管理
岗位 JD 管理
AI 匹配分析
WebSocket AI 分析进度
AI 面试题生成
岗位推荐
RAG 岗位知识库
投递记录管理
系统操作日志
管理员后台
````

功能越来越多后，仅靠手动测试已经不可靠。

因此需要建立一套基础测试体系，保证：

```text
核心功能不容易被改坏
权限控制不被绕过
AI 模块可以稳定 Mock
前后端联调前后端各自可验证
后续 CI/CD 可以自动跑测试
```

---

# 2. 为什么要做测试增强

当前项目已经从 MVP 阶段进入工程化阶段。

如果没有测试，后续会出现这些问题：

```text
改了 RBAC，登录接口坏了
改了简历版本，AI 分析读不到简历内容
改了 WebSocket，任务状态不更新
改了岗位推荐，已投递岗位又被推荐
改了管理员后台，普通用户能访问后台接口
改了 AI Prompt，AI 返回 JSON 解析失败
```

测试增强的目标不是追求 100% 覆盖率，而是保证核心链路稳定。

---

# 3. 测试目标

第一阶段测试增强目标：

1. Auth 登录注册测试；
    
2. JWT 鉴权测试；
    
3. RBAC 权限测试；
    
4. 简历上传与版本测试；
    
5. 岗位 JD 管理测试；
    
6. AI 分析 Mock 测试；
    
7. WebSocket AI 任务测试；
    
8. AI 面试题生成 Mock 测试；
    
9. 岗位推荐规则测试；
    
10. RAG 文本切分与检索测试；
    
11. 投递记录测试；
    
12. 操作日志 AOP 测试；
    
13. 管理员后台权限测试；
    
14. 前端 build 测试；
    
15. Docker Compose 启动测试。
    

---

# 4. 测试分层设计

InternPilot 推荐采用四层测试：

```text
单元测试
  ↓
Service 层测试
  ↓
Controller / Web 层测试
  ↓
集成测试
```

---

## 4.1 单元测试

主要测试纯 Java 逻辑：

```text
JWT 工具类
权限工具类
文本切分工具
向量相似度工具
技能关键词提取
推荐分数计算
JSON 解析工具
```

特点：

```text
速度快
不启动 Spring 容器
不连接数据库
适合高频运行
```

---

## 4.2 Service 层测试

主要测试业务逻辑：

```text
用户注册
简历版本创建
岗位创建
AI 分析逻辑
岗位推荐生成
投递记录创建
```

特点：

```text
可以使用 Mockito Mock Mapper
不一定连接真实数据库
适合测试业务规则
```

---

## 4.3 Controller 层测试

主要测试接口行为：

```text
请求参数校验
HTTP 状态码
返回结构
权限控制
401 / 403
```

特点：

```text
使用 MockMvc
可以 Mock Service
重点验证接口层
```

---

## 4.4 集成测试

主要测试完整链路：

```text
注册
登录
携带 Token
访问受保护接口
权限不足返回 403
上传简历
创建岗位
AI Mock 分析
创建投递记录
```

特点：

```text
启动 Spring 测试上下文
可以使用 H2 / Testcontainers / 本地 MySQL
速度较慢
但价值高
```

---

# 5. 测试技术选型

## 5.1 后端测试技术栈

|技术|用途|
|---|---|
|JUnit 5|Java 单元测试|
|Mockito|Mock 对象|
|Spring Boot Test|Spring 集成测试|
|MockMvc|Controller 接口测试|
|H2 Database|测试数据库|
|AssertJ|断言|
|Testcontainers|可选，真实 MySQL / Redis 容器测试|

---

## 5.2 前端测试技术栈

第一阶段不强制复杂前端测试。

建议至少做：

```text
TypeScript 类型检查
npm run build
ESLint
核心工具函数测试
```

后续可加入：

|技术|用途|
|---|---|
|Vitest|前端单元测试|
|Vue Test Utils|Vue 组件测试|
|Playwright|E2E 测试|

---

# 6. Gradle 依赖检查

## 6.1 build.gradle 推荐依赖

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'org.mockito:mockito-junit-jupiter'

    runtimeOnly 'com.h2database:h2'
}

test {
    useJUnitPlatform()
}
```

---

## 6.2 Spring Security Test 作用

`spring-security-test` 可以帮助测试：

```text
模拟登录用户
模拟权限
测试 @PreAuthorize
测试 401
测试 403
```

常用注解：

```java
@WithMockUser
```

示例：

```java
@WithMockUser(username = "wan", authorities = {"resume:read"})
```

---

# 7. 测试目录结构

建议后端测试目录：

```text
src/test/java/com/internpilot
├── controller
│   ├── AuthControllerTest.java
│   ├── ResumeControllerTest.java
│   ├── JobControllerTest.java
│   ├── AnalysisControllerTest.java
│   ├── ApplicationControllerTest.java
│   └── AdminControllerSecurityTest.java
├── service
│   ├── AuthServiceTest.java
│   ├── ResumeVersionServiceTest.java
│   ├── JobRecommendationServiceTest.java
│   ├── InterviewQuestionServiceTest.java
│   └── ApplicationServiceTest.java
├── security
│   ├── JwtTokenProviderTest.java
│   └── RbacPermissionTest.java
├── util
│   ├── TextChunkUtilsTest.java
│   ├── VectorUtilsTest.java
│   ├── SkillKeywordUtilsTest.java
│   └── RecommendationScoreCalculatorTest.java
├── integration
│   ├── AuthIntegrationTest.java
│   ├── SecurityIntegrationTest.java
│   └── BusinessFlowIntegrationTest.java
└── config
    └── TestConfig.java
```

---

# 8. 测试环境配置

## 8.1 application-test.yml

路径：

```text
src/test/resources/application-test.yml
```

推荐：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:intern_pilot_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:

  sql:
    init:
      mode: always
      schema-locations: classpath:sql/test-schema.sql
      data-locations: classpath:sql/test-data.sql

  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true

jwt:
  secret: intern-pilot-test-secret-key-please-change-to-long-enough-string
  expiration: 3600000

ai:
  provider: mock
  api-key: test
  base-url: mock
  model: mock-ai
  timeout: 3000

rag:
  enabled: false
  top-k: 3
  embedding:
    provider: mock
    model: mock-embedding-64
```

---

## 8.2 为什么测试环境用 Mock AI

AI 接口不适合直接放进自动化测试：

```text
慢
贵
不稳定
受网络影响
返回内容不确定
```

测试中应该使用：

```text
MockAiClient
MockEmbeddingClient
```

这样测试稳定可重复。

---

# 9. Mock AI 设计

## 9.1 AiClient 接口

如果已有：

```java
public interface AiClient {
    String chat(String prompt);
}
```

测试环境可以注入 Mock 实现。

---

## 9.2 MockAiClient

路径：

```text
src/test/java/com/internpilot/config/MockAiClientTestConfig.java
```

```java
package com.internpilot.config;

import com.internpilot.service.AiClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockAiClientTestConfig {

    @Bean
    @Primary
    public AiClient mockAiClient() {
        return prompt -> """
                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": ["Java基础较好", "有Spring Boot项目经验"],
                  "weaknesses": ["缺少Docker部署经验"],
                  "missingSkills": ["Docker", "消息队列"],
                  "suggestions": ["补充Docker基础", "完善项目部署说明"],
                  "interviewTips": ["准备Spring Security过滤器链", "准备MySQL索引问题"]
                }
                """;
    }
}
```

---

## 9.3 Mock 面试题 AI 返回

```java
@Bean
@Primary
public AiClient mockAiClient() {
    return prompt -> {
        if (prompt.contains("面试题")) {
            return """
                    {
                      "title": "Java后端实习面试题准备",
                      "questions": [
                        {
                          "questionType": "SPRING_SECURITY",
                          "difficulty": "MEDIUM",
                          "question": "请介绍 Spring Security 的过滤器链执行流程。",
                          "answer": "Spring Security 通过 SecurityFilterChain 处理请求。",
                          "answerPoints": ["SecurityFilterChain", "JwtAuthenticationFilter", "SecurityContext"],
                          "relatedSkills": ["Spring Security", "JWT"]
                        }
                      ]
                    }
                    """;
        }

        return """
                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": ["Java基础较好"],
                  "weaknesses": ["缺少Docker经验"],
                  "missingSkills": ["Docker"],
                  "suggestions": ["补充Docker"],
                  "interviewTips": ["准备JWT鉴权流程"]
                }
                """;
    };
}
```

---

# 10. 工具类测试

## 10.1 TextChunkUtilsTest

测试目标：

```text
长文本能切分
空文本返回空列表
chunk 长度合理
```

示例：

```java
package com.internpilot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkUtilsTest {

    @Test
    void splitToChunks_shouldReturnEmpty_whenContentBlank() {
        List<String> chunks = TextChunkUtils.splitToChunks("   ");

        assertThat(chunks).isEmpty();
    }

    @Test
    void splitToChunks_shouldSplitParagraphs() {
        String content = """
                Java后端岗位要求掌握Java基础、Spring Boot、MySQL。

                Redis是常见缓存中间件，需要理解缓存穿透、击穿和雪崩。

                Spring Security常用于认证和授权，JWT项目中通常会自定义过滤器。
                """;

        List<String> chunks = TextChunkUtils.splitToChunks(content);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0)).contains("Java后端");
    }
}
```

---

## 10.2 VectorUtilsTest

```java
package com.internpilot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VectorUtilsTest {

    @Test
    void cosineSimilarity_shouldReturnOne_whenVectorsSame() {
        List<Double> a = List.of(1.0, 0.0, 1.0);
        List<Double> b = List.of(1.0, 0.0, 1.0);

        double similarity = VectorUtils.cosineSimilarity(a, b);

        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    void cosineSimilarity_shouldReturnZero_whenVectorEmpty() {
        double similarity = VectorUtils.cosineSimilarity(List.of(), List.of(1.0));

        assertThat(similarity).isEqualTo(0.0);
    }
}
```

---

## 10.3 SkillKeywordUtilsTest

```java
package com.internpilot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillKeywordUtilsTest {

    @Test
    void extractSkills_shouldFindJavaBackendSkills() {
        String text = "我熟悉 Java、Spring Boot、MySQL、Redis 和 Spring Security。";

        List<String> skills = SkillKeywordUtils.extractSkills(text);

        assertThat(skills).contains("Java", "Spring Boot", "MySQL", "Redis", "Spring Security");
    }

    @Test
    void calculateSkillScore_shouldCalculateCorrectly() {
        int score = SkillKeywordUtils.calculateSkillScore(
                List.of("Java", "Spring Boot"),
                List.of("Java", "Spring Boot", "MySQL", "Redis")
        );

        assertThat(score).isEqualTo(50);
    }
}
```

---

## 10.4 RecommendationScoreCalculatorTest

```java
package com.internpilot.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationScoreCalculatorTest {

    @Test
    void calculateFinalScore_shouldUseHybrid_whenAiScoreExists() {
        int score = RecommendationScoreCalculator.calculateFinalScore(80, 90, 70);

        assertThat(score).isEqualTo(83);
    }

    @Test
    void calculateFinalScore_shouldUseRuleScore_whenAiScoreMissing() {
        int score = RecommendationScoreCalculator.calculateFinalScore(80, 0, 60);

        assertThat(score).isEqualTo(76);
    }
}
```

---

# 11. JWT 测试

## 11.1 JwtTokenProviderTest

测试目标：

```text
能生成 Token
能解析 username
能解析 userId
无效 Token 校验失败
过期 Token 校验失败
```

示例：

```java
package com.internpilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "wan", "USER");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("wan");
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenInvalid() {
        boolean valid = jwtTokenProvider.validateToken("invalid.token.value");

        assertThat(valid).isFalse();
    }
}
```

---

# 12. AuthController 测试

## 12.1 测试目标

认证模块至少测试：

```text
注册成功
用户名重复注册失败
登录成功
密码错误登录失败
未登录访问 /api/user/me 返回 401
```

---

## 12.2 AuthControllerTest 示例

```java
package com.internpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.service.AuthService;
import com.internpilot.vo.auth.LoginResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void login_shouldReturnToken_whenSuccess() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setToken("mock-token");
        response.setTokenType("Bearer");

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest();
        request.setUsername("wan");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("mock-token"));
    }

    @Test
    void register_shouldReturnBadRequest_whenUsernameBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("123456");
        request.setConfirmPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

---

# 13. RBAC 权限测试

## 13.1 测试目标

RBAC 至少测试：

```text
有权限可以访问
无权限返回 403
未登录返回 401
普通用户不能访问管理员接口
管理员可以访问管理员接口
```

---

## 13.2 Controller 权限测试示例

```java
package com.internpilot.controller;

import com.internpilot.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
class ResumeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

    @Test
    void list_shouldReturnUnauthorized_whenNotLogin() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"resume:read"})
    void list_shouldReturnOk_whenHasPermission() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"job:read"})
    void list_shouldReturnForbidden_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isForbidden());
    }
}
```

---

# 14. Service 层测试

## 14.1 AuthServiceTest

测试目标：

```text
注册时密码加密
注册时创建 USER 角色关系
用户名重复时失败
登录密码错误时失败
禁用用户不能登录
```

---

## 14.2 ResumeVersionServiceTest

测试目标：

```text
上传简历后创建 ORIGINAL 版本
创建手动版本成功
设置当前版本时其他版本变为非当前
不能删除当前版本
不能访问他人版本
版本对比返回新增和删除行
```

示例测试点：

```java
@Test
void setCurrent_shouldOnlyOneCurrentVersion() {
    // given
    // 用户已有两个版本 v1 / v2，其中 v1 是当前版本

    // when
    // 设置 v2 为当前版本

    // then
    // v1.isCurrent = 0
    // v2.isCurrent = 1
}
```

---

## 14.3 JobRecommendationServiceTest

测试目标：

```text
技能匹配分计算正确
已投递岗位默认被排除
推荐结果按分数降序
有 AI 分析分时使用混合分数
无 AI 分析分时使用规则分数
不能访问他人的推荐批次
```

---

## 14.4 ApplicationServiceTest

测试目标：

```text
创建投递记录成功
重复投递同一岗位失败
修改投递状态成功
删除投递记录为逻辑删除
用户不能修改他人的投递记录
```

---

# 15. AI 模块测试

## 15.1 AI 分析测试目标

```text
使用 Mock AI 返回固定 JSON
能解析 matchScore
能保存 analysis_report
缓存命中时不重复调用 AI
forceRefresh=true 时重新生成
AI 返回非法 JSON 时抛出业务异常
```

---

## 15.2 InterviewQuestionServiceTest

测试目标：

```text
Mock AI 返回题目 JSON
能保存 interview_question_report
能保存 interview_question
forceRefresh=false 时复用旧报告
forceRefresh=true 时重新生成
AI 返回空 questions 时失败
```

---

## 15.3 RAG 测试目标

```text
创建知识文档后生成 chunk
chunk embedding 不为空
检索返回相似度排序结果
RAG 检索失败时 AI 分析降级
```

---

# 16. WebSocket AI 任务测试

## 16.1 测试目标

WebSocket 模块至少测试：

```text
创建任务后状态为 PENDING
异步执行后状态变为 SUCCESS
失败时状态变为 FAILED
任务完成后 reportId 不为空
用户不能查询他人任务
```

---

## 16.2 不强制测试真实 WebSocket

第一阶段可以不测试真实 STOMP 推送。

先测试任务状态表：

```text
analysis_task.status
analysis_task.progress
analysis_task.report_id
```

真实 WebSocket 可以放到联调测试中。

---

# 17. 操作日志 AOP 测试

## 17.1 测试目标

```text
加 @OperationLog 的接口成功后写入日志
接口抛异常时 success = 0
costTime 有值
module / operation / operationType 正确
普通查询接口不记录日志
```

---

## 17.2 注意点

AOP 测试必须通过 Spring Bean 调用，不能直接 new 对象。

否则切面不会生效。

---

# 18. 管理员后台测试

## 18.1 后台接口权限测试

测试目标：

```text
普通用户访问 /api/admin/users 返回 403
管理员访问 /api/admin/users 返回 200
未登录访问 /api/admin/users 返回 401
普通用户不能查看操作日志
管理员可以查看操作日志
```

---

## 18.2 禁用用户测试

测试目标：

```text
管理员可以禁用普通用户
管理员不能禁用自己
禁用用户不能登录
启用后可以登录
```

---

# 19. 集成测试设计

## 19.1 SecurityIntegrationTest

完整验证：

```text
注册用户
登录获取 Token
无 Token 访问业务接口返回 401
普通用户访问业务接口成功
普通用户访问管理员接口返回 403
管理员访问管理员接口成功
```

---

## 19.2 BusinessFlowIntegrationTest

测试完整业务链路：

```text
注册 / 登录
上传简历
自动创建原始版本
创建岗位 JD
发起 AI 分析 Mock
生成 AI 面试题 Mock
创建投递记录
修改投递状态
查询 Dashboard 数据
```

---

# 20. PowerShell 本地验收命令

## 20.1 后端测试

```powershell
cd backend\intern-pilot-backend
.\gradlew.bat clean test
```

期望：

```text
BUILD SUCCESSFUL
```

---

## 20.2 单独运行某个测试类

```powershell
.\gradlew.bat test --tests AuthControllerTest
```

---

## 20.3 单独运行某个包

```powershell
.\gradlew.bat test --tests "com.internpilot.service.*"
```

---

# 21. 前端测试增强

## 21.1 第一阶段必须做

前端至少保证：

```text
npm install 成功
npm run build 成功
页面无明显 TypeScript 错误
路由无明显错误
Axios 封装可用
```

命令：

```powershell
cd frontend\intern-pilot-frontend
npm install
npm run build
```

---

## 21.2 建议增加 package.json 脚本

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview",
    "type-check": "vue-tsc -b"
  }
}
```

---

## 21.3 后续加入 Vitest

安装：

```bash
npm install -D vitest @vue/test-utils jsdom
```

测试工具函数：

```text
权限判断 hasPermission
Token 工具 getToken / setToken
状态映射函数
推荐等级颜色映射
```

---

# 22. Docker 测试

## 22.1 Docker Compose 启动测试

```powershell
cd deploy
docker compose up -d --build
```

检查：

```powershell
docker compose ps
```

期望：

```text
mysql running
redis running
backend running
```

---

## 22.2 后端健康检查

```powershell
curl.exe http://localhost:8080/api/health
```

---

## 22.3 Swagger 检查

浏览器访问：

```text
http://localhost:8080/doc.html
```

---

# 23. 测试覆盖优先级

## 23.1 P0 必须测试

```text
Auth 登录注册
JWT 解析
RBAC 401 / 403
用户数据隔离
AI Mock 分析
投递记录
管理员接口权限
```

---

## 23.2 P1 建议测试

```text
简历版本管理
岗位推荐
AI 面试题生成
RAG 文本切分
操作日志 AOP
WebSocket 任务状态
```

---

## 23.3 P2 后续测试

```text
真实 WebSocket 推送
真实 Redis 缓存
真实 MySQL Testcontainers
前端 E2E
CI/CD 部署测试
压力测试
```

---

# 24. 常见测试问题

## 24.1 测试启动慢

原因：

```text
@SpringBootTest 启动完整 Spring 容器
```

解决：

```text
工具类测试不用 Spring
Controller 用 @WebMvcTest
Service 用 Mockito
只有集成测试用 @SpringBootTest
```

---

## 24.2 测试误调用真实 AI

原因：

```text
测试环境没有注入 MockAiClient
```

解决：

```text
@ActiveProfiles("test")
@TestConfiguration + @Primary
```

---

## 24.3 权限测试一直 401

可能原因：

```text
MockMvc 没有加载 Spring Security
测试配置缺少 security filter
没有使用 @WithMockUser
```

---

## 24.4 权限测试一直 403

可能原因：

```text
@WithMockUser 没有配置 authorities
写成 roles 但接口用 hasAuthority
权限字符串不一致
```

示例：

```java
@WithMockUser(authorities = {"resume:read"})
```

不要写成：

```java
@WithMockUser(roles = {"resume:read"})
```

---

## 24.5 H2 和 MySQL SQL 不兼容

解决：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:intern_pilot_test;MODE=MySQL
```

如果仍然不兼容，复杂 SQL 可以用 Testcontainers 跑真实 MySQL。

---

# 25. CI/CD 前置测试命令

后续 GitHub Actions 可以执行：

```bash
cd backend/intern-pilot-backend
./gradlew clean test

cd ../../frontend/intern-pilot-frontend
npm ci
npm run build
```

CI 要求：

```text
后端测试失败，不允许合并
前端 build 失败，不允许合并
Docker 构建失败，不允许部署
```

---

# 26. 推荐测试清单

## 26.1 后端测试清单

```text
[ ] JwtTokenProviderTest
[ ] AuthControllerTest
[ ] AuthServiceTest
[ ] ResumeControllerSecurityTest
[ ] ResumeVersionServiceTest
[ ] JobServiceTest
[ ] AnalysisServiceMockTest
[ ] AnalysisTaskServiceTest
[ ] InterviewQuestionServiceTest
[ ] JobRecommendationServiceTest
[ ] RagKnowledgeServiceTest
[ ] ApplicationServiceTest
[ ] OperationLogAspectTest
[ ] AdminSecurityTest
[ ] BusinessFlowIntegrationTest
```

---

## 26.2 前端测试清单

```text
[ ] npm install
[ ] npm run build
[ ] 登录页可打开
[ ] 管理员菜单权限过滤
[ ] AI 分析页可打开
[ ] 岗位推荐页可打开
[ ] 操作日志页仅管理员可见
```

---

# 27. 面试讲解准备

## 27.1 面试官问：你的项目怎么保证功能稳定？

回答：

```text
我给项目补充了分层测试。工具类和推荐算法使用 JUnit 做单元测试，Service 层使用 Mockito Mock Mapper 和 AI Client，Controller 层使用 MockMvc 测试接口参数、返回结构和权限控制。

对于 AI 模块，我没有在测试中调用真实大模型，而是注入 MockAiClient 返回固定 JSON，保证测试稳定可重复。权限部分重点测试 401 和 403，例如未登录访问业务接口返回 401，普通用户访问管理员接口返回 403。
```

---

## 27.2 面试官问：AI 接口怎么测试？

回答：

```text
AI 接口不适合在自动化测试中直接调用真实模型，因为它慢、贵、不稳定，而且返回内容不确定。所以我抽象了 AiClient 接口，在测试环境用 MockAiClient 替代真实实现。

MockAiClient 返回固定 JSON，这样可以测试后端能否正确解析 AI 返回、保存分析报告、保存面试题和处理异常。
```

---

## 27.3 面试官问：权限测试怎么做？

回答：

```text
我使用 spring-security-test 和 MockMvc 测试权限控制。通过 @WithMockUser(authorities = {...}) 模拟不同权限的用户访问接口。

例如有 resume:read 权限可以访问简历列表，没有该权限返回 403；不带登录信息访问业务接口返回 401。管理员接口也会测试普通用户访问返回 403，管理员访问返回 200。
```

---

## 27.4 面试官问：单元测试和集成测试有什么区别？

回答：

```text
单元测试主要验证某个类或方法的逻辑，比如推荐分数计算、文本切分、向量相似度，不依赖 Spring 容器和数据库。

集成测试会启动 Spring 上下文，测试多个模块之间能否协同工作，比如注册登录、携带 Token 访问接口、创建岗位、AI Mock 分析、创建投递记录等完整业务链路。
```

---

# 28. 简历写法

完成测试增强后，简历可以写：

```text
- 为项目补充 JUnit5 + Mockito + MockMvc 测试体系，覆盖认证登录、JWT 鉴权、RBAC 权限、AI Mock 分析、岗位推荐和投递记录等核心模块。
```

更完整写法：

```text
- 建立分层测试体系，使用 JUnit5 测试工具类和推荐算法，使用 Mockito Mock AI Client 保证 AI 模块测试稳定，使用 MockMvc + spring-security-test 验证接口权限控制、401/403 返回和核心业务链路。
```

---

# 29. 开发顺序建议

推荐按以下顺序补测试：

```text
1. 先补工具类测试；
2. 补 JwtTokenProviderTest；
3. 补 AuthControllerTest；
4. 补 RBAC 权限测试；
5. 补 ResumeVersionServiceTest；
6. 补 AnalysisService Mock 测试；
7. 补 InterviewQuestionService Mock 测试；
8. 补 JobRecommendationServiceTest；
9. 补 ApplicationServiceTest；
10. 补 AdminSecurityTest；
11. 补 BusinessFlowIntegrationTest；
12. 最后接入 CI/CD。
```

---

# 30. 验收标准

## 30.1 后端验收

-  `./gradlew test` 可以通过；
    
-  工具类测试通过；
    
-  JWT 测试通过；
    
-  Auth 测试通过；
    
-  RBAC 401 / 403 测试通过；
    
-  AI Mock 测试通过；
    
-  岗位推荐测试通过；
    
-  投递记录测试通过；
    
-  管理员权限测试通过；
    
-  不会调用真实 AI API；
    
-  测试环境不依赖真实生产配置。
    

---

## 30.2 前端验收

-  `npm install` 成功；
    
-  `npm run build` 成功；
    
-  TypeScript 无明显错误；
    
-  权限菜单过滤正常；
    
-  主要页面可访问。
    

---

## 30.3 工程验收

-  测试配置独立；
    
-  测试数据独立；
    
-  敏感配置不进入测试代码；
    
-  测试失败可以定位问题；
    
-  后续可以接入 GitHub Actions。
    

---

# 31. 模块设计结论

测试增强是 InternPilot 从“能跑的项目”升级为“更可靠的工程项目”的关键步骤。

它的核心不是追求测试数量，而是保证关键链路：

```text
认证
权限
AI Mock
简历
岗位
投递
管理员后台
```

不会因为后续改动被破坏。

完成测试增强后，InternPilot 将具备更完整的工程化能力：

```text
功能模块完整
权限体系完整
AI 能力完整
测试体系初步建立
后续可接入 CI/CD
```