package com.internpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan({
        "com.internpilot.application.mapper",
        "com.internpilot.feedback.mapper",
        "com.internpilot.job.mapper",
        "com.internpilot.resume.mapper",
        "com.internpilot.analysis.mapper",
        "com.internpilot.interview.mapper",
        "com.internpilot.recommendation.mapper",
        "com.internpilot.rag.mapper",
        "com.internpilot.user.mapper",
        "com.internpilot.rbac.mapper",
        "com.internpilot.audit.mapper"
})
@EnableScheduling
@SpringBootApplication
public class InternPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternPilotApplication.class, args);
    }
}
