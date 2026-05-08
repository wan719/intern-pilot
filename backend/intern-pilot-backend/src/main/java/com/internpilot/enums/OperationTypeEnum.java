package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum OperationTypeEnum {

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