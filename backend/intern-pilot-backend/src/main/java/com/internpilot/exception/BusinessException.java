package com.internpilot.exception;

import com.internpilot.common.ResultCode;
import lombok.Getter;

/**
 * 自定义业务异常类，继承自RuntimeException
 * 使用@Getter注解自动为所有字段生成getter方法
 */
@Getter
public class BusinessException extends RuntimeException {

    // 错误码，使用final修饰，确保不可变性
    private final Integer code;

    /**
     * 构造方法1：只提供错误信息
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    /**
     * 构造方法2：提供ResultCode
     * @param resultCode 结果代码枚举，包含错误码和错误信息
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 构造方法3：同时提供ResultCode和自定义错误信息
     * @param resultCode 结果代码枚举
     * @param message 自定义错误信息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
//RuntimeException，是Java中所有未检查异常的父类，表示程序运行过程中可能发生的异常情况。
// 通过继承RuntimeException，BusinessException成为一个自定义的未检查异常类，
// 可以在程序中抛出并捕获，以处理特定的业务错误情况。
//使用@Getter注解，Lombok会自动为BusinessException类中的所有字段生成getter方法，
// 这样在其他地方可以通过调用getCode()方法来获取异常的错误码，