package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.service.AdminPermissionService;
import com.internpilot.service.AdminUserService;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "admin:user:read"})
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
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "admin:user:disable"})
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
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "admin:role:read"})
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
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "dashboard:admin:read"})
    void dashboard_shouldReturnOk_whenHasPermission() throws Exception {
        when(adminUserService.dashboardSummary()).thenReturn(new AdminDashboardSummaryResponse());

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk());
    }
}