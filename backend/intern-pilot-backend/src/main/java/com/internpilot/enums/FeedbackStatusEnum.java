package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "反馈状态枚举")
public enum FeedbackStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RESOLVED("RESOLVED", "已解决"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String description;

    FeedbackStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static FeedbackStatusEnum fromCode(String code) {
        for (FeedbackStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}