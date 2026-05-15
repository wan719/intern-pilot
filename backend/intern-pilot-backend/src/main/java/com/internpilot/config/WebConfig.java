package com.internpilot.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

@Configuration
@Schema(description = "Web配置类，定义了RestTemplate Bean，用于与外部服务进行HTTP通信，并设置了连接和读取超时时间")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class WebConfig implements WebMvcConfigurer {

    @Bean
    @Schema(description = "获取RestTemplate实例，用于与外部服务进行HTTP通信")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    public RestTemplate restTemplate(RestTemplateBuilder builder, AiProperties aiProperties) {
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds(aiProperties));
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Schema(description = "解析超时时间，单位为秒")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    private long resolveTimeoutSeconds(AiProperties aiProperties) {
        Long configured = aiProperties.getTimeoutSeconds();
        return configured == null || configured <= 0 ? 60L : configured;
    }
}
