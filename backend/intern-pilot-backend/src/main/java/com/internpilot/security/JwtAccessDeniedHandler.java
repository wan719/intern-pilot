package com.internpilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.common.Result;
import com.internpilot.common.ResultCode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Schema(description = "JWT访问拒绝处理器，当用户尝试访问没有权限的资源时返回403错误和统一的JSON响应")//这个注解用于Swagger API文档生成，提供了对该组件的描述信息
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Result<Void> result = Result.fail(ResultCode.FORBIDDEN, "无权限访问该资源");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
