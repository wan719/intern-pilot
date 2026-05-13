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