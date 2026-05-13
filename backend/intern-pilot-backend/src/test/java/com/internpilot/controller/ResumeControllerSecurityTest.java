package com.internpilot.controller;

import com.internpilot.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

    @Test
    void list_shouldReturnUnauthorized_whenNotLogin() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"resume:read"})
    void list_shouldReturnOk_whenHasPermission() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "wan", authorities = {"job:read"})
    void list_shouldReturnForbidden_whenNoPermission() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isForbidden());
    }
}