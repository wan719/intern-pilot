package com.internpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration//这个注解表示这是一个配置类，用于定义Spring Bean
@Schema(description = "异步任务配置类，定义了一个名为analysisTaskExecutor的线程池，用于执行异步分析任务")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class AsyncConfig {

    @Bean("analysisTaskExecutor")
    @Schema(description = "获取分析任务执行器，用于执行异步分析任务")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    public Executor analysisTaskExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}