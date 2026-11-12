package com.internpilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.common.Result;
import com.internpilot.common.ResultCode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Schema(description = "JWT认证入口点，当用户未认证或Token无效时返回401错误和统一的JSON响应")//这个注解用于Swagger API文档生成，提供了对该组件的描述信息
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED, "请先登录或 Token 已失效");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
