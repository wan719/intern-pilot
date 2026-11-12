package com.internpilot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.annotation.OperationLog;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 1000;
    private static final int MAX_ERROR_LENGTH = 2000;
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(?i)(password|token|api[-_]?key|secret|authorization|jwt|mailPassword|emailAuthorizationCode|prompt|resume|content|jdContent|parsedText|rawAiResponse)");

    private final SystemOperationLogMapper systemOperationLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object recordOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        SystemOperationLog operationLogEntity = new SystemOperationLog();
        fillBasicInfo(operationLogEntity, operationLog, joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            operationLogEntity.setSuccess(1);
            operationLogEntity.setCostTime(System.currentTimeMillis() - startTime);
            safeInsert(operationLogEntity);
            return result;
        } catch (Throwable ex) {
            operationLogEntity.setSuccess(0);
            operationLogEntity.setErrorMessage(truncate(sanitizeText(ex.getMessage()), MAX_ERROR_LENGTH));
            operationLogEntity.setCostTime(System.currentTimeMillis() - startTime);
            safeInsert(operationLogEntity);
            throw ex;
        }
    }

    private void fillBasicInfo(SystemOperationLog operationLogEntity, OperationLog operationLog, Object[] args) {
        operationLogEntity.setModule(operationLog.module());
        operationLogEntity.setOperation(operationLog.operation());
        operationLogEntity.setOperationType(operationLog.type().getCode());

        fillUserInfo(operationLogEntity, args);
        fillRequestInfo(operationLogEntity, operationLog.recordParams(), args);
    }

    private void fillUserInfo(SystemOperationLog operationLogEntity, Object[] args) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            operationLogEntity.setOperatorId(userDetails.getUserId());
            operationLogEntity.setOperatorUsername(userDetails.getUsername());
        } else if (principal instanceof String username && !"anonymousUser".equals(username)) {
            operationLogEntity.setOperatorUsername(username);
        }

        if (operationLogEntity.getOperatorUsername() == null) {
            operationLogEntity.setOperatorUsername(inferUsernameFromArgs(args));
        }
    }

    private String inferUsernameFromArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                Method getUsername = arg.getClass().getMethod("getUsername");
                Object value = getUsername.invoke(arg);
                if (value instanceof String username && !username.isBlank()) {
                    return username;
                }
            } catch (ReflectiveOperationException ignored) {
                // Request objects without username are expected for most endpoints.
            }
        }
        return null;
    }

    private void fillRequestInfo(SystemOperationLog operationLogEntity, boolean recordParams, Object[] args) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        operationLogEntity.setRequestUri(request.getRequestURI());
        operationLogEntity.setRequestMethod(request.getMethod());
        operationLogEntity.setIpAddress(getClientIp(request));
        operationLogEntity.setUserAgent(truncate(request.getHeader("User-Agent"), 500));

        if (recordParams) {
            operationLogEntity.setRequestParams(extractRequestParams(request, args));
        }
    }

    private String extractRequestParams(HttpServletRequest request, Object[] args) {
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            request.getParameterMap().forEach((key, value) -> params.put(key, sanitizeValue(key, Arrays.toString(value))));
            Object[] safeArgs = Arrays.stream(args == null ? new Object[0] : args)
                    .filter(arg -> !(arg instanceof MultipartFile))
                    .filter(arg -> !(arg instanceof HttpServletRequest))
                    .map(this::sanitizeObject)
                    .toArray();
            if (safeArgs.length > 0) {
                params.put("body", safeArgs);
            }
            return truncate(sanitizeText(objectMapper.writeValueAsString(params)), MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return null;
        }
    }

    private Object sanitizeObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return truncate(sanitizeText(text), 120);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(value, Map.class);
            Map<String, Object> safe = new LinkedHashMap<>();
            map.forEach((key, item) -> safe.put(key, sanitizeValue(key, item)));
            return safe;
        } catch (Exception e) {
            return value.getClass().getSimpleName();
        }
    }

    private Object sanitizeValue(String key, Object value) {
        if (key != null && SENSITIVE_FIELD_PATTERN.matcher(key).find()) {
            return "[MASKED]";
        }
        if (value instanceof String text) {
            return truncate(sanitizeText(text), 120);
        }
        return value;
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

    private String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._\\-]+", "$1[MASKED]")
                .replaceAll("(?i)(api[-_]?key\\s*[=:]\\s*)[^,}\\s]+", "$1[MASKED]")
                .replaceAll("(?i)(password\\s*[=:]\\s*)[^,}\\s]+", "$1[MASKED]")
                .replaceAll("(?i)(token\\s*[=:]\\s*)[^,}\\s]+", "$1[MASKED]");
    }

    private void safeInsert(SystemOperationLog operationLogEntity) {
        try {
            systemOperationLogMapper.insert(operationLogEntity);
        } catch (Exception e) {
            log.warn("Failed to persist operation log. module={}, operation={}, success={}, reason={}",
                    operationLogEntity.getModule(), operationLogEntity.getOperation(),
                    operationLogEntity.getSuccess(), e.getMessage());
        }
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
