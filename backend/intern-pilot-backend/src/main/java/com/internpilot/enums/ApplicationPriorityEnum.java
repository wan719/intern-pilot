package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "申请优先级枚举，定义申请的优先级别，每个优先级对应一个唯一的code和描述")
public enum ApplicationPriorityEnum {//申请优先级枚举，定义申请的优先级别，
// 每个优先级对应一个唯一的code和描述

    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String description;

    ApplicationPriorityEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    // 这个静态方法根据输入的code返回对应的枚举实例，如果code是null或空字符串，则返回false，
    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (ApplicationPriorityEnum priority : values()) {
            if (priority.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
