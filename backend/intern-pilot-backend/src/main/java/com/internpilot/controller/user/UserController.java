package com.internpilot.controller.user;

import com.internpilot.common.Result;
import com.internpilot.dto.user.ChangePasswordRequest;
import com.internpilot.dto.user.UpdateProfileRequest;
import com.internpilot.service.user.UserProfileService;
import com.internpilot.service.user.UserService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.user.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户信息接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    @Operation(summary = "查询当前用户信息", description = "根据 JWT Token 查询当前登录用户信息")
    @GetMapping("/me")
    public Result<AuthUserResponse> getCurrentUserInfo() {
        return Result.success(userService.getCurrentUserInfo());
    }

    @Operation(summary = "获取当前用户中心资料")
    @GetMapping("/profile")
    public Result<UserProfileVO> getCurrentProfile() {
        return Result.success(userProfileService.getCurrentProfile());
    }

    @Operation(summary = "修改当前用户基础资料")
    @PutMapping("/profile")
    public Result<UserProfileVO> updateCurrentProfile(@RequestBody @Valid UpdateProfileRequest request) {
        return Result.success(userProfileService.updateCurrentProfile(request));
    }

    @Operation(summary = "上传或修改当前用户头像")
    @PostMapping("/avatar")
    public Result<UserProfileVO> updateCurrentAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userProfileService.updateCurrentAvatar(file));
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/password")
    public Result<Void> changeCurrentPassword(@RequestBody @Valid ChangePasswordRequest request) {
        userProfileService.changeCurrentPassword(request);
        return Result.success();
    }
}
