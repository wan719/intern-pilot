package com.internpilot.exception;

import com.internpilot.common.ResultCode;

public class AiServiceException extends BusinessException {

    public AiServiceException(String message) {
        super(ResultCode.AI_SERVICE_ERROR, message);
    }
}