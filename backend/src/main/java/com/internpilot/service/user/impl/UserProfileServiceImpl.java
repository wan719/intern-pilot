package com.internpilot.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.user.ChangePasswordRequest;
import com.internpilot.dto.user.UpdateProfileRequest;
import com.internpilot.entity.Resume;
import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.service.user.UserProfileService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.user.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;
    private static final Set<String> AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> AVATAR_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final ResumeMapper resumeMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Override
    public UserProfileVO getCurrentProfile() {
        return toProfile(loadCurrentUser());
    }

    @Override
    @Transactional
    public UserProfileVO updateCurrentProfile(UpdateProfileRequest request) {
        User user = loadCurrentUser();
        user.setRealName(request.getNickname());
        user.setPreferredJobTitle(request.getPreferredJobTitle());
        user.setPreferredCity(request.getPreferredCity());
        user.setExpectedSalary(request.getExpectedSalary());
        user.setEmploymentType(request.getEmploymentType());
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateCurrentAvatar(MultipartFile file) {
        User user = loadCurrentUser();
        validateAvatar(file);

        String extension = getExtension(file.getOriginalFilename());
        String storedFileName = generateAvatarFileName(user.getId(), extension);
        Path userDir = Paths.get(avatarDir, "user-" + user.getId());

        try {
            Files.createDirectories(userDir);
            Path targetPath = userDir.resolve(storedFileName).normalize();
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            user.setAvatarUrl("/uploads/avatars/user-" + user.getId() + "/" + storedFileName);
            userMapper.updateById(user);
            return toProfile(user);
        } catch (IOException e) {
            throw new BusinessException("头像保存失败，请稍后重试");
        }
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
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setEmail(user.getEmail());
        profile.setEmailVerified(Integer.valueOf(1).equals(user.getEmailVerified()));
        profile.setPreferredJobTitle(user.getPreferredJobTitle());
        profile.setPreferredCity(user.getPreferredCity());
        profile.setExpectedSalary(user.getExpectedSalary());
        profile.setEmploymentType(user.getEmploymentType());
        fillDefaultResume(profile, user.getId());
        profile.setRoles(permissionMapper.selectRoleCodesByUserId(user.getId()));
        profile.setPermissions(permissionMapper.selectPermissionCodesByUserId(user.getId()));
        profile.setLastLoginTime(user.getLastLoginTime());
        profile.setCreatedAt(user.getCreatedAt());
        profile.setUpdatedAt(user.getUpdatedAt());
        return profile;
    }

    private void fillDefaultResume(UserProfileVO profile, Long userId) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .eq(Resume::getIsDefault, 1)
                .eq(Resume::getDeleted, 0)
                .last("LIMIT 1"));
        if (resume == null) {
            return;
        }
        profile.setDefaultResumeId(resume.getId());
        profile.setDefaultResumeName(resume.getResumeName());
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像文件");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BusinessException("头像大小不能超过 2MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!AVATAR_EXTENSIONS.contains(extension)) {
            throw new BusinessException("头像仅支持 JPG、PNG、WEBP 格式");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !AVATAR_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("头像仅支持 JPG、PNG、WEBP 格式");
        }
    }

    private String generateAvatarFileName(Long userId, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return userId + "_" + timestamp + "_" + random + "." + extension;
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
