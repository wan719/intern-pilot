package com.internpilot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.annotation.OperationLog;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 1000;
    private static final int MAX_ERROR_LENGTH = 2000;

    private final SystemOperationLogMapper systemOperationLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object recordOperationLog(
            ProceedingJoinPoint joinPoint,
            OperationLog operationLog
    ) throws Throwable {

        long startTime = System.currentTimeMillis();

        SystemOperationLog log = new SystemOperationLog();
        fillBasicInfo(log, operationLog);

        Object result;
        try {
            result = joinPoint.proceed();

            log.setSuccess(1);
            log.setCostTime(System.currentTimeMillis() - startTime);
            systemOperationLogMapper.insert(log);

            return result;

        } catch (Throwable ex) {
            log.setSuccess(0);
            log.setErrorMessage(truncate(ex.getMessage(), MAX_ERROR_LENGTH));
            log.setCostTime(System.currentTimeMillis() - startTime);
            systemOperationLogMapper.insert(log);

            throw ex;
        }
    }

    private void fillBasicInfo(SystemOperationLog log, OperationLog operationLog) {
        log.setModule(operationLog.module());
        log.setOperation(operationLog.operation());
        log.setOperationType(operationLog.type().getCode());

        fillUserInfo(log);
        fillRequestInfo(log, operationLog.recordParams());
    }

    private void fillUserInfo(SystemOperationLog log) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            log.setOperatorId(userDetails.getUserId());
            log.setOperatorUsername(userDetails.getUsername());
        } else if (principal instanceof String username) {
            log.setOperatorUsername(username);
        }
    }

    private void fillRequestInfo(SystemOperationLog log, boolean recordParams) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();

        log.setRequestUri(request.getRequestURI());
        log.setRequestMethod(request.getMethod());
        log.setIpAddress(getClientIp(request));
        log.setUserAgent(truncate(request.getHeader("User-Agent"), 500));

        if (recordParams) {
            log.setRequestParams(extractRequestParams(request));
        }
    }

    private String extractRequestParams(HttpServletRequest request) {
        try {
            String params = objectMapper.writeValueAsString(request.getParameterMap());
            return truncate(params, MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}