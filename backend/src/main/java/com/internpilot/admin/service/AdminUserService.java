package com.internpilot.admin.service;

import com.internpilot.common.PageResult;
import com.internpilot.rbac.dto.AdminUserRoleUpdateRequest;
import com.internpilot.admin.vo.AdminDashboardSummaryResponse;
import com.internpilot.admin.vo.AdminUserDetailResponse;
import com.internpilot.admin.vo.AdminUserListResponse;

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