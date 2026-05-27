package com.internpilot.service.user.impl;

import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.auth.AuthUserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private UserServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserInfoShouldReturnProfileRolesAndPermissions() {
        mockLoginUser(7L);
        when(userMapper.selectById(7L)).thenReturn(user(7L, 0));
        when(permissionMapper.selectRoleCodesByUserId(7L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectPermissionCodesByUserId(7L)).thenReturn(List.of("resume:read"));

        AuthUserResponse response = service.getCurrentUserInfo();

        assertEquals(7L, response.getUserId());
        assertEquals("tester", response.getUsername());
        assertEquals("Tester", response.getNickname());
        assertEquals(List.of("USER"), response.getRoles());
        assertEquals(List.of("resume:read"), response.getPermissions());
    }

    @Test
    void getCurrentUserInfoShouldRejectMissingOrDeletedUser() {
        mockLoginUser(7L);
        when(userMapper.selectById(7L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getCurrentUserInfo());

        when(userMapper.selectById(7L)).thenReturn(user(7L, 1));
        assertThrows(BusinessException.class, () -> service.getCurrentUserInfo());
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails principal = new CustomUserDetails(userId, "tester", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private User user(Long id, Integer deleted) {
        User user = new User();
        user.setId(id);
        user.setUsername("tester");
        user.setRealName("Tester");
        user.setAvatarUrl("/uploads/a.png");
        user.setEmail("tester@example.com");
        user.setPhone("13800000000");
        user.setSchool("SWU");
        user.setMajor("SE");
        user.setGrade("2026");
        user.setRole("USER");
        user.setDeleted(deleted);
        return user;
    }
}
