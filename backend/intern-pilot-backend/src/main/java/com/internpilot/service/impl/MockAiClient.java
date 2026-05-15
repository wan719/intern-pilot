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
          "questions": [
            {
              "type": "TECHNICAL",
              "question": "Spring Boot 中 @Transactional 注解的原理是什么？什么情况下事务会失效？",
              "answer": "基于 AOP 代理实现，通过 TransactionInterceptor 拦截。失效场景：同类方法调用、非 public 方法、异常被 catch、rollbackFor 配置不当。",
              "difficulty": "MEDIUM",
              "tags": ["Spring Boot", "事务"]
            },
            {
              "type": "TECHNICAL",
              "question": "Redis 缓存穿透、击穿和雪崩分别是什么？如何解决？",
              "answer": "穿透：查询不存在的数据，用布隆过滤器或缓存空值。击穿：热点 key 过期，用互斥锁或永不过期。雪崩：大量 key 同时过期，用随机过期时间或多级缓存。",
              "difficulty": "MEDIUM",
              "tags": ["Redis", "缓存"]
            },
            {
              "type": "TECHNICAL",
              "question": "JWT 的组成结构是什么？如何防止 JWT 被伪造？",
              "answer": "Header.Payload.Signature 三部分。防伪造：使用强密钥签名、设置合理过期时间、HTTPS 传输、不存敏感信息在 Payload。",
              "difficulty": "EASY",
              "tags": ["JWT", "安全"]
            },
            {
              "type": "PROJECT",
              "question": "请详细介绍你在 InternPilot 项目中负责的模块和技术难点。",
              "answer": "建议从 RBAC 权限设计、AI 分析流程、缓存策略等方面展开，重点说明自己独立完成的部分和遇到的挑战。",
              "difficulty": "MEDIUM",
              "tags": ["项目经验", "InternPilot"]
            },
            {
              "type": "BEHAVIORAL",
              "question": "在团队项目中遇到技术分歧时，你是如何处理的？",
              "answer": "先理解对方观点，用数据和 demo 验证方案可行性，以项目目标为导向达成共识。",
              "difficulty": "EASY",
              "tags": ["团队协作", "沟通"]
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
