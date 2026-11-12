package com.internpilot.service.user;

import com.internpilot.dto.user.ChangePasswordRequest;
import com.internpilot.dto.user.UpdateProfileRequest;
import com.internpilot.vo.user.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileVO getCurrentProfile();

    UserProfileVO updateCurrentProfile(UpdateProfileRequest request);

    UserProfileVO updateCurrentAvatar(MultipartFile file);

    void changeCurrentPassword(ChangePasswordRequest request);
}
