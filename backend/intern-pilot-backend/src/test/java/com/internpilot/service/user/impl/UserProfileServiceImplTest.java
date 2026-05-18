package com.internpilot.service.user.impl;

import com.internpilot.dto.user.ChangePasswordRequest;
import com.internpilot.dto.user.UpdateProfileRequest;
import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.user.UserProfileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserProfileServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(userMapper, permissionMapper, resumeMapper, passwordEncoder);
        CustomUserDetails principal = new CustomUserDetails(1L, "wan", List.of("USER"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentProfile_shouldReturnCurrentUserOnly() {
        when(userMapper.selectById(1L)).thenReturn(mockUser());
        when(permissionMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectPermissionCodesByUserId(1L)).thenReturn(List.of("resume:read"));

        UserProfileVO profile = service.getCurrentProfile();

        assertEquals(1L, profile.getId());
        assertEquals("wan@example.com", profile.getEmail());
        assertTrue(profile.getEmailVerified());
        assertEquals(List.of("USER"), profile.getRoles());
    }

    @Test
    void updateCurrentProfile_shouldUpdateNickname() {
        User user = mockUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(permissionMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectPermissionCodesByUserId(1L)).thenReturn(List.of());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");
        request.setPreferredJobTitle("Java 后端实习生");
        request.setPreferredCity("重庆");
        request.setExpectedSalary("150-200/天");
        request.setEmploymentType("实习");

        UserProfileVO profile = service.updateCurrentProfile(request);

        assertEquals("新昵称", user.getRealName());
        assertEquals("新昵称", profile.getNickname());
        assertEquals("Java 后端实习生", user.getPreferredJobTitle());
        assertEquals("重庆", profile.getPreferredCity());
        assertEquals("150-200/天", profile.getExpectedSalary());
        assertEquals("实习", profile.getEmploymentType());
        verify(userMapper).updateById(user);
    }

    @Test
    void updateCurrentAvatar_shouldStoreImageAndUpdateProfile() {
        User user = mockUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(permissionMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectPermissionCodesByUserId(1L)).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "avatarDir", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        UserProfileVO profile = service.updateCurrentAvatar(file);

        assertNotNull(profile.getAvatarUrl());
        assertTrue(profile.getAvatarUrl().startsWith("/uploads/avatars/user-1/"));
        assertEquals(profile.getAvatarUrl(), user.getAvatarUrl());
        verify(userMapper).updateById(user);
    }

    @Test
    void changeCurrentPassword_shouldEncodeNewPassword() {
        User user = mockUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass");
        request.setConfirmPassword("new-pass");

        service.changeCurrentPassword(request);

        assertEquals("encoded-new", user.getPassword());
        verify(userMapper).updateById(user);
    }

    @Test
    void changeCurrentPassword_shouldFailWhenOldPasswordWrong() {
        when(userMapper.selectById(1L)).thenReturn(mockUser());
        when(passwordEncoder.matches("bad-pass", "encoded-old")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("bad-pass");
        request.setNewPassword("new-pass");
        request.setConfirmPassword("new-pass");

        assertThrows(BusinessException.class, () -> service.changeCurrentPassword(request));
        verify(userMapper, never()).updateById(any(User.class));
    }

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("wan");
        user.setPassword("encoded-old");
        user.setEmail("wan@example.com");
        user.setRealName("旧昵称");
        user.setRole("USER");
        user.setEmailVerified(1);
        user.setDeleted(0);
        return user;
    }
}
