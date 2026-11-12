package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.controller.admin.AdminController;
import com.internpilot.controller.admin.AdminFeedbackController;
import com.internpilot.controller.admin.AdminOperationLogController;
import com.internpilot.controller.admin.AdminRagKnowledgeController;
import com.internpilot.controller.application.ApplicationController;
import com.internpilot.controller.feedback.FeedbackController;
import com.internpilot.controller.resume.ResumeVersionController;
import com.internpilot.controller.user.UserController;
import com.internpilot.dto.admin.AdminRolePermissionUpdateRequest;
import com.internpilot.dto.admin.AdminUserRoleUpdateRequest;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.dto.feedback.FeedbackCreateRequest;
import com.internpilot.dto.feedback.FeedbackReplyRequest;
import com.internpilot.dto.feedback.FeedbackStatusUpdateRequest;
import com.internpilot.dto.rag.RagKnowledgeCreateRequest;
import com.internpilot.dto.rag.RagKnowledgeUpdateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
import com.internpilot.dto.resume.ResumeVersionCreateRequest;
import com.internpilot.dto.resume.ResumeVersionOptimizeRequest;
import com.internpilot.dto.resume.ResumeVersionUpdateRequest;
import com.internpilot.dto.user.ChangePasswordRequest;
import com.internpilot.dto.user.UpdateProfileRequest;
import com.internpilot.service.admin.AdminOperationLogService;
import com.internpilot.service.admin.AdminPermissionService;
import com.internpilot.service.admin.AdminUserService;
import com.internpilot.service.application.ApplicationService;
import com.internpilot.service.feedback.FeedbackService;
import com.internpilot.service.rag.RagKnowledgeService;
import com.internpilot.service.resume.ResumeVersionService;
import com.internpilot.service.user.UserProfileService;
import com.internpilot.service.user.UserService;
import com.internpilot.vo.admin.AdminDashboardSummaryResponse;
import com.internpilot.vo.admin.AdminUserDetailResponse;
import com.internpilot.vo.admin.AdminUserListResponse;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;
import com.internpilot.vo.admin.PermissionResponse;
import com.internpilot.vo.admin.RoleResponse;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.feedback.FeedbackResponse;
import com.internpilot.vo.rag.RagKnowledgeDetailResponse;
import com.internpilot.vo.rag.RagKnowledgeListResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;
import com.internpilot.vo.resume.ResumeVersionCompareResponse;
import com.internpilot.vo.resume.ResumeVersionCreateResponse;
import com.internpilot.vo.resume.ResumeVersionDetailResponse;
import com.internpilot.vo.resume.ResumeVersionListResponse;
import com.internpilot.vo.user.UserProfileVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAndMiscControllerDirectCoverageTest {

    @Test
    void adminControllersShouldDelegateEveryEndpoint() {
        AdminUserService userService = mock(AdminUserService.class);
        AdminPermissionService permissionService = mock(AdminPermissionService.class);
        AdminController adminController = new AdminController(userService, permissionService);
        when(userService.list("wan", "ADMIN", 1, 1, 10)).thenReturn(page(new AdminUserListResponse()));
        when(userService.getDetail(1L)).thenReturn(new AdminUserDetailResponse());
        when(userService.disable(1L)).thenReturn(true);
        when(userService.enable(1L)).thenReturn(true);
        when(userService.updateRoles(eq(1L), any(AdminUserRoleUpdateRequest.class))).thenReturn(true);
        when(userService.dashboardSummary()).thenReturn(new AdminDashboardSummaryResponse());
        when(permissionService.listRoles()).thenReturn(List.of(new RoleResponse()));
        when(permissionService.listPermissions("MENU")).thenReturn(List.of(new PermissionResponse()));
        when(permissionService.updateRolePermissions(eq(2L), any())).thenReturn(true);
        AdminRolePermissionUpdateRequest rolePermissionRequest = new AdminRolePermissionUpdateRequest();
        rolePermissionRequest.setPermissionIds(List.of(1L, 2L));

        assertThat(adminController.ping().getData()).isEqualTo("admin ok");
        assertThat(adminController.listUsers("wan", "ADMIN", 1, 1, 10).getData().getRecords()).hasSize(1);
        assertThat(adminController.getUserDetail(1L).getData()).isNotNull();
        assertThat(adminController.disableUser(1L).getData()).isTrue();
        assertThat(adminController.enableUser(1L).getData()).isTrue();
        assertThat(adminController.updateUserRoles(1L, new AdminUserRoleUpdateRequest()).getData()).isTrue();
        assertThat(adminController.listRoles().getData()).hasSize(1);
        assertThat(adminController.listPermissions("MENU").getData()).hasSize(1);
        assertThat(adminController.updateRolePermissions(2L, rolePermissionRequest).getData()).isTrue();
        assertThat(adminController.dashboardSummary().getData()).isNotNull();

        AdminOperationLogService logService = mock(AdminOperationLogService.class);
        AdminOperationLogController logController = new AdminOperationLogController(logService);
        when(logService.list("auth", "LOGIN", "wan", 1, 1, 10)).thenReturn(page(new OperationLogListResponse()));
        when(logService.getDetail(9L)).thenReturn(new OperationLogDetailResponse());
        when(logService.delete(9L)).thenReturn(true);

        assertThat(logController.list("auth", "LOGIN", "wan", 1, 1, 10).getData().getRecords()).hasSize(1);
        assertThat(logController.getDetail(9L).getData()).isNotNull();
        assertThat(logController.delete(9L).getData()).isTrue();
    }

    @Test
    void feedbackApplicationUserResumeVersionAndRagControllersShouldDelegate() {
        FeedbackService feedbackService = mock(FeedbackService.class);
        FeedbackController feedbackController = new FeedbackController(feedbackService);
        AdminFeedbackController adminFeedbackController = new AdminFeedbackController(feedbackService);
        when(feedbackService.createFeedback(any())).thenReturn(new FeedbackResponse());
        when(feedbackService.listMyFeedbacks()).thenReturn(List.of(new FeedbackResponse()));
        when(feedbackService.getMyFeedbackById(1L)).thenReturn(new FeedbackResponse());
        when(feedbackService.listAllFeedbacks("BUG", "OPEN")).thenReturn(List.of(new FeedbackResponse()));
        when(feedbackService.getFeedbackById(1L)).thenReturn(new FeedbackResponse());
        when(feedbackService.updateStatus(eq(1L), any(FeedbackStatusUpdateRequest.class))).thenReturn(new FeedbackResponse());
        when(feedbackService.reply(eq(1L), any(FeedbackReplyRequest.class))).thenReturn(new FeedbackResponse());

        assertThat(feedbackController.createFeedback(new FeedbackCreateRequest()).getData()).isNotNull();
        assertThat(feedbackController.listMyFeedbacks().getData()).hasSize(1);
        assertThat(feedbackController.getMyFeedback(1L).getData()).isNotNull();
        assertThat(adminFeedbackController.listFeedbacks("BUG", "OPEN").getData()).hasSize(1);
        assertThat(adminFeedbackController.getFeedback(1L).getData()).isNotNull();
        assertThat(adminFeedbackController.updateStatus(1L, new FeedbackStatusUpdateRequest()).getData()).isNotNull();
        assertThat(adminFeedbackController.reply(1L, new FeedbackReplyRequest()).getData()).isNotNull();
        assertThat(adminFeedbackController.deleteFeedback(1L).getCode()).isEqualTo(200);
        verify(feedbackService).deleteFeedback(1L);

        ApplicationService applicationService = mock(ApplicationService.class);
        ApplicationController applicationController = new ApplicationController(applicationService);
        when(applicationService.create(any())).thenReturn(new ApplicationCreateResponse());
        when(applicationService.list("APPLIED", "java", 1, 10)).thenReturn(page(new ApplicationListResponse()));
        when(applicationService.getDetail(3L)).thenReturn(new ApplicationDetailResponse());
        when(applicationService.updateStatus(eq(3L), any(ApplicationStatusUpdateRequest.class))).thenReturn(true);
        when(applicationService.updateNote(eq(3L), any(ApplicationNoteUpdateRequest.class))).thenReturn(true);
        when(applicationService.delete(3L)).thenReturn(true);

        assertThat(applicationController.create(new ApplicationCreateRequest()).getData()).isNotNull();
        assertThat(applicationController.list("APPLIED", "java", 1, 10).getData().getRecords()).hasSize(1);
        assertThat(applicationController.getDetail(3L).getData()).isNotNull();
        assertThat(applicationController.updateStatus(3L, new ApplicationStatusUpdateRequest()).getData()).isTrue();
        assertThat(applicationController.updateNote(3L, new ApplicationNoteUpdateRequest()).getData()).isTrue();
        assertThat(applicationController.delete(3L).getData()).isTrue();

        UserService userService = mock(UserService.class);
        UserProfileService profileService = mock(UserProfileService.class);
        UserController userController = new UserController(userService, profileService);
        when(userService.getCurrentUserInfo()).thenReturn(new AuthUserResponse());
        when(profileService.getCurrentProfile()).thenReturn(new UserProfileVO());
        when(profileService.updateCurrentProfile(any())).thenReturn(new UserProfileVO());
        when(profileService.updateCurrentAvatar(any())).thenReturn(new UserProfileVO());
        MockMultipartFile avatar = new MockMultipartFile("file", "a.png", "image/png", "x".getBytes());

        assertThat(userController.getCurrentUserInfo().getData()).isNotNull();
        assertThat(userController.getCurrentProfile().getData()).isNotNull();
        assertThat(userController.updateCurrentProfile(new UpdateProfileRequest()).getData()).isNotNull();
        assertThat(userController.updateCurrentAvatar(avatar).getData()).isNotNull();
        assertThat(userController.changeCurrentPassword(new ChangePasswordRequest()).getCode()).isEqualTo(200);
        verify(profileService).changeCurrentPassword(any(ChangePasswordRequest.class));

        ResumeVersionService versionService = mock(ResumeVersionService.class);
        ResumeVersionController versionController = new ResumeVersionController(versionService);
        when(versionService.create(eq(10L), any())).thenReturn(new ResumeVersionCreateResponse());
        when(versionService.list(10L)).thenReturn(List.of(new ResumeVersionListResponse()));
        when(versionService.getDetail(10L, 11L)).thenReturn(new ResumeVersionDetailResponse());
        when(versionService.update(eq(10L), eq(11L), any(ResumeVersionUpdateRequest.class))).thenReturn(true);
        when(versionService.setCurrent(10L, 11L)).thenReturn(true);
        when(versionService.delete(10L, 11L)).thenReturn(true);
        when(versionService.optimize(eq(10L), any(ResumeVersionOptimizeRequest.class))).thenReturn(new ResumeVersionCreateResponse());
        when(versionService.compare(10L, 11L, 12L)).thenReturn(new ResumeVersionCompareResponse());

        assertThat(versionController.create(10L, new ResumeVersionCreateRequest()).getData()).isNotNull();
        assertThat(versionController.list(10L).getData()).hasSize(1);
        assertThat(versionController.getDetail(10L, 11L).getData()).isNotNull();
        assertThat(versionController.update(10L, 11L, new ResumeVersionUpdateRequest()).getData()).isTrue();
        assertThat(versionController.setCurrent(10L, 11L).getData()).isTrue();
        assertThat(versionController.delete(10L, 11L).getData()).isTrue();
        assertThat(versionController.optimize(10L, new ResumeVersionOptimizeRequest()).getData()).isNotNull();
        assertThat(versionController.compare(10L, 11L, 12L).getData()).isNotNull();

        RagKnowledgeService ragService = mock(RagKnowledgeService.class);
        AdminRagKnowledgeController ragController = new AdminRagKnowledgeController(ragService);
        when(ragService.create(any())).thenReturn(99L);
        when(ragService.update(eq(99L), any())).thenReturn(true);
        when(ragService.delete(99L)).thenReturn(true);
        when(ragService.rebuildChunks(99L)).thenReturn(true);
        when(ragService.list("backend", "SKILL", 1, 1, 10)).thenReturn(page(new RagKnowledgeListResponse()));
        when(ragService.getDetail(99L)).thenReturn(new RagKnowledgeDetailResponse());
        when(ragService.search(any())).thenReturn(List.of(new RagSearchResultResponse()));

        assertThat(ragController.create(new RagKnowledgeCreateRequest()).getData()).isEqualTo(99L);
        assertThat(ragController.update(99L, new RagKnowledgeUpdateRequest()).getData()).isTrue();
        assertThat(ragController.delete(99L).getData()).isTrue();
        assertThat(ragController.rebuild(99L).getData()).isTrue();
        assertThat(ragController.list("backend", "SKILL", 1, 1, 10).getData().getRecords()).hasSize(1);
        assertThat(ragController.getDetail(99L).getData()).isNotNull();
        assertThat(ragController.search(new RagSearchRequest()).getData()).hasSize(1);
    }

    @Test
    void healthControllerShouldReturnBackendAndAiProviderStatus() {
        HealthController controller = new HealthController();
        ReflectionTestUtils.setField(controller, "aiProvider", "deepseek");

        assertThat(controller.health().getData()).contains("InternPilot");
        assertThat(controller.aiProvider().getData()).containsEntry("provider", "deepseek");
    }

    private static <T> PageResult<T> page(T item) {
        return new PageResult<>(List.of(item), 1L, 1L, 10L, 1L);
    }
}
