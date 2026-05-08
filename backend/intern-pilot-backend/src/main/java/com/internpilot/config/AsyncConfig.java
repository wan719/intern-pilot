package com.internpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean("analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}