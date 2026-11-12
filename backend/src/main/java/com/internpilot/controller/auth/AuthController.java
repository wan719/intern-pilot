package com.internpilot.controller.auth;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.Result;
import com.internpilot.dto.auth.CaptchaSendRequest;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.auth.AuthService;
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

    @Operation(summary = "发送注册验证码", description = "向手机号或邮箱发送注册验证码")
    @OperationLog(module = "用户认证", operation = "发送注册验证码", type = OperationTypeEnum.CREATE, recordParams = false)
    @PostMapping("/captcha/register")
    public Result<Void> sendRegisterCaptcha(@RequestBody @Valid CaptchaSendRequest request) {
        authService.sendRegisterCaptcha(request);
        return Result.success();
    }

    @Operation(summary = "用户注册", description = "用户通过手机号或邮箱、验证码、密码等信息注册账号")
    @OperationLog(module = "用户认证", operation = "用户注册", type = OperationTypeEnum.CREATE, recordParams = false)
    @PostMapping("/register")
    public Result<AuthUserResponse> register(@RequestBody @Valid RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "用户通过手机号/邮箱和密码登录系统，系统账号兼容用户名登录，登录成功后返回 JWT Token")
    @OperationLog(module = "用户认证", operation = "用户登录", type = OperationTypeEnum.LOGIN, recordParams = false)
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.success(authService.login(request));
    }
}