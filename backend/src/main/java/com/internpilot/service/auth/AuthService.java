package com.internpilot.service.auth;

import com.internpilot.dto.auth.CaptchaSendRequest;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;

public interface AuthService {

    AuthUserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void sendRegisterCaptcha(CaptchaSendRequest request);
}