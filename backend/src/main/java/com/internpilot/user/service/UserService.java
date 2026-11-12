package com.internpilot.user.service;

import com.internpilot.auth.vo.AuthUserResponse;

public interface UserService {

    AuthUserResponse getCurrentUserInfo();
}
