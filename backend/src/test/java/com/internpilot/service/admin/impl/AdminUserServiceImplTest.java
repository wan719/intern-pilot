package com.internpilot.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.admin.AdminUserRoleUpdateRequest;
import com.internpilot.entity.Role;
import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.InterviewQuestionReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.RoleMapper;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.mapper.UserRoleMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import com.internpilot.vo.admin.AdminUserDetailResponse;
import com.internpilot.vo.admin.AdminUserListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private JobDescriptionMapper jobDescriptionMapper;

    @Mock
    private AnalysisReportMapper analysisReportMapper;

    @Mock
    private ApplicationRecordMapper applicationRecordMapper;

    @Mock
    private InterviewQuestionReportMapper interviewQuestionReportMapper;

    @Mock
    private SystemOperationLogMapper systemOperationLogMapper;

    @InjectMocks
    private AdminUserServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listShouldReturnUsersAndRoleCodes() {
        Role role = role(1L, "USER");
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(role);

        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user(7L, "student", 1)));
        page.setTotal(1L);
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(permissionMapper.selectRoleCodesByUserId(7L)).thenReturn(List.of("USER"));

        PageResult<AdminUserListResponse> result = service.list("stu", "USER", 1, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("student", result.getRecords().get(0).getUsername());
        assertEquals(List.of("USER"), result.getRecords().get(0).getRoles());
    }

    @Test
    void listShouldReturnEmptyWhenRoleCodeDoesNotExist() {
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        PageResult<AdminUserListResponse> result = service.list(null, "UNKNOWN", null, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void getDetailShouldReturnCountsRolesAndPermissions() {
        when(userMapper.selectById(7L)).thenReturn(user(7L, "student", 1));
        when(permissionMapper.selectRoleCodesByUserId(7L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectPermissionCodesByUserId(7L)).thenReturn(List.of("resume:read"));
        when(resumeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(jobDescriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(analysisReportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(interviewQuestionReportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(6L);

        AdminUserDetailResponse detail = service.getDetail(7L);

        assertEquals(7L, detail.getUserId());
        assertEquals(2, detail.getResumeCount());
        assertEquals(6, detail.getInterviewQuestionReportCount());
        assertEquals(List.of("resume:read"), detail.getPermissions());
    }

    @Test
    void disableShouldRejectCurrentUserAndLastAdmin() {
        mockLoginUser(7L);
        assertThrows(BusinessException.class, () -> service.disable(7L));

        mockLoginUser(99L);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", 1));
        when(permissionMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("ADMIN"));
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.disable(1L));
    }

    @Test
    void disableAndEnableShouldUpdateUserState() {
        mockLoginUser(99L);
        User target = user(7L, "student", 1);
        when(userMapper.selectById(7L)).thenReturn(target);
        when(permissionMapper.selectRoleCodesByUserId(7L)).thenReturn(List.of("USER"));

        assertTrue(service.disable(7L));
        assertEquals(0, target.getEnabled());

        assertTrue(service.enable(7L));
        assertEquals(1, target.getEnabled());
        verify(userMapper, org.mockito.Mockito.times(2)).updateById(target);
    }

    @Test
    void disableShouldReturnTrueWhenAlreadyDisabled() {
        mockLoginUser(99L);
        User target = user(7L, "student", 0);
        when(userMapper.selectById(7L)).thenReturn(target);

        assertTrue(service.disable(7L));
    }

    @Test
    void updateRolesShouldUpsertAndSyncSingleLegacyRole() {
        mockLoginUser(99L);
        when(userMapper.selectById(7L)).thenReturn(user(7L, "student", 1));
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "ADMIN"));

        AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
        request.setRoleIds(List.of(1L));

        assertTrue(service.updateRoles(7L, request));

        verify(userRoleMapper).disableByUserId(7L);
        verify(userRoleMapper).upsertActive(7L, 1L);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void updateRolesShouldRejectInvalidRolesAndRemovingOwnAdmin() {
        mockLoginUser(99L);
        when(userMapper.selectById(8L)).thenReturn(null);
        AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
        request.setRoleIds(List.of(1L));
        assertThrows(BusinessException.class, () -> service.updateRoles(8L, request));

        when(userMapper.selectById(99L)).thenReturn(user(99L, "admin", 1));
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(roleMapper.selectBatchIds(List.of(2L))).thenReturn(List.of(role(2L, "USER")));
        AdminUserRoleUpdateRequest removeAdmin = new AdminUserRoleUpdateRequest();
        removeAdmin.setRoleIds(List.of(2L));

        assertThrows(BusinessException.class, () -> service.updateRoles(99L, removeAdmin));
    }

    @Test
    void dashboardSummaryShouldReturnAllCounters() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 2L);
        when(resumeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(20L);
        when(jobDescriptionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(30L);
        when(analysisReportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(40L);
        when(interviewQuestionReportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(50L);
        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(60L);
        when(systemOperationLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(70L, 3L);

        AdminDashboardSummaryResponse response = service.dashboardSummary();

        assertEquals(10L, response.getUserCount());
        assertEquals(2L, response.getTodayNewUserCount());
        assertEquals(70L, response.getTodayOperationLogCount());
        assertEquals(3L, response.getFailedOperationCount());
    }

    @Test
    void getDetailShouldRejectMissingOrDeletedUser() {
        when(userMapper.selectById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getDetail(1L));

        User deleted = user(2L, "deleted", 1);
        deleted.setDeleted(1);
        when(userMapper.selectById(2L)).thenReturn(deleted);
        assertThrows(BusinessException.class, () -> service.getDetail(2L));
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails principal = new CustomUserDetails(userId, "admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private User user(Long id, String username, Integer enabled) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username + " name");
        user.setEmail(username + "@example.com");
        user.setSchool("SWU");
        user.setMajor("SE");
        user.setGrade("2026");
        user.setEnabled(enabled);
        user.setRole("USER");
        user.setDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(code + "角色");
        role.setEnabled(1);
        role.setDeleted(0);
        return role;
    }
}
