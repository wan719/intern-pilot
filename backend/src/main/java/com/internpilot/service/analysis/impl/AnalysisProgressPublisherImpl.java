package com.internpilot.service.analysis.impl;

import com.internpilot.constant.RedisKeyConstants;
import com.internpilot.service.analysis.AnalysisProgressPublisher;
import com.internpilot.vo.analysis.AnalysisProgressMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisProgressPublisherImpl implements AnalysisProgressPublisher {

    private static final Duration REDIS_TTL = Duration.ofHours(24);

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    @Override
    public void publish(
            String taskNo,
            Long userId,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisProgressMessage progressMessage = AnalysisProgressMessage.of(
                taskNo,
                userId,
                status,
                progress,
                message,
                reportId,
                errorMessage
        );

        messagingTemplate.convertAndSend(
                "/topic/analysis/" + taskNo,
                progressMessage
        );

        try {
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.analysisTask(taskNo),
                    progressMessage,
                    REDIS_TTL
            );
        } catch (Exception e) {
            log.warn("Failed to save task status to Redis for taskNo={}: {}", taskNo, e.getMessage());
        }
    }
}
