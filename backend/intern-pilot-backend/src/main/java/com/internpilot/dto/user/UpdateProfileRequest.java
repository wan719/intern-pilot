package com.internpilot.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;
}
