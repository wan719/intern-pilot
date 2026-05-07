package com.internpilot.vo.auth;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "认证用户信息")
public class AuthUserResponse {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "wan")
    private String username;

    @Schema(description = "邮箱", example = "wan@example.com")
    private String email;

    @Schema(description = "学校", example = "西南大学")
    private String school;

    @Schema(description = "专业", example = "软件工程")
    private String major;

    @Schema(description = "年级", example = "大二")
    private String grade;

    @Schema(description = "角色", example = "USER")
    private String role;
    @Schema(description = "权限列表", example = "[\"internship:read\", \"internship:write\"]")
    private List<String> roles;
    @Schema(description = "权限列表", example = "[\"internship:read\", \"internship:write\"]")
    private List<String> permissions;
}
