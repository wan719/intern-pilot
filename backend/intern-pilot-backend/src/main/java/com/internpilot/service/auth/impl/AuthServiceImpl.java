package com.internpilot.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.auth.CaptchaSendRequest;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.entity.Role;
import com.internpilot.entity.User;
import com.internpilot.entity.UserRole;
import com.internpilot.enums.AccountTypeEnum;
import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.enums.UserRoleEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.RoleMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.mapper.UserRoleMapper;
import com.internpilot.security.JwtTokenProvider;
import com.internpilot.service.auth.AuthService;
import com.internpilot.service.auth.CaptchaService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final CaptchaService captchaService;

    @Override
    public void sendRegisterCaptcha(CaptchaSendRequest request) {
        authServiceEmailOnly(request.getType());
        captchaService.sendRegisterCaptcha(request);
    }

    @Override
    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        String account = request.getAccount().trim();
        String accountType = request.getAccountType().trim().toUpperCase();
        if (!AccountTypeEnum.EMAIL.getCode().equals(accountType)) {
            throw new BusinessException("当前阶段仅支持邮箱注册");
        }
        if (!account.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException("邮箱格式不正确");
        }

        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, account)
                        .eq(User::getDeleted, 0));
        if (emailCount != null && emailCount > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        captchaService.validateCaptcha(account, CaptchaSceneEnum.EMAIL_REGISTER, request.getCaptchaCode());

        User user = new User();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRoleEnum.USER.getCode());
        user.setEnabled(1);
        user.setAccountType(AccountTypeEnum.EMAIL.getCode());
        user.setEmail(account);
        user.setEmailVerified(1);
        user.setPhoneVerified(0);
        user.setUsername(StringUtils.hasText(request.getUsername())
                ? request.getUsername()
                : account.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 6));
        user.setSchool(request.getSchool());
        user.setMajor(request.getMajor());
        user.setGrade(request.getGrade());

        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, user.getUsername())
                        .eq(User::getDeleted, 0));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException("用户名已被占用，请更换用户名");
        }

        userMapper.insert(user);

        Role userRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleCode, UserRoleEnum.USER.getCode())
                        .eq(Role::getDeleted, 0)
                        .last("LIMIT 1"));

        if (userRole == null) {
            throw new BusinessException("系统默认角色不存在");
        }

        UserRole relation = new UserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(userRole.getId());
        userRoleMapper.insert(relation);

        return toAuthUserResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.getAccount().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException("邮箱或密码错误");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .eq(User::getDeleted, 0)
                        .last("LIMIT 1"));

        if (user == null) {
            throw new BusinessException("邮箱或密码错误");
        }
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BusinessException("当前用户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("邮箱或密码错误");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        LoginResponse response = new LoginResponse();
        response.setToken(jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole()));
        response.setExpiresIn(jwtTokenProvider.getExpirationSeconds());
        response.setUser(toAuthUserResponse(user));
        return response;
    }

    private void authServiceEmailOnly(String type) {
        if (!"EMAIL".equalsIgnoreCase(type == null ? "" : type.trim())) {
            throw new BusinessException("当前阶段仅支持邮箱注册验证码");
        }
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        List<String> roles = permissionMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(user.getId());

        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setSchool(user.getSchool());
        response.setMajor(user.getMajor());
        response.setGrade(user.getGrade());
        response.setRole(user.getRole());
        response.setRoles(roles);
        response.setPermissions(permissions);
        return response;
    }
}
