package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.admin.AdminUserRoleUpdateRequest;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import com.internpilot.vo.admin.AdminUserDetailResponse;
import com.internpilot.vo.admin.AdminUserListResponse;

public interface AdminUserService {

    PageResult<AdminUserListResponse> list(
            String keyword,
            String roleCode,
            Integer enabled,
            Integer pageNum,
            Integer pageSize
    );

    AdminUserDetailResponse getDetail(Long userId);

    Boolean disable(Long userId);

    Boolean enable(Long userId);

    Boolean updateRoles(Long userId, AdminUserRoleUpdateRequest request);

    AdminDashboardSummaryResponse dashboardSummary();
}