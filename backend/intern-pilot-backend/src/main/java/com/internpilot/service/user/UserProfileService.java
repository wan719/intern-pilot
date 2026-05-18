package com.internpilot.service.user;

import com.internpilot.dto.user.ChangePasswordRequest;
import com.internpilot.dto.user.UpdateProfileRequest;
import com.internpilot.vo.user.UserProfileVO;

public interface UserProfileService {

    UserProfileVO getCurrentProfile();

    UserProfileVO updateCurrentProfile(UpdateProfileRequest request);

    void changeCurrentPassword(ChangePasswordRequest request);
}
