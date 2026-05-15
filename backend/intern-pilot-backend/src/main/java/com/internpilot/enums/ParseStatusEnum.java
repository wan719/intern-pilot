package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "解析状态枚举，定义简历解析的不同状态，每个状态对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum ParseStatusEnum {//解析状态枚举，定义简历解析的不同状态，
// 每个状态对应一个唯一的code和描述

    SUCCESS("SUCCESS", "解析成功"),
    FAILED("FAILED", "解析失败"),
    PENDING("PENDING", "等待解析");

    private final String code;
    private final String description;

    ParseStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
