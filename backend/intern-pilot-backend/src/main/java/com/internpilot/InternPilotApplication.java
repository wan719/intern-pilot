package com.internpilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.internpilot.mapper")
@SpringBootApplication
public class InternPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternPilotApplication.class, args);
    }
}