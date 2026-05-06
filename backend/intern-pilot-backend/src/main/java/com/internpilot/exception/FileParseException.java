package com.internpilot.exception;

import com.internpilot.common.ResultCode;

public class FileParseException extends BusinessException {

    public FileParseException(String message) {
        super(ResultCode.FILE_PROCESS_ERROR, message);
    }
}