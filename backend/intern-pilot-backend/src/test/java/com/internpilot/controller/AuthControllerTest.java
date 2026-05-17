package com.internpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.dto.auth.CaptchaSendRequest;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.service.auth.AuthService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void login_shouldReturnToken_whenSuccess() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setToken("mock-token");
        response.setTokenType("Bearer");

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest();
        request.setAccount("demo@internpilot.local");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("mock-token"));
    }

    @Test
    void register_shouldReturnBadRequest_whenAccountBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setAccount("");
        request.setAccountType("EMAIL");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        request.setCaptchaCode("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void sendRegisterCaptcha_shouldReturnBadRequest_whenTargetBlank() throws Exception {
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setTarget("");
        request.setType("EMAIL");

        mockMvc.perform(post("/api/auth/captcha/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}