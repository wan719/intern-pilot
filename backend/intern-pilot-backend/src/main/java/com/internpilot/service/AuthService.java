package com.internpilot.service;

import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;

public interface AuthService {

    AuthUserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
