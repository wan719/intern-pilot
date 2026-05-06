package com.internpilot.util;

import com.internpilot.dto.analysis.AiAnalysisResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilsTest {

    @Test
    void parseAiJsonShouldHandleMarkdownCodeFence() {
        String raw = """
                ```json
                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": ["A"],
                  "weaknesses": ["B"],
                  "missingSkills": ["C"],
                  "suggestions": ["D"],
                  "interviewTips": ["E"]
                }
                ```
                """;

        AiAnalysisResult result = JsonUtils.parseAiJson(raw, AiAnalysisResult.class);

        assertEquals(82, result.getMatchScore());
        assertEquals("MEDIUM_HIGH", result.getMatchLevel());
        assertEquals(List.of("A"), result.getStrengths());
    }

    @Test
    void toStringListShouldReturnTypedList() {
        List<String> list = JsonUtils.toStringList("[\"Java\",\"Redis\"]");
        assertEquals(List.of("Java", "Redis"), list);
    }
}
