package com.internpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan({
        "com.internpilot.mapper",
        "com.internpilot.application.mapper",
        "com.internpilot.feedback.mapper",
        "com.internpilot.job.mapper",
        "com.internpilot.resume.mapper",
        "com.internpilot.analysis.mapper",
        "com.internpilot.interview.mapper",
        "com.internpilot.recommendation.mapper"
})
@EnableScheduling
@SpringBootApplication
public class InternPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternPilotApplication.class, args);
    }
}
