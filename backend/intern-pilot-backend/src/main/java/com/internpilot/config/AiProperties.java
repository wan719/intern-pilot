package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider = "deepseek";

    private String apiKey;

    private String baseUrl = "https://api.deepseek.com";

    private String model = "deepseek-v4-flash";

    private String proModel = "deepseek-v4-pro";

    private Long timeoutSeconds = 60L;
}
