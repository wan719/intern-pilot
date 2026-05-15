package com.internpilot.exception;

import com.internpilot.common.ResultCode;

/**
 * 自定义异常类：文件解析异常
 * 继承自BusinessException，用于处理文件解析过程中出现的错误
 */
public class FileParseException extends BusinessException {

    /**
     * 构造函数：创建文件解析异常实例
     * @param message 异常信息描述，用于说明具体的错误原因
     */
    public FileParseException(String message) {
        super(ResultCode.FILE_PROCESS_ERROR, message);
    }
}
//super函数调用父类BusinessException的构造函数，
// 传入ResultCode.FILE_PROCESS_ERROR作为错误代码，以及message作为错误信息描述。
// 这样可以确保在抛出FileParseException时，能够提供统一的错误代码和详细的错误信息，
// 方便前端进行错误处理和用户提示。