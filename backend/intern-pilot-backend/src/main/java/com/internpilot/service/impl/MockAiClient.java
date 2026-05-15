package com.internpilot.service.impl;

import com.internpilot.enums.AiScenarioEnum;
import com.internpilot.service.AiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock")
public class MockAiClient implements AiClient {

  @Override
  public String chat(String prompt) {
    AiScenarioEnum scenario = detectScenario(prompt);

    return switch (scenario) {
      case RESUME_JOB_ANALYSIS -> mockResumeJobAnalysis();
      case RESUME_OPTIMIZATION -> mockResumeOptimization();
      case INTERVIEW_QUESTION_GENERATION -> mockInterviewQuestions();
      case JOB_RECOMMENDATION -> mockJobRecommendation();
      case RAG_QA -> mockRagQa();
      default -> mockDefaultResponse();
    };
  }

  private AiScenarioEnum detectScenario(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return AiScenarioEnum.UNKNOWN;
    }

    String lowerPrompt = prompt.toLowerCase();

    if (lowerPrompt.contains("recommend")
        || lowerPrompt.contains("job recommendation")) {
      return AiScenarioEnum.JOB_RECOMMENDATION;
    }

    if (lowerPrompt.contains("optimize")
        || lowerPrompt.contains("optimized resume")
        || lowerPrompt.contains("resume optimization")) {
      return AiScenarioEnum.RESUME_OPTIMIZATION;
    }

    if (lowerPrompt.contains("interview")
        || lowerPrompt.contains("questiontype")
        || lowerPrompt.contains("follow-up")
        || lowerPrompt.contains("followup")) {
      return AiScenarioEnum.INTERVIEW_QUESTION_GENERATION;
    }

    if (prompt.contains("matchScore")
        || prompt.contains("matching report")
        || lowerPrompt.contains("match analysis")
        || (lowerPrompt.contains("resume") && lowerPrompt.contains("job"))
        || (lowerPrompt.contains("json") && prompt.contains("JD"))) {
      return AiScenarioEnum.RESUME_JOB_ANALYSIS;
    }

    if (lowerPrompt.contains("rag")
        || lowerPrompt.contains("knowledge base")) {
      return AiScenarioEnum.RAG_QA;
    }

