package com.internpilot.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.internpilot.config.SystemCleanupProperties;
import com.internpilot.entity.AnalysisTask;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.mapper.AnalysisTaskMapper;
import com.internpilot.mapper.SystemOperationLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemCleanupTaskTest {

    @Mock
    private SystemCleanupProperties properties;

    @Mock
    private AnalysisTaskMapper analysisTaskMapper;

    @Mock
    private SystemOperationLogMapper systemOperationLogMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private SystemCleanupTask task;

    @Test
    void cleanupShouldSoftDeleteExpiredDataAndRedisKeysWithoutTtl() {
        when(properties.getAnalysisTaskRetentionDays()).thenReturn(30);
        when(properties.getOperationLogRetentionDays()).thenReturn(90);
        when(redisTemplate.keys("internpilot:ai:analysis:task:*"))
                .thenReturn(Set.of("internpilot:ai:analysis:task:1", "internpilot:ai:analysis:task:2"));
        when(redisTemplate.getExpire("internpilot:ai:analysis:task:1")).thenReturn(-1L);
        when(redisTemplate.getExpire("internpilot:ai:analysis:task:2")).thenReturn(120L);

        task.cleanup();

        verify(analysisTaskMapper).update(any(AnalysisTask.class), any(LambdaUpdateWrapper.class));
        verify(systemOperationLogMapper).update(any(SystemOperationLog.class), any(LambdaUpdateWrapper.class));
        verify(redisTemplate).delete("internpilot:ai:analysis:task:1");
        verify(redisTemplate, never()).delete("internpilot:ai:analysis:task:2");
    }

    @Test
    void cleanupShouldSwallowMapperAndRedisFailures() {
        when(properties.getAnalysisTaskRetentionDays()).thenReturn(30);
        doThrow(new RuntimeException("database down"))
                .when(analysisTaskMapper).update(any(AnalysisTask.class), any(LambdaUpdateWrapper.class));

        task.cleanup();

        verify(systemOperationLogMapper, never()).update(any(SystemOperationLog.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void cleanupShouldIgnoreRedisScanFailure() {
        when(properties.getAnalysisTaskRetentionDays()).thenReturn(30);
        when(properties.getOperationLogRetentionDays()).thenReturn(90);
        doThrow(new RuntimeException("redis down")).when(redisTemplate).keys("internpilot:ai:analysis:task:*");

        task.cleanup();

        verify(analysisTaskMapper).update(any(AnalysisTask.class), any(LambdaUpdateWrapper.class));
        verify(systemOperationLogMapper).update(any(SystemOperationLog.class), any(LambdaUpdateWrapper.class));
    }
}
