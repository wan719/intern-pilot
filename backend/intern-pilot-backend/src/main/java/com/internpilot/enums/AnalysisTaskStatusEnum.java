package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum AnalysisTaskStatusEnum {

    PENDING("PENDING", "等待执行", 0),
    PARSING_RESUME("PARSING_RESUME", "正在解析简历", 15),
    BUILDING_CONTEXT("BUILDING_CONTEXT", "正在构建上下文", 35),
    CALLING_AI("CALLING_AI", "正在调用 AI", 60),
    GENERATING_REPORT("GENERATING_REPORT", "正在生成报告", 85),
    COMPLETED("COMPLETED", "分析完成", 100),
    FAILED("FAILED", "分析失败", 100);

    private final String code;
    private final String description;
    private final int defaultProgress;

    AnalysisTaskStatusEnum(String code, String description, int defaultProgress) {
        this.code = code;
        this.description = description;
        this.defaultProgress = defaultProgress;
    }

    public static AnalysisTaskStatusEnum fromCode(String code) {
        if ("SUCCESS".equals(code)) {
            return COMPLETED;
        }
        for (AnalysisTaskStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