    return AiScenarioEnum.UNKNOWN;
  }

  private String mockResumeJobAnalysis() {
    return """
        {
          "matchScore": 82,
          "matchLevel": "MEDIUM_HIGH",
          "strengths": [
            "Spring Boot 项目经验与目标岗位高度匹配",
            "MySQL、Redis、JWT 和 RBAC 经验满足常见后端要求",
            "简历包含完整全栈项目及部署实践"
          ],
          "weaknesses": [
            "高并发生产经验仍然有限",
            "分布式系统经验需要更具体的证据",
            "测试和性能调优示例可以扩展"
          ],
          "missingSkills": [
            "Docker 部署",
            "Linux 运维",
            "消息队列"
          ],
          "suggestions": [
            "在项目部分补充可量化的 API、缓存和权限模块成果",
            "准备 Spring Security 过滤器链和 JWT 认证的具体案例",
            "增加部署或 CI/CD 段落以强化工程完整性"
          ],
          "interviewTips": [
            "清晰解释 Spring Security 认证流程",
            "准备 Redis 缓存穿透、击穿和雪崩的解决方案",
            "以 InternPilot 为主要项目故事，描述自己的职责分工"
          ]
        }
        """;
  }

  private String mockResumeOptimization() {
    return """
        优化后的简历内容（模拟）：

        个人信息
        --------
        姓名：张三
        学校：XX大学 软件工程 本科 2026届
        邮箱：zhangsan@example.com
        电话：138-xxxx-xxxx
        GitHub：github.com/zhangsan

        技术栈
        --------
        Java, Spring Boot, Spring Security, MyBatis, MySQL, Redis, JWT, Vue3, Docker, Git

        项目经历
        --------
        InternPilot - AI 实习投递平台 | 全栈开发
        2025.03 - 至今
        - 基于 Spring Boot + Vue3 构建 AI 驱动的实习投递平台
        - 设计并实现 RBAC 权限管理系统，支持角色-权限动态绑定
        - 集成 JWT 无状态认证，实现登录拦截和 Token 刷新
        - 使用 Redis 缓存 AI 分析结果，减少重复 API 调用
        - 通过 WebSocket 实时推送 AI 分析任务进度
        - 实现简历解析、岗位匹配、面试题生成等 AI 功能

        技能亮点
        --------
        - 熟悉 Spring Boot 自动配置原理和常用 Starter
        - 理解 MySQL InnoDB 索引结构和 SQL 优化
        - 掌握 Redis 缓存策略和常见问题解决方案
        - 了解 Docker 容器化部署和 CI/CD 流程
        """;
  }

  private String mockInterviewQuestions() {
    return """
        {
          "title": "Java后端实习面试题准备",
          "questions": [
            {
              "questionType": "SPRING_BOOT",
              "difficulty": "MEDIUM",
              "question": "Spring Boot 中 @Transactional 注解的原理是什么？什么情况下事务会失效？",
              "answer": "基于 AOP 代理实现，通过 TransactionInterceptor 拦截。失效场景：同类方法调用、非 public 方法、异常被 catch、rollbackFor 配置不当。",
              "answerPoints": [
                "Spring AOP 动态代理机制",
                "TransactionInterceptor 拦截器",
                "同类方法调用不走代理",
                "非 public 方法不生效",
                "rollbackFor 需正确配置"
              ],
              "relatedSkills": ["Spring Boot", "事务管理", "AOP"],
              "followUps": [
                "Spring 事务的传播行为有哪些？",
                "编程式事务和声明式事务的区别是什么？"
              ],
              "keywords": ["Spring Boot", "事务", "AOP"],
              "source": "根据岗位 Spring Boot 技能要求生成",
              "sortOrder": 1
            },
            {
              "questionType": "REDIS",
              "difficulty": "MEDIUM",
              "question": "Redis 缓存穿透、击穿和雪崩分别是什么？如何解决？",
              "answer": "穿透：查询不存在的数据，用布隆过滤器或缓存空值。击穿：热点 key 过期，用互斥锁或永不过期。雪崩：大量 key 同时过期，用随机过期时间或多级缓存。",
              "answerPoints": [
                "缓存穿透：布隆过滤器、缓存空值",
                "缓存击穿：互斥锁、逻辑过期",
                "缓存雪崩：随机过期时间、多级缓存"
              ],
              "relatedSkills": ["Redis", "缓存策略"],
              "followUps": [
                "布隆过滤器的原理是什么？",
                "Redis 集群模式下如何处理缓存一致性问题？"
              ],
              "keywords": ["Redis", "缓存穿透", "缓存击穿", "缓存雪崩"],
              "source": "根据岗位 Redis 技能要求生成",
              "sortOrder": 2
            },
            {
              "questionType": "SPRING_SECURITY",
              "difficulty": "EASY",
              "question": "JWT 的组成结构是什么？如何防止 JWT 被伪造？",
              "answer": "Header.Payload.Signature 三部分。防伪造：使用强密钥签名、设置合理过期时间、HTTPS 传输、不存敏感信息在 Payload。",
              "answerPoints": [
                "Header 包含算法类型",
                "Payload 包含声明信息",
                "Signature 用于验证完整性",
                "使用强密钥和 HTTPS"
              ],
              "relatedSkills": ["JWT", "Spring Security", "安全"],
              "followUps": [
                "JWT 和 Session 认证有什么区别？",
                "如何实现 Token 刷新机制？"
              ],
              "keywords": ["JWT", "认证", "安全"],
              "source": "根据岗位 Spring Security 技能要求生成",
              "sortOrder": 3
            },
            {
              "questionType": "PROJECT",
              "difficulty": "MEDIUM",
              "question": "请详细介绍你在 InternPilot 项目中负责的模块和技术难点。",
              "answer": "建议从 RBAC 权限设计、AI 分析流程、缓存策略等方面展开，重点说明自己独立完成的部分和遇到的挑战。",
              "answerPoints": [
                "RBAC 权限模型设计",
                "AI 分析异步任务流程",
                "Redis 缓存策略",
                "WebSocket 实时推送"
              ],
              "relatedSkills": ["Spring Boot", "RBAC", "Redis", "WebSocket"],
              "followUps": [
                "RBAC 和 ABAC 的区别是什么？",
                "WebSocket 连接断开后如何恢复？"
              ],
              "keywords": ["InternPilot", "RBAC", "全栈开发"],
              "source": "根据简历项目经历生成",
              "sortOrder": 4
            },
            {
              "questionType": "MYSQL",
              "difficulty": "MEDIUM",
              "question": "MySQL 中索引的底层数据结构是什么？为什么使用 B+ 树？",
              "answer": "InnoDB 使用 B+ 树作为索引结构。B+ 树所有数据存储在叶子节点，非叶子节点只存键值，适合范围查询和磁盘 IO 优化。",
              "answerPoints": [
                "B+ 树结构特点",
                "叶子节点形成有序链表",
                "适合磁盘预读",
                "支持范围查询"
              ],
              "relatedSkills": ["MySQL", "索引", "B+树"],
              "followUps": [
                "聚簇索引和非聚簇索引的区别？",
                "什么情况下索引会失效？"
              ],
              "keywords": ["MySQL", "索引", "B+树"],
              "source": "根据岗位 MySQL 技能要求生成",
              "sortOrder": 5
            },
            {
              "questionType": "JAVA_BASIC",
              "difficulty": "EASY",
              "question": "Java 中 HashMap 的底层实现原理是什么？",
              "answer": "JDK 8 中 HashMap 采用数组+链表+红黑树实现。通过 hash 计算索引，冲突时用链表法，链表长度超过 8 且数组长度 >= 64 时转为红黑树。",
              "answerPoints": [
                "数组+链表+红黑树结构",
                "hash 冲突解决",
                "链表转红黑树条件",
                "扩容机制"
              ],
              "relatedSkills": ["Java", "HashMap", "数据结构"],
              "followUps": [
                "HashMap 和 ConcurrentHashMap 的区别？",
                "为什么 HashMap 的容量是 2 的幂？"
              ],
              "keywords": ["Java", "HashMap", "集合"],
              "source": "根据岗位 Java 基础技能要求生成",
              "sortOrder": 6
            },
            {
              "questionType": "HR",
              "difficulty": "EASY",
              "question": "你为什么选择我们公司？你对实习有什么期望？",
              "answer": "建议结合公司业务方向和个人职业规划回答，表达对技术成长的渴望和对公司文化的认同。",
              "answerPoints": [
                "了解公司业务和技术栈",
                "表达个人成长诉求",
                "展示积极态度"
              ],
              "relatedSkills": ["沟通表达", "职业规划"],
              "followUps": [
                "你未来 3 年的职业规划是什么？",
                "如果实习期间遇到不熟悉的技术怎么办？"
              ],
              "keywords": ["HR面试", "职业规划", "沟通"],
              "source": "HR 面试常见问题",
              "sortOrder": 7
            },
            {
              "questionType": "JOB_SKILL",
              "difficulty": "HARD",
              "question": "如果让你设计一个高并发的秒杀系统，你会考虑哪些方面？",
              "answer": "前端限流、CDN 加速、Nginx 负载均衡、Redis 预减库存、消息队列异步下单、数据库最终一致性。",
              "answerPoints": [
                "前端限流和防重复提交",
                "Redis 预减库存",
                "消息队列削峰",
                "数据库最终一致性"
              ],
              "relatedSkills": ["高并发", "Redis", "消息队列", "系统设计"],
              "followUps": [
                "如何保证 Redis 和数据库的库存一致性？",
                "消息队列在秒杀场景中如何保证不丢消息？"
              ],
              "keywords": ["秒杀", "高并发", "系统设计"],
              "source": "根据岗位综合能力要求生成",
              "sortOrder": 8
            }
          ]
        }
        """;
  }

  private String mockJobRecommendation() {
    return """
        {
          "recommendations": [
            {
              "jobTitle": "Java 后端实习生",
              "companyName": "星云科技",
              "matchScore": 85,
              "reason": "Spring Boot 和 RBAC 项目经验与岗位高度匹配。"
            },
            {
              "jobTitle": "AI 应用开发实习生",
              "companyName": "阿斯特 AI",
              "matchScore": 78,
              "reason": "AI 分析流程和 Prompt 工程经验相关。"
            },
            {
              "jobTitle": "全栈开发实习生",
              "companyName": "云桥科技",
              "matchScore": 72,
              "reason": "Vue 和 Spring Boot 技术栈合适，但生产部署经验可以扩展。"
            }
          ]
        }
        """;
  }

  private String mockRagQa() {
    return "根据知识库内容，Spring Boot、MyBatis、Redis 和 JWT 是后端实习面试的常见话题。建议准备基于项目的示例和技术权衡的解释。";
  }

  private String mockDefaultResponse() {
    return """
        {
          "message": "Mock AI 默认响应",
          "note": "未检测到特定业务场景。"
        }
        """;
  }
}
