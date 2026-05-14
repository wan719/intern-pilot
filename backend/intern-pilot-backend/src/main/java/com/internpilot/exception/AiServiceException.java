package com.internpilot.exception;

import com.internpilot.common.ResultCode;
import lombok.Getter;

@Getter
public class AiServiceException extends BusinessException {

    private final String errorCode;

    public AiServiceException(String message) {
        super(ResultCode.AI_SERVICE_ERROR, message);
        this.errorCode = "AI_ANALYSIS_FAILED";
    }

    public AiServiceException(String errorCode, String message) {
        super(ResultCode.AI_SERVICE_ERROR, message);
        this.errorCode = errorCode;
    }
}