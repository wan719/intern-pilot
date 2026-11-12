package com.internpilot.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class AiAnalysisResult {

    private Integer matchScore;

    private String matchLevel;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> missingSkills;

    private List<String> suggestions;

    private List<String> interviewTips;
}
