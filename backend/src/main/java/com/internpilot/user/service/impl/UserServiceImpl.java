package com.internpilot.user.service.impl;

import com.internpilot.user.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.rbac.mapper.PermissionMapper;
import com.internpilot.user.mapper.UserMapper;
import com.internpilot.user.service.UserService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.auth.vo.AuthUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public AuthUserResponse getCurrentUserInfo() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存");
        }

        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getRealName());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setSchool(user.getSchool());
        response.setMajor(user.getMajor());
        response.setGrade(user.getGrade());
        response.setRole(user.getRole());
        response.setRoles(permissionMapper.selectRoleCodesByUserId(user.getId()));
        response.setPermissions(permissionMapper.selectPermissionCodesByUserId(user.getId()));
        return response;
    }
}
