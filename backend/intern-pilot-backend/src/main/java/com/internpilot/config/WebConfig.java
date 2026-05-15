package com.internpilot.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, AiProperties aiProperties) {
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds(aiProperties));
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    private long resolveTimeoutSeconds(AiProperties aiProperties) {
        Long configured = aiProperties.getTimeoutSeconds();
        return configured == null || configured <= 0 ? 60L : configured;
    }
}
