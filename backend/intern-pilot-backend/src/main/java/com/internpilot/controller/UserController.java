package com.internpilot.controller;

import com.internpilot.common.Result;
import com.internpilot.service.UserService;
import com.internpilot.vo.auth.AuthUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户信息接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "查询当前用户信息", description = "根据 JWT Token 查询当前登录用户信息")
    @GetMapping("/me")
    public Result<AuthUserResponse> getCurrentUserInfo() {
        return Result.success(userService.getCurrentUserInfo());
    }
}
