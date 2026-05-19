package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "反馈类型枚举")
public enum FeedbackTypeEnum {

    BUG("BUG", "功能异常"),
    SUGGESTION("SUGGESTION", "使用建议"),
    UI_UX("UI_UX", "页面体验问题"),
    AI_RESULT("AI_RESULT", "AI 结果不准确"),
    PERFORMANCE("PERFORMANCE", "系统响应太慢"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    FeedbackTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static FeedbackTypeEnum fromCode(String code) {
        for (FeedbackTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}