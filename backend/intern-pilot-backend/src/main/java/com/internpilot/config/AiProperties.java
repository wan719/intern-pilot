package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider;

    private String apiKey;

    private String baseUrl;

    private String model;

    private Long timeout;
}
