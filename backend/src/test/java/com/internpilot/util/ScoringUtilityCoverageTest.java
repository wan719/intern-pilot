package com.internpilot.util;

import com.internpilot.enums.MatchLevelEnum;
import com.internpilot.enums.RecommendationLevelEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringUtilityCoverageTest {

    @Test
    void recommendationLevelShouldCoverEveryScoreRange() {
        assertEquals("HIGH", RecommendationLevelEnum.ofScore(90));
        assertEquals("MEDIUM_HIGH", RecommendationLevelEnum.ofScore(70));
        assertEquals("MEDIUM", RecommendationLevelEnum.ofScore(60));
        assertEquals("LOW", RecommendationLevelEnum.ofScore(40));
        assertEquals("NOT_RECOMMENDED", RecommendationLevelEnum.ofScore(39));
        assertEquals("NOT_RECOMMENDED", RecommendationLevelEnum.ofScore(null));
        assertFalse(RecommendationLevelEnum.HIGH.getDescription().isBlank());
    }

    @Test
    void matchLevelShouldCoverEveryScoreRange() {
        assertEquals("HIGH", MatchLevelEnum.fromScore(85));
        assertEquals("MEDIUM_HIGH", MatchLevelEnum.fromScore(70));
        assertEquals("MEDIUM", MatchLevelEnum.fromScore(60));
        assertEquals("LOW", MatchLevelEnum.fromScore(40));
        assertEquals("VERY_LOW", MatchLevelEnum.fromScore(10));
        assertEquals("MEDIUM", MatchLevelEnum.fromScore(null));
        assertFalse(MatchLevelEnum.HIGH.getDescription().isBlank());
    }

    @Test
    void textChunkUtilsShouldSplitBlankShortAndLongContent() {
        assertTrue(TextChunkUtils.splitToChunks(null).isEmpty());
        assertTrue(TextChunkUtils.splitToChunks("   ").isEmpty());
        assertEquals(List.of("short text"), TextChunkUtils.splitToChunks("short text"));

        String longParagraph = "a".repeat(1700);
        List<String> chunks = TextChunkUtils.splitToChunks(longParagraph);

        assertEquals(3, chunks.size());
        assertEquals(800, chunks.get(0).length());
        assertEquals(800, chunks.get(1).length());
        assertEquals(100, chunks.get(2).length());
    }

    @Test
    void textChunkUtilsShouldMergeTrailingSmallParagraph() {
        String first = "a".repeat(120);
        String second = "b".repeat(20);

        List<String> chunks = TextChunkUtils.splitToChunks(first + "\n\n" + second);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains(first));
        assertTrue(chunks.get(0).contains(second));
    }
}
