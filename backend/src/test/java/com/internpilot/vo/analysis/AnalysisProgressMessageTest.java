package com.internpilot.vo.analysis;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnalysisProgressMessageTest {

    @Test
    void ofShouldCreateMessageWithAllFields() {
        AnalysisProgressMessage message = AnalysisProgressMessage.of(
                "TASK_20240101_abc123",
                1L,
                "PARSING_RESUME",
                15,
                "正在解析简历内容",
                100L,
                null
        );

        assertEquals("TASK_20240101_abc123", message.getTaskNo());
        assertEquals(1L, message.getUserId());
        assertEquals("PARSING_RESUME", message.getStatus());
        assertEquals(15, message.getProgress());
        assertEquals("正在解析简历内容", message.getMessage());
        assertEquals(100L, message.getReportId());
        assertNull(message.getErrorMessage());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void ofShouldCreateFailedMessage() {
        AnalysisProgressMessage message = AnalysisProgressMessage.of(
                "TASK_20240101_def456",
                2L,
                "FAILED",
                35,
                "分析失败",
                null,
                "AI 服务调用超时"
        );

        assertEquals("TASK_20240101_def456", message.getTaskNo());
        assertEquals(2L, message.getUserId());
        assertEquals("FAILED", message.getStatus());
        assertEquals(35, message.getProgress());
        assertEquals("分析失败", message.getMessage());
        assertNull(message.getReportId());
        assertEquals("AI 服务调用超时", message.getErrorMessage());
    }

    @Test
    void ofShouldSetCurrentTimestamp() {
        LocalDateTime before = LocalDateTime.now();
        AnalysisProgressMessage message = AnalysisProgressMessage.of(
                "TASK_001", 1L, "PENDING", 0, "任务已创建", null, null
        );
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(message.getTimestamp());
        assertFalse(message.getTimestamp().isBefore(before.minusSeconds(1)));
        assertFalse(message.getTimestamp().isAfter(after.plusSeconds(1)));
    }

    @Test
    void ofShouldHandleNullReportIdAndErrorMessage() {
        AnalysisProgressMessage message = AnalysisProgressMessage.of(
                "TASK_001", 1L, "PENDING", 0, "任务已创建", null, null
        );

        assertNull(message.getReportId());
        assertNull(message.getErrorMessage());
    }
}
