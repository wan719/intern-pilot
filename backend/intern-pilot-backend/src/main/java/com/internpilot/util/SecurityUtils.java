package com.internpilot.util;

import com.internpilot.exception.BusinessException;
import com.internpilot.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
