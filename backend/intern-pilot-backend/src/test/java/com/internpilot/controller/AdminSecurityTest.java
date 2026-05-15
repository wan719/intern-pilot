package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.security.CustomUserDetailsService;
import com.internpilot.service.AdminPermissionService;
import com.internpilot.service.AdminUserService;
import com.internpilot.security.JwtTokenProvider;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AdminPermissionService adminPermissionService;

    @MockBean
    private SystemOperationLogMapper systemOperationLogMapper;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void users_shouldReturnUnauthorized_whenNotLogin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"ROLE_ADMIN", "job:read"})
    void users_shouldReturnForbidden_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "user:read"})
    void users_shouldReturnOk_whenHasPermission() throws Exception {
        when(adminUserService.list(null, null, null, 1, 10))
                .thenReturn(new PageResult<>());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"ROLE_ADMIN", "job:read"})
    void disableUser_shouldReturnForbidden_whenNoPermission() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/disable"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "user:update"})
    void disableUser_shouldReturnOk_whenHasPermission() throws Exception {
        when(adminUserService.disable(1L)).thenReturn(true);

        mockMvc.perform(put("/api/admin/users/1/disable"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"ROLE_ADMIN", "job:read"})
    void roles_shouldReturnForbidden_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "role:read"})
    void roles_shouldReturnOk_whenHasPermission() throws Exception {
        when(adminPermissionService.listRoles()).thenReturn(List.of(new RoleResponse()));

        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"ROLE_ADMIN", "job:read"})
    void dashboard_shouldReturnForbidden_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "admin:dashboard"})
    void dashboard_shouldReturnOk_whenHasPermission() throws Exception {
        when(adminUserService.dashboardSummary()).thenReturn(new AdminDashboardSummaryResponse());

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk());
    }

    @Test
    void ping_shouldReturnUnauthorized_whenNotLogin() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ping_shouldReturnForbidden_whenUserTokenHasNoDashboardPermission() throws Exception {
        when(customUserDetailsService.loadUserByUsername("wan"))
                .thenReturn(userDetails("wan", "ROLE_USER", "resume:read"));
        String token = jwtTokenProvider.generateToken(1L, "wan", "USER");

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void ping_shouldReturnOk_whenAdminTokenHasDashboardPermission() throws Exception {
        when(customUserDetailsService.loadUserByUsername("admin"))
                .thenReturn(userDetails("admin", "ROLE_ADMIN", "admin:dashboard"));
        String token = jwtTokenProvider.generateToken(2L, "admin", "ADMIN");

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private UserDetails userDetails(String username, String... authorities) {
        return new User(
                username,
                "N/A",
                List.of(authorities).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
    }
}
