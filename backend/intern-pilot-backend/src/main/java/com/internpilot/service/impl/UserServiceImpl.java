package com.internpilot.service.impl;

import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.UserMapper;
import com.internpilot.service.UserService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.auth.AuthUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public AuthUserResponse getCurrentUserInfo() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }

        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setSchool(user.getSchool());
        response.setMajor(user.getMajor());
        response.setGrade(user.getGrade());
        response.setRole(user.getRole());
        return response;
    }
}
