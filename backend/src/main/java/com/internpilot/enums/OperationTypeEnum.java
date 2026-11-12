package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "操作类型枚举，定义系统中不同的操作类型，每个类型对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum OperationTypeEnum {//操作类型枚举，定义系统中不同的操作类型，
// 每个类型对应一个唯一的code和描述

    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "退出"),
    UPLOAD("UPLOAD", "上传"),
    DOWNLOAD("DOWNLOAD", "下载"),
    AI("AI", "AI操作"),
    GRANT("GRANT", "授权"),
    DISABLE("DISABLE", "禁用"),
    ENABLE("ENABLE", "启用"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    OperationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}