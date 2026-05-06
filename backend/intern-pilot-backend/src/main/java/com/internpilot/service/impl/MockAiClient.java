package com.internpilot.service.impl;

import com.internpilot.service.AiClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockAiClient implements AiClient {

    @Override
    public String chat(String prompt) {
        return """
                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": [
                    "具备 Spring Boot 项目经验",
                    "熟悉 JWT 鉴权和权限控制",
                    "有 MySQL 和 Redis 使用经验"
                  ],
                  "weaknesses": [
                    "缺少真实企业实习经历",
                    "项目中高并发和部署经验体现不足"
                  ],
                  "missingSkills": [
                    "Docker",
                    "Linux 部署",
                    "消息队列"
                  ],
                  "suggestions": [
                    "在项目描述中补充 Redis 缓存使用场景",
                    "增加 Docker Compose 部署说明",
                    "补充接口文档和测试说明"
                  ],
                  "interviewTips": [
                    "准备 Spring Security 过滤链执行流程",
                    "准备 JWT 登录流程",
                    "准备 Redis 缓存穿透、击穿、雪崩问题"
                  ]
                }
                """;
    }
}
