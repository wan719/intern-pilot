package com.internpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan({
        "com.internpilot.mapper",
        "com.internpilot.application.mapper",
        "com.internpilot.feedback.mapper"
})
@EnableScheduling
@SpringBootApplication
public class InternPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternPilotApplication.class, args);
    }
}
