package com.internpilot.service.impl;

import com.internpilot.enums.AnalysisTaskStatusEnum;
import com.internpilot.vo.analysis.AnalysisProgressMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisProgressPublisherImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Test
    void publishShouldSendWebSocketMessageAndSaveRedisStatus() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AnalysisProgressPublisherImpl publisher = new AnalysisProgressPublisherImpl(
                messagingTemplate,
                redisTemplate
        );

        publisher.publish(
                "TASK_20260514_abcd1234",
                7L,
                AnalysisTaskStatusEnum.CALLING_AI.getCode(),
                AnalysisTaskStatusEnum.CALLING_AI.getDefaultProgress(),
                "正在调用 AI 模型生成分析",
                null,
                null
        );

        ArgumentCaptor<AnalysisProgressMessage> messageCaptor =
                ArgumentCaptor.forClass(AnalysisProgressMessage.class);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/analysis/TASK_20260514_abcd1234"),
                messageCaptor.capture()
        );

        AnalysisProgressMessage message = messageCaptor.getValue();
        assertEquals("TASK_20260514_abcd1234", message.getTaskNo());
        assertEquals(7L, message.getUserId());
        assertEquals("CALLING_AI", message.getStatus());
        assertEquals(60, message.getProgress());
        assertEquals("正在调用 AI 模型生成分析", message.getMessage());
        assertNotNull(message.getTimestamp());

        verify(valueOperations).set(
                "ai:analysis:task:TASK_20260514_abcd1234",
                message,
                Duration.ofHours(24)
        );
    }

    @Test
    void publishFailedShouldIncludeErrorMessage() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AnalysisProgressPublisherImpl publisher = new AnalysisProgressPublisherImpl(
                messagingTemplate,
                redisTemplate
        );

        publisher.publish(
                "TASK_20260514_failed",
                8L,
                AnalysisTaskStatusEnum.FAILED.getCode(),
                35,
                "分析失败",
                null,
                "AI 服务调用超时"
        );

        ArgumentCaptor<AnalysisProgressMessage> messageCaptor =
                ArgumentCaptor.forClass(AnalysisProgressMessage.class);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/analysis/TASK_20260514_failed"),
                messageCaptor.capture()
        );

        AnalysisProgressMessage message = messageCaptor.getValue();
        assertEquals("FAILED", message.getStatus());
        assertEquals("AI 服务调用超时", message.getErrorMessage());
    }
}
