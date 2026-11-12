package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.captcha")
public class CaptchaProperties {

    private String mode = "mock";

    private String emailProvider = "mock";

    private String smsProvider = "disabled";

    private int ttlSeconds = 300;

    private int cooldownSeconds = 60;

    private int maxFailCount = 5;

    private int dailyLimit = 10;
}
