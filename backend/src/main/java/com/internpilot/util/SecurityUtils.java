package com.internpilot.util;

import com.internpilot.exception.BusinessException;
import com.internpilot.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
@Schema(description = "安全工具类，提供了获取当前认证用户信息和用户ID等功能，主要用于在需要用户身份信息的业务逻辑中方便地获取当前用户的相关信息")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException("当前用户未登录");
        }
        return userDetails;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
