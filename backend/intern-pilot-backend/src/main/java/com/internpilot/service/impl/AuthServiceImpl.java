package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.entity.Role;
import com.internpilot.entity.User;
import com.internpilot.entity.UserRole;
import com.internpilot.enums.UserRoleEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.RoleMapper;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.mapper.UserRoleMapper;
import com.internpilot.security.JwtTokenProvider;
import com.internpilot.service.AuthService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .eq(User::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setSchool(request.getSchool());
        user.setMajor(request.getMajor());
        user.setGrade(request.getGrade());
        user.setRole(UserRoleEnum.USER.getCode());
        user.setEnabled(1);

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
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .eq(User::getDeleted, 0)
                        .last("LIMIT 1"));

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BusinessException("当前用户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        LoginResponse response = new LoginResponse();
        response.setToken(jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole()));
        response.setExpiresIn(jwtTokenProvider.getExpirationSeconds());
        response.setUser(toAuthUserResponse(user));
        return response;
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        List<String> roles = permissionMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(user.getId());

        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setSchool(user.getSchool());
        response.setMajor(user.getMajor());
        response.setGrade(user.getGrade());
        response.setRole(user.getRole());
        response.setRoles(roles);
        response.setPermissions(permissions);
        return response;
    }
}
