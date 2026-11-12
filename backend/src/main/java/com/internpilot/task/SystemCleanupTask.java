package com.internpilot.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.internpilot.config.SystemCleanupProperties;
import com.internpilot.constant.RedisKeyConstants;
import com.internpilot.entity.AnalysisTask;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.enums.AnalysisTaskStatusEnum;
import com.internpilot.mapper.AnalysisTaskMapper;
import com.internpilot.mapper.SystemOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "system.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SystemCleanupTask {

    private final SystemCleanupProperties properties;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final SystemOperationLogMapper systemOperationLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(cron = "${system.cleanup.cron:0 30 3 * * *}")
    public void cleanup() {
        try {
            cleanupAnalysisTasks();
            cleanupOperationLogs();
            cleanupExpiredRedisTaskNotifications();
        } catch (Exception e) {
            log.warn("System cleanup task failed: {}", e.getMessage());
        }
    }

    private void cleanupAnalysisTasks() {
        LocalDateTime expireBefore = LocalDateTime.now().minusDays(properties.getAnalysisTaskRetentionDays());
        AnalysisTask update = new AnalysisTask();
        update.setDeleted(1);
        int count = analysisTaskMapper.update(update, new LambdaUpdateWrapper<AnalysisTask>()
                .eq(AnalysisTask::getDeleted, 0)
                .lt(AnalysisTask::getCreatedAt, expireBefore)
                .in(AnalysisTask::getStatus,
                        AnalysisTaskStatusEnum.COMPLETED.getCode(),
                        AnalysisTaskStatusEnum.FAILED.getCode(),
                        AnalysisTaskStatusEnum.CANCELLED.getCode()));
        log.info("System cleanup analysis tasks completed. deleted={}, expireBefore={}", count, expireBefore);
    }

    private void cleanupOperationLogs() {
        LocalDateTime expireBefore = LocalDateTime.now().minusDays(properties.getOperationLogRetentionDays());
        SystemOperationLog update = new SystemOperationLog();
        update.setDeleted(1);
        int count = systemOperationLogMapper.update(update, new LambdaUpdateWrapper<SystemOperationLog>()
                .eq(SystemOperationLog::getDeleted, 0)
                .lt(SystemOperationLog::getCreatedAt, expireBefore));
        log.info("System cleanup operation logs completed. deleted={}, expireBefore={}", count, expireBefore);
    }

    private void cleanupExpiredRedisTaskNotifications() {
        try {
            Set<String> keys = redisTemplate.keys(RedisKeyConstants.AI_ANALYSIS_TASK_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            int deleted = 0;
            for (String key : keys) {
                Long expireSeconds = redisTemplate.getExpire(key);
                if (expireSeconds != null && expireSeconds < 0) {
                    redisTemplate.delete(key);
                    deleted++;
                }
            }
            log.info("System cleanup Redis task notifications completed. deleted={}", deleted);
        } catch (Exception e) {
            log.warn("System cleanup Redis task notifications skipped: {}", e.getMessage());
        }
    }
}
