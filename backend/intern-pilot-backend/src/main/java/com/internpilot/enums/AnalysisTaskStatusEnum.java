package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "分析任务状态枚举，定义简历分析任务的不同状态，每个状态对应一个唯一的code、描述和默认进度值")
public enum AnalysisTaskStatusEnum {//分析任务状态枚举，定义简历分析任务的不同状态，
// 每个状态对应一个唯一的code、描述和默认进度值

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
    // 这个静态方法根据输入的code返回对应的枚举实例，如果code是"SUCCESS"，
    // 则返回COMPLETED状态，
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
    // 这个方法判断当前状态是否是一个终止状态，COMPLETED和FAILED都被认为是终止状态，
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
