package com.internpilot.auth.service;

import com.internpilot.auth.dto.CaptchaSendRequest;
import com.internpilot.auth.dto.LoginRequest;
import com.internpilot.auth.dto.RegisterRequest;
import com.internpilot.auth.vo.AuthUserResponse;
import com.internpilot.auth.vo.LoginResponse;

public interface AuthService {

    AuthUserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void sendRegisterCaptcha(CaptchaSendRequest request);
}