package com.internpilot.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleUpdateRequest {

    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}