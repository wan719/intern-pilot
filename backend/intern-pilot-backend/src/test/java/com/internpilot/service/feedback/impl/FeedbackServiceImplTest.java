package com.internpilot.service.feedback.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.feedback.FeedbackCreateRequest;
import com.internpilot.dto.feedback.FeedbackReplyRequest;
import com.internpilot.dto.feedback.FeedbackStatusUpdateRequest;
import com.internpilot.entity.User;
import com.internpilot.entity.UserFeedback;
import com.internpilot.enums.FeedbackStatusEnum;
import com.internpilot.enums.FeedbackTypeEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.UserFeedbackMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.feedback.FeedbackResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private UserFeedbackMapper feedbackMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FeedbackServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createFeedbackShouldPersistCurrentUserFeedback() {
        mockLoginUser(5L);
        when(userMapper.selectById(5L)).thenReturn(user(5L, "student", "学生"));
        doAnswer(invocation -> {
            UserFeedback feedback = invocation.getArgument(0);
            feedback.setId(12L);
            feedback.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(feedbackMapper).insert(any(UserFeedback.class));

        FeedbackResponse response = service.createFeedback(createRequest());

        assertEquals(12L, response.getId());
        assertEquals(5L, response.getUserId());
        assertEquals(FeedbackStatusEnum.PENDING.getCode(), response.getStatus());
        assertTrue(response.getAllowContact());

        ArgumentCaptor<UserFeedback> captor = ArgumentCaptor.forClass(UserFeedback.class);
        verify(feedbackMapper).insert(captor.capture());
        assertEquals("BUG", captor.getValue().getType());
        assertEquals(1, captor.getValue().getAllowContact());
    }

    @Test
    void getAndListShouldMapUserAndHandlerNames() {
        mockLoginUser(5L);
        UserFeedback feedback = feedback(12L);
        feedback.setHandledBy(99L);
        when(feedbackMapper.selectById(12L)).thenReturn(feedback);
        when(feedbackMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(feedback);
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(feedback));
        when(userMapper.selectById(5L)).thenReturn(user(5L, "student", "学生"));
        when(userMapper.selectById(99L)).thenReturn(user(99L, "admin", ""));

        FeedbackResponse detail = service.getFeedbackById(12L);
        FeedbackResponse myDetail = service.getMyFeedbackById(12L);
        List<FeedbackResponse> mine = service.listMyFeedbacks();
        List<FeedbackResponse> all = service.listAllFeedbacks("BUG", "PENDING");

        assertEquals("学生", detail.getUserName());
        assertEquals("admin", myDetail.getHandledByName());
        assertEquals(1, mine.size());
        assertEquals(1, all.size());
    }

    @Test
    void updateStatusAndReplyShouldSetHandler() {
        mockLoginUser(99L);
        when(feedbackMapper.selectById(12L)).thenReturn(feedback(12L));
        when(userMapper.selectById(5L)).thenReturn(user(5L, "student", ""));

        FeedbackStatusUpdateRequest statusRequest = new FeedbackStatusUpdateRequest();
        statusRequest.setStatus(FeedbackStatusEnum.RESOLVED.getCode());
        FeedbackResponse statusResponse = service.updateStatus(12L, statusRequest);

        FeedbackReplyRequest replyRequest = new FeedbackReplyRequest();
        replyRequest.setReply("已修复");
        FeedbackResponse replyResponse = service.reply(12L, replyRequest);

        assertEquals(FeedbackStatusEnum.RESOLVED.getCode(), statusResponse.getStatus());
        assertEquals(FeedbackStatusEnum.PROCESSING.getCode(), replyResponse.getStatus());
        assertEquals("已修复", replyResponse.getAdminReply());
        verify(feedbackMapper, org.mockito.Mockito.times(2)).updateById(any(UserFeedback.class));
    }

    @Test
    void updatePendingStatusShouldNotSetHandler() {
        mockLoginUser(99L);
        when(feedbackMapper.selectById(12L)).thenReturn(feedback(12L));
        when(userMapper.selectById(5L)).thenReturn(user(5L, "student", ""));

        FeedbackStatusUpdateRequest request = new FeedbackStatusUpdateRequest();
        request.setStatus(FeedbackStatusEnum.PENDING.getCode());

        FeedbackResponse response = service.updateStatus(12L, request);

        assertEquals(FeedbackStatusEnum.PENDING.getCode(), response.getStatus());
        ArgumentCaptor<UserFeedback> captor = ArgumentCaptor.forClass(UserFeedback.class);
        verify(feedbackMapper).updateById(captor.capture());
        assertEquals(null, captor.getValue().getHandledBy());
    }

    @Test
    void deleteAndMissingFeedbackShouldBehaveCorrectly() {
        when(feedbackMapper.selectById(12L)).thenReturn(feedback(12L));
        service.deleteFeedback(12L);
        verify(feedbackMapper).deleteById(12L);

        when(feedbackMapper.selectById(13L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getFeedbackById(13L));

        UserFeedback deleted = feedback(14L);
        deleted.setDeleted(1);
        when(feedbackMapper.selectById(14L)).thenReturn(deleted);
        assertThrows(BusinessException.class, () -> service.deleteFeedback(14L));
    }

    @Test
    void enumsShouldFallbackForUnknownCodes() {
        assertEquals(FeedbackTypeEnum.OTHER, FeedbackTypeEnum.fromCode("UNKNOWN"));
        assertEquals(FeedbackStatusEnum.PENDING, FeedbackStatusEnum.fromCode("UNKNOWN"));
        assertFalse(FeedbackTypeEnum.BUG.getDescription().isBlank());
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails principal = new CustomUserDetails(userId, "tester", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private FeedbackCreateRequest createRequest() {
        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setType(FeedbackTypeEnum.BUG.getCode());
        request.setTitle("页面异常");
        request.setContent("点击后空白");
        request.setContact("tester@example.com");
        request.setAllowContact(true);
        request.setPageUrl("/dashboard");
        request.setBrowserInfo("Chrome");
        return request;
    }

    private UserFeedback feedback(Long id) {
        UserFeedback feedback = new UserFeedback();
        feedback.setId(id);
        feedback.setUserId(5L);
        feedback.setType(FeedbackTypeEnum.BUG.getCode());
        feedback.setTitle("页面异常");
        feedback.setContent("点击后空白");
        feedback.setContact("tester@example.com");
        feedback.setAllowContact(0);
        feedback.setPageUrl("/dashboard");
        feedback.setBrowserInfo("Chrome");
        feedback.setStatus(FeedbackStatusEnum.PENDING.getCode());
        feedback.setDeleted(0);
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        return feedback;
    }

    private User user(Long id, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setEmail(username + "@example.com");
        return user;
    }
}
