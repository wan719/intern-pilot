package com.internpilot.service;

import com.internpilot.vo.auth.AuthUserResponse;

public interface UserService {

    AuthUserResponse getCurrentUserInfo();
}
