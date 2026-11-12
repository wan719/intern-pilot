package com.internpilot.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;

    @Size(max = 100, message = "期望岗位长度不能超过 100 个字符")
    private String preferredJobTitle;

    @Size(max = 100, message = "期望城市长度不能超过 100 个字符")
    private String preferredCity;

    @Size(max = 100, message = "期望薪资长度不能超过 100 个字符")
    private String expectedSalary;

    @Size(max = 50, message = "求职类型长度不能超过 50 个字符")
    private String employmentType;
}
