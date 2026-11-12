package com.internpilot.exception;

import com.internpilot.common.ResultCode;
import lombok.Getter;

/**
 * AiServiceException类，继承自BusinessException
 * 用于处理AI服务相关的异常情况
 * 使用@Getter注解自动生成getter方法
 */
@Getter
public class AiServiceException extends BusinessException {

    // 错误代码，使用final修饰，表示一旦赋值后不可更改
    private final String errorCode;

    /**
     * 构造方法1：使用默认错误代码"AI_ANALYSIS_FAILED"
     * @param message 异常信息
     */
    public AiServiceException(String message) {
        super(ResultCode.AI_SERVICE_ERROR, message);
        this.errorCode = "AI_ANALYSIS_FAILED";
    }

    /**
     * 构造方法2：允许自定义错误代码
     * @param errorCode 自定义错误代码
     * @param message 异常信息
     */
    public AiServiceException(String errorCode, String message) {
        super(ResultCode.AI_SERVICE_ERROR, message);
        this.errorCode = errorCode;
    }
}