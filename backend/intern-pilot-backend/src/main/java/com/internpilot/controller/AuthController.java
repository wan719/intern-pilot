package com.internpilot.controller;

import com.internpilot.common.Result;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.service.AuthService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册", description = "用户通过用户名、密码、邮箱等信息注册账号")
    @PostMapping("/register")
    public Result<AuthUserResponse> register(@RequestBody @Valid RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统，登录成功后返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
