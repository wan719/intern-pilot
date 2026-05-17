package com.internpilot.service.user;

import com.internpilot.vo.auth.AuthUserResponse;

public interface UserService {

    AuthUserResponse getCurrentUserInfo();
}
