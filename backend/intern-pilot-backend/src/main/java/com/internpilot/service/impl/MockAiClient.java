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
                    "Has Spring Boot project experience",
                    "Understands JWT authentication and authorization",
                    "Has MySQL and Redis usage experience"
                  ],
                  "weaknesses": [
                    "Lacks real enterprise internship experience",
                    "High concurrency and deployment experience can be improved"
                  ],
                  "missingSkills": [
                    "Docker",
                    "Linux deployment",
                    "Message queue"
                  ],
                  "suggestions": [
                    "Add Redis cache scenarios to the project description",
                    "Add Docker Compose deployment notes",
                    "Improve API documentation and test guide"
                  ],
                  "interviewTips": [
                    "Prepare the Spring Security filter chain flow",
                    "Prepare the JWT login flow",
                    "Prepare Redis cache penetration, breakdown, and avalanche questions"
                  ]
                }
                """;
    }
}
