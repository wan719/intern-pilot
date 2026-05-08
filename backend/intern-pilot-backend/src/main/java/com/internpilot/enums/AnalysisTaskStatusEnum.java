package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum AnalysisTaskStatusEnum {

    PENDING("PENDING", "等待执行"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败");

    private final String code;
    private final String description;

    AnalysisTaskStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
