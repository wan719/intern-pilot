package com.internpilot.service.impl;

import com.internpilot.enums.AiScenarioEnum;
import com.internpilot.service.AiClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
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

        if (prompt.contains("匹配分析") || prompt.contains("匹配度") || prompt.contains("matchScore")) {
            return AiScenarioEnum.RESUME_JOB_ANALYSIS;
        }

        if (prompt.contains("优化简历") || prompt.contains("简历优化") || prompt.contains("简历优化版本")) {
            return AiScenarioEnum.RESUME_OPTIMIZATION;
        }

        if (prompt.contains("面试题") || prompt.contains("面试准备") || prompt.contains("interview")) {
            return AiScenarioEnum.INTERVIEW_QUESTION_GENERATION;
        }

        if (prompt.contains("岗位推荐") || prompt.contains("推荐岗位")) {
            return AiScenarioEnum.JOB_RECOMMENDATION;
        }

        if (prompt.contains("知识库") || prompt.contains("RAG")) {
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
                    "具备 Spring Boot 项目开发经验",
                    "熟悉 MySQL、Redis、JWT 等后端常用技术",
                    "有完整项目从认证到部署的实践经历"
                  ],
                  "weaknesses": [
                    "企业级高并发经验不足",
                    "缺少真实线上项目性能优化经历",
                    "对分布式系统理解仍需加强"
                  ],
                  "missingSkills": [
                    "Docker",
                    "Linux 部署",
                    "消息队列"
                  ],
                  "suggestions": [
                    "补充 Redis 缓存、接口限流和性能优化相关实践",
                    "完善项目 README 和部署演示说明",
                    "准备 Spring Security、MyBatis、事务等面试题"
                  ],
                  "interviewTips": [
                    "准备 Spring Security 过滤器链流程",
                    "准备 JWT 登录流程",
                    "准备 Redis 缓存穿透、击穿、雪崩问题"
                  ]
                }
                """;
    }

    private String mockResumeOptimization() {
        return """
                优化后的简历内容：

                项目名称：InternPilot 面向大学生的 AI 实习投递与简历优化平台

                项目描述：
                基于 Spring Boot、Spring Security、MyBatis、MySQL、Redis、Vue3 和 Docker 构建的前后端分离 AI 应用平台，支持简历上传解析、岗位 JD 管理、AI 匹配分析、WebSocket 实时进度展示、RAG 岗位知识库和管理员权限系统。

                个人职责：
                1. 负责用户认证与 JWT 鉴权模块设计。
                2. 负责 RBAC 权限系统设计，实现用户、角色、权限动态管理。
                3. 负责 AI 简历匹配分析链路，实现 Redis 缓存和 WebSocket 进度推送。
                4. 负责前后端联调、接口测试和 Docker Compose 本地部署。
                """;
    }

    private String mockInterviewQuestions() {
        return """
                {
                  "title": "Java后端开发实习生 面试题准备",
                  "questions": [
                    {
                      "category": "Java基础",
                      "difficulty": "EASY",
                      "question": "HashMap 的底层数据结构是什么？",
                      "answer": "JDK 8 中 HashMap 底层由数组、链表和红黑树组成。当链表长度超过阈值且数组容量足够时，链表会转换为红黑树以提高查询效率。"
                    },
                    {
                      "category": "Spring Boot",
                      "difficulty": "MEDIUM",
                      "question": "Spring Boot 自动配置的原理是什么？",
                      "answer": "Spring Boot 通过自动配置类、条件注解和配置元数据，在应用启动时根据类路径、配置文件和 Bean 条件自动装配所需组件。"
                    },
                    {
                      "category": "项目经验",
                      "difficulty": "MEDIUM",
                      "question": "你的项目中为什么要使用 WebSocket 展示 AI 分析进度？",
                      "answer": "因为 AI 分析属于耗时任务，如果只使用同步接口，用户体验较差。WebSocket 可以在任务执行过程中实时推送阶段进度，提升交互体验。"
                    },
                    {
                      "category": "MySQL",
                      "difficulty": "MEDIUM",
                      "question": "MySQL 索引的底层数据结构是什么？为什么使用 B+ 树？",
                      "answer": "MySQL InnoDB 引擎使用 B+ 树作为索引结构。B+ 树所有数据存储在叶子节点，非叶子节点只存储键值，适合范围查询和磁盘 IO 优化。"
                    },
                    {
                      "category": "Redis",
                      "difficulty": "MEDIUM",
                      "question": "Redis 缓存穿透、击穿、雪崩分别是什么？如何解决？",
                      "answer": "缓存穿透：查询不存在的数据，解决方案是布隆过滤器或缓存空值。缓存击穿：热点 key 过期，解决方案是互斥锁或永不过期。缓存雪崩：大量 key 同时过期，解决方案是过期时间加随机值或集群部署。"
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
                      "jobTitle": "Java后端开发实习生",
                      "companyName": "腾讯",
                      "matchScore": 85,
                      "reason": "你的 Spring Boot 项目经验和后端技术栈与该岗位高度匹配"
                    },
                    {
                      "jobTitle": "Java开发实习生",
                      "companyName": "字节跳动",
                      "matchScore": 78,
                      "reason": "你的技术基础扎实，但高并发经验需要加强"
                    },
                    {
                      "jobTitle": "后端开发实习生",
                      "companyName": "美团",
                      "matchScore": 72,
                      "reason": "你的项目经验与岗位要求部分匹配，建议补充分布式系统知识"
                    }
                  ]
                }
                """;
    }

    private String mockRagQa() {
        return """
                根据知识库内容，以下是相关回答：

                Spring Boot 是一个基于 Spring 框架的快速开发框架，它提供了自动配置、起步依赖和 Actuator 等功能，帮助开发者快速构建生产级别的 Spring 应用。

                在实习面试中，面试官通常会关注候选人对 Spring Boot 核心特性的理解，包括自动配置原理、常用注解、以及如何与 MyBatis、Redis 等中间件集成。
                """;
    }

    private String mockDefaultResponse() {
        return """
                {
                  "message": "Mock AI 默认响应",
                  "note": "未识别到具体业务场景，返回通用响应"
                }
                """;
    }
}
