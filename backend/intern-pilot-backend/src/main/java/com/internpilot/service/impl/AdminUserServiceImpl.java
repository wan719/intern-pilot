package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.admin.AdminUserRoleUpdateRequest;
import com.internpilot.entity.Role;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.InterviewQuestionReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.RoleMapper;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.mapper.UserRoleMapper;
import com.internpilot.service.AdminUserService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import com.internpilot.vo.admin.AdminUserDetailResponse;
import com.internpilot.vo.admin.AdminUserListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final ResumeMapper resumeMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final ApplicationRecordMapper applicationRecordMapper;
    private final InterviewQuestionReportMapper interviewQuestionReportMapper;
    private final SystemOperationLogMapper systemOperationLogMapper;

    @Override
    public PageResult<AdminUserListResponse> list(String keyword, String roleCode, Integer enabled, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword.trim()).or().like(User::getEmail, keyword.trim()));
        }
        if (enabled != null) {
            wrapper.eq(User::getEnabled, enabled);
        }
        if (StringUtils.hasText(roleCode)) {
            Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleCode, roleCode.trim())
                    .eq(Role::getDeleted, 0)
                    .last("LIMIT 1"));
            if (role == null) {
                return new PageResult<AdminUserListResponse>(
                        List.of(),
                        0L,
                        Long.valueOf(pageNum),
                        Long.valueOf(pageSize),
                        0L
                );
            }
            wrapper.inSql(User::getId,
                    "SELECT ur.user_id FROM user_role ur WHERE ur.deleted = 0 AND ur.role_id = " + role.getId());
        }

        wrapper.orderByDesc(User::getCreatedAt).orderByDesc(User::getId);

        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> resultPage = userMapper.selectPage(page, wrapper);

        List<AdminUserListResponse> records = resultPage.getRecords().stream().map(user -> {
            AdminUserListResponse item = new AdminUserListResponse();
            item.setUserId(user.getId());
            item.setUsername(user.getUsername());
            item.setEmail(user.getEmail());
            item.setSchool(user.getSchool());
            item.setMajor(user.getMajor());
            item.setGrade(user.getGrade());
            item.setEnabled(user.getEnabled());
            item.setRoles(permissionMapper.selectRoleCodesByUserId(user.getId()));
            item.setCreatedAt(user.getCreatedAt());
            return item;
        }).toList();

        return new PageResult<>(records, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize(), resultPage.getPages());
    }

    @Override
    public AdminUserDetailResponse getDetail(Long userId) {
        User user = getUserOrThrow(userId);

        AdminUserDetailResponse detail = new AdminUserDetailResponse();
        detail.setUserId(user.getId());
        detail.setUsername(user.getUsername());
        detail.setEmail(user.getEmail());
        detail.setSchool(user.getSchool());
        detail.setMajor(user.getMajor());
        detail.setGrade(user.getGrade());
        detail.setEnabled(user.getEnabled());
        detail.setRoles(permissionMapper.selectRoleCodesByUserId(user.getId()));
        detail.setPermissions(permissionMapper.selectPermissionCodesByUserId(user.getId()));

        detail.setResumeCount(toInt(resumeMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.Resume>()
                .eq(com.internpilot.entity.Resume::getUserId, userId)
                .eq(com.internpilot.entity.Resume::getDeleted, 0))));
        detail.setJobCount(toInt(jobDescriptionMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.JobDescription>()
                .eq(com.internpilot.entity.JobDescription::getUserId, userId)
                .eq(com.internpilot.entity.JobDescription::getDeleted, 0))));
        detail.setAnalysisReportCount(toInt(analysisReportMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.AnalysisReport>()
                .eq(com.internpilot.entity.AnalysisReport::getUserId, userId)
                .eq(com.internpilot.entity.AnalysisReport::getDeleted, 0))));
        detail.setApplicationCount(toInt(applicationRecordMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.ApplicationRecord>()
                .eq(com.internpilot.entity.ApplicationRecord::getUserId, userId)
                .eq(com.internpilot.entity.ApplicationRecord::getDeleted, 0))));
        detail.setInterviewQuestionReportCount(toInt(interviewQuestionReportMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.InterviewQuestionReport>()
                .eq(com.internpilot.entity.InterviewQuestionReport::getUserId, userId)
                .eq(com.internpilot.entity.InterviewQuestionReport::getDeleted, 0))));

        detail.setCreatedAt(user.getCreatedAt());
        detail.setUpdatedAt(user.getUpdatedAt());
        return detail;
    }

    @Override
    @Transactional
    public Boolean disable(Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能禁用当前登录用户");
        }

        User user = getUserOrThrow(userId);
        if (Integer.valueOf(0).equals(user.getEnabled())) {
            return true;
        }

        boolean targetIsAdmin = permissionMapper.selectRoleCodesByUserId(userId).contains("ADMIN");
        if (targetIsAdmin && countEnabledAdminUsers() <= 1) {
            throw new BusinessException("至少保留一个启用状态的ADMIN用户");
        }

        user.setEnabled(0);
        userMapper.updateById(user);
        return true;
    }

    @Override
    @Transactional
    public Boolean enable(Long userId) {
        User user = getUserOrThrow(userId);
        user.setEnabled(1);
        userMapper.updateById(user);
        return true;
    }

    @Override
    @Transactional
    public Boolean updateRoles(Long userId, AdminUserRoleUpdateRequest request) {
        User user = getUserOrThrow(userId);
        List<Long> roleIds = request.getRoleIds();

        Long roleCount = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .in(Role::getId, roleIds)
                .eq(Role::getDeleted, 0)
                .eq(Role::getEnabled, 1));
        if (roleCount == null || roleCount != roleIds.size()) {
            throw new BusinessException("角色不存在或已禁用");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            Set<String> roleCodes = roleMapper.selectBatchIds(roleIds).stream().map(Role::getRoleCode).collect(java.util.stream.Collectors.toSet());
            if (!roleCodes.contains("ADMIN")) {
                throw new BusinessException("不能移除当前登录用户的ADMIN角色");
            }
        }

        userRoleMapper.disableByUserId(userId);
        for (Long roleId : roleIds) {
            userRoleMapper.upsertActive(userId, roleId);
        }

        if (roleIds.size() == 1) {
            Role role = roleMapper.selectById(roleIds.get(0));
            if (role != null) {
                user.setRole(role.getRoleCode());
                userMapper.updateById(user);
            }
        }
        return true;
    }

    @Override
    public AdminDashboardSummaryResponse dashboardSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        AdminDashboardSummaryResponse data = new AdminDashboardSummaryResponse();
        data.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));
        data.setTodayNewUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .ge(User::getCreatedAt, todayStart)));
        data.setResumeCount(resumeMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.Resume>().eq(com.internpilot.entity.Resume::getDeleted, 0)));
        data.setJobCount(jobDescriptionMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.JobDescription>().eq(com.internpilot.entity.JobDescription::getDeleted, 0)));
        data.setAnalysisReportCount(analysisReportMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.AnalysisReport>().eq(com.internpilot.entity.AnalysisReport::getDeleted, 0)));
        data.setInterviewQuestionReportCount(interviewQuestionReportMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.InterviewQuestionReport>().eq(com.internpilot.entity.InterviewQuestionReport::getDeleted, 0)));
        data.setApplicationCount(applicationRecordMapper.selectCount(new LambdaQueryWrapper<com.internpilot.entity.ApplicationRecord>().eq(com.internpilot.entity.ApplicationRecord::getDeleted, 0)));
        data.setTodayOperationLogCount(systemOperationLogMapper.selectCount(new LambdaQueryWrapper<SystemOperationLog>()
                .eq(SystemOperationLog::getDeleted, 0)
                .ge(SystemOperationLog::getCreatedAt, todayStart)));
        data.setFailedOperationCount(systemOperationLogMapper.selectCount(new LambdaQueryWrapper<SystemOperationLog>()
                .eq(SystemOperationLog::getDeleted, 0)
                .eq(SystemOperationLog::getSuccess, 0)
                .ge(SystemOperationLog::getCreatedAt, todayStart)));
        return data;
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private long countEnabledAdminUsers() {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .eq(User::getEnabled, 1)
                .inSql(User::getId,
                        "SELECT ur.user_id FROM user_role ur JOIN role r ON ur.role_id = r.id " +
                                "WHERE ur.deleted = 0 AND r.deleted = 0 AND r.enabled = 1 AND r.role_code = 'ADMIN'"));
    }
}
