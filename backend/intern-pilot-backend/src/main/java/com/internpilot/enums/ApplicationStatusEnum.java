package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatusEnum {

    TO_APPLY("TO_APPLY", "待投递"),
    APPLIED("APPLIED", "已投递"),
    WRITTEN_TEST("WRITTEN_TEST", "笔试中"),
    FIRST_INTERVIEW("FIRST_INTERVIEW", "一面"),
    SECOND_INTERVIEW("SECOND_INTERVIEW", "二面"),
    HR_INTERVIEW("HR_INTERVIEW", "HR面"),
    OFFER("OFFER", "已Offer"),
    REJECTED("REJECTED", "被拒"),
    GIVEN_UP("GIVEN_UP", "放弃");

    private final String code;
    private final String description;

    ApplicationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (ApplicationStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
