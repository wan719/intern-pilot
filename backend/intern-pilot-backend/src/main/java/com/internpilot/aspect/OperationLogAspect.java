package com.internpilot.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.annotation.OperationLog;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import java.lang.reflect.Method;

@Aspect//这个注解表示OperationLogAspect是一个切面类，使用Spring AOP实现对标记了@OperationLog注解的方法进行切面处理，记录操作日志到数据库
@Component//这个注解表示OperationLogAspect是一个Spring组件，会被Spring容器管理和自动扫描到
@RequiredArgsConstructor
@Schema(description = "操作日志切面类，使用Spring AOP实现对标记了@OperationLog注解的方法进行切面处理，记录操作日志到数据库")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 1000;//请求参数最大长度，
    // 超过该长度的参数会被截断，以避免日志表中存储过长的数据
    private static final int MAX_ERROR_LENGTH = 2000;//错误信息最大长度，
    // 超过该长度的错误信息会被截断，以避免日志表中存储过长的数据

    private final SystemOperationLogMapper systemOperationLogMapper;//操作日志Mapper，
    // 用于将操作日志记录到数据库中
    // ObjectMapper实例，用于将请求参数转换为JSON字符串，方便存储到数据库中
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Schema(description = "记录操作日志的方法，使用@Around注解定义一个环绕通知，拦截所有标记了@OperationLog注解的方法，在方法执行前后记录操作日志到数据库中")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    @Around("@annotation(operationLog)")
    public Object recordOperationLog(
            ProceedingJoinPoint joinPoint,
            OperationLog operationLog
    ) throws Throwable {

        long startTime = System.currentTimeMillis();

        SystemOperationLog log = new SystemOperationLog();
        fillBasicInfo(log, operationLog, joinPoint.getArgs());

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
    @Schema(description = "填充操作日志的基本信息，包括操作模块、操作名称、操作类型、操作者信息和请求信息等")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    private void fillBasicInfo(SystemOperationLog log, OperationLog operationLog, Object[] args) {
        log.setModule(operationLog.module());
        log.setOperation(operationLog.operation());
        log.setOperationType(operationLog.type().getCode());

        fillUserInfo(log, args);
        fillRequestInfo(log, operationLog.recordParams());
    }
    @Schema(description = "填充操作者信息，包括操作者ID和用户名等，如果当前用户未登录，则尝试从请求参数中推断用户名")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    private void fillUserInfo(SystemOperationLog log, Object[] args) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            log.setOperatorId(userDetails.getUserId());
            log.setOperatorUsername(userDetails.getUsername());
        } else if (principal instanceof String username) {
            if (!"anonymousUser".equals(username)) {
                log.setOperatorUsername(username);
            }
        }

        if (log.getOperatorUsername() == null) {
            log.setOperatorUsername(inferUsernameFromArgs(args));
        }
    }
    @Schema(description = "推断用户名，从请求参数中查找包含用户名的对象，并尝试获取其用户名属性")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
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

    @Schema(description = "填充请求信息，包括请求URI、请求方法、客户端IP地址和User-Agent等")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
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

    @Schema(description = "提取请求参数，将请求参数转换为JSON字符串")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    private String extractRequestParams(HttpServletRequest request) {
        try {
            String params = objectMapper.writeValueAsString(request.getParameterMap());
            return truncate(params, MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return null;
        }
    }
    @Schema(description = "获取客户端IP地址，优先从X-Forwarded-For和X-Real-IP等HTTP头中获取，如果没有则使用request.getRemoteAddr()")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
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
    @Schema(description = "截断字符串，如果字符串长度超过指定的最大长度，则截断并返回前maxLength个字符")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
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
