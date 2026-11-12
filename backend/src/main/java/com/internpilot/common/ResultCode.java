package com.internpilot.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),

    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),

    INTERNAL_ERROR(500, "系统内部错误"),

    AI_SERVICE_ERROR(600, "AI 服务异常"),
    FILE_PROCESS_ERROR(700, "文件处理异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
