package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "system.cleanup")
public class SystemCleanupProperties {

    private boolean enabled = true;

    private int analysisTaskRetentionDays = 30;

    private int operationLogRetentionDays = 90;

    private int redisTaskNotificationRetentionHours = 24;
}
