package com.internpilot.user.service;

import com.internpilot.user.dto.ChangePasswordRequest;
import com.internpilot.user.dto.UpdateProfileRequest;
import com.internpilot.user.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileVO getCurrentProfile();

    UserProfileVO updateCurrentProfile(UpdateProfileRequest request);

    UserProfileVO updateCurrentAvatar(MultipartFile file);

    void changeCurrentPassword(ChangePasswordRequest request);
}
