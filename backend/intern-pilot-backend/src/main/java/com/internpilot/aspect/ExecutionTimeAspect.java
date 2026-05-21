package com.internpilot.aspect;

import com.internpilot.annotation.LogExecutionTime;
import com.internpilot.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {

    @Value("${system.execution-time.warn-threshold-ms:1500}")
    private long warnThresholdMs;

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String operation = logExecutionTime.value().isBlank()
                    ? joinPoint.getSignature().toShortString()
                    : logExecutionTime.value();
            HttpServletRequest request = currentRequest();
            Long userId = currentUserId();
            String uri = request == null ? "" : request.getRequestURI();
            String method = request == null ? "" : request.getMethod();

            if (durationMs >= warnThresholdMs) {
                log.warn("api execution slow: operation={}, userId={}, method={}, uri={}, durationMs={}, success={}",
                        operation, userId, method, uri, durationMs, success);
            } else {
                log.info("api execution: operation={}, userId={}, method={}, uri={}, durationMs={}, success={}",
                        operation, userId, method, uri, durationMs, success);
            }
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }
}
