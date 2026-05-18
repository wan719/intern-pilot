package com.internpilot.service.user.impl;

import com.internpilot.dto.user.ChangePasswordRequest;
import com.internpilot.dto.user.UpdateProfileRequest;
import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.service.user.UserProfileService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.user.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO getCurrentProfile() {
        return toProfile(loadCurrentUser());
    }

    @Override
    @Transactional
    public UserProfileVO updateCurrentProfile(UpdateProfileRequest request) {
        User user = loadCurrentUser();
        user.setRealName(request.getNickname());
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Override
    @Transactional
    public void changeCurrentPassword(ChangePasswordRequest request) {
        User user = loadCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次新密码输入不一致");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    private User loadCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserProfileVO toProfile(User user) {
        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getRealName());
        profile.setEmail(user.getEmail());
        profile.setEmailVerified(Integer.valueOf(1).equals(user.getEmailVerified()));
        profile.setRoles(permissionMapper.selectRoleCodesByUserId(user.getId()));
        profile.setPermissions(permissionMapper.selectPermissionCodesByUserId(user.getId()));
        profile.setLastLoginTime(user.getLastLoginTime());
        profile.setCreatedAt(user.getCreatedAt());
        profile.setUpdatedAt(user.getUpdatedAt());
        return profile;
    }
}
