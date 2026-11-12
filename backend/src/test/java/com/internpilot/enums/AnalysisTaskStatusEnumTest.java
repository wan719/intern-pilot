package com.internpilot.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisTaskStatusEnumTest {

    @Test
    void shouldHaveEightStatuses() {
        assertEquals(8, AnalysisTaskStatusEnum.values().length);
    }

    @Test
    void pendingShouldNotBeTerminal() {
        assertFalse(AnalysisTaskStatusEnum.PENDING.isTerminal());
    }

    @Test
    void parsingResumeShouldNotBeTerminal() {
        assertFalse(AnalysisTaskStatusEnum.PARSING_RESUME.isTerminal());
    }

    @Test
    void buildingContextShouldNotBeTerminal() {
        assertFalse(AnalysisTaskStatusEnum.BUILDING_CONTEXT.isTerminal());
    }

    @Test
    void callingAiShouldNotBeTerminal() {
        assertFalse(AnalysisTaskStatusEnum.CALLING_AI.isTerminal());
    }

    @Test
    void generatingReportShouldNotBeTerminal() {
        assertFalse(AnalysisTaskStatusEnum.GENERATING_REPORT.isTerminal());
    }

    @Test
    void completedShouldBeTerminal() {
        assertTrue(AnalysisTaskStatusEnum.COMPLETED.isTerminal());
    }

    @Test
    void failedShouldBeTerminal() {
        assertTrue(AnalysisTaskStatusEnum.FAILED.isTerminal());
    }

    @Test
    void cancelledShouldBeTerminal() {
        assertTrue(AnalysisTaskStatusEnum.CANCELLED.isTerminal());
    }

    @Test
    void fromCodeShouldReturnCorrectEnum() {
        assertEquals(AnalysisTaskStatusEnum.PENDING, AnalysisTaskStatusEnum.fromCode("PENDING"));
        assertEquals(AnalysisTaskStatusEnum.PARSING_RESUME, AnalysisTaskStatusEnum.fromCode("PARSING_RESUME"));
        assertEquals(AnalysisTaskStatusEnum.BUILDING_CONTEXT, AnalysisTaskStatusEnum.fromCode("BUILDING_CONTEXT"));
        assertEquals(AnalysisTaskStatusEnum.CALLING_AI, AnalysisTaskStatusEnum.fromCode("CALLING_AI"));
        assertEquals(AnalysisTaskStatusEnum.GENERATING_REPORT, AnalysisTaskStatusEnum.fromCode("GENERATING_REPORT"));
        assertEquals(AnalysisTaskStatusEnum.COMPLETED, AnalysisTaskStatusEnum.fromCode("COMPLETED"));
        assertEquals(AnalysisTaskStatusEnum.COMPLETED, AnalysisTaskStatusEnum.fromCode("SUCCESS"));
        assertEquals(AnalysisTaskStatusEnum.FAILED, AnalysisTaskStatusEnum.fromCode("FAILED"));
        assertEquals(AnalysisTaskStatusEnum.CANCELLED, AnalysisTaskStatusEnum.fromCode("CANCELLED"));
    }

    @Test
    void fromCodeShouldReturnNullForUnknownCode() {
        assertNull(AnalysisTaskStatusEnum.fromCode("UNKNOWN"));
        assertNull(AnalysisTaskStatusEnum.fromCode("RUNNING"));
    }

    @Test
    void defaultProgressShouldBeCorrect() {
        assertEquals(0, AnalysisTaskStatusEnum.PENDING.getDefaultProgress());
        assertEquals(15, AnalysisTaskStatusEnum.PARSING_RESUME.getDefaultProgress());
        assertEquals(35, AnalysisTaskStatusEnum.BUILDING_CONTEXT.getDefaultProgress());
        assertEquals(60, AnalysisTaskStatusEnum.CALLING_AI.getDefaultProgress());
        assertEquals(85, AnalysisTaskStatusEnum.GENERATING_REPORT.getDefaultProgress());
        assertEquals(100, AnalysisTaskStatusEnum.COMPLETED.getDefaultProgress());
        assertEquals(100, AnalysisTaskStatusEnum.FAILED.getDefaultProgress());
        assertEquals(100, AnalysisTaskStatusEnum.CANCELLED.getDefaultProgress());
    }

    @Test
    void codeShouldMatchEnumName() {
        for (AnalysisTaskStatusEnum status : AnalysisTaskStatusEnum.values()) {
            assertEquals(status.name(), status.getCode());
        }
    }
}
