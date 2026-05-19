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
import com.internpilot.service.feedback.FeedbackService;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.feedback.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final UserFeedbackMapper feedbackMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setType(request.getType());
        feedback.setTitle(request.getTitle());
        feedback.setContent(request.getContent());
        feedback.setPageUrl(request.getPageUrl());
        feedback.setContact(request.getContact());
        feedback.setAllowContact(Boolean.TRUE.equals(request.getAllowContact()) ? 1 : 0);
        feedback.setBrowserInfo(request.getBrowserInfo());
        feedback.setStatus(FeedbackStatusEnum.PENDING.getCode());

        feedbackMapper.insert(feedback);
        return toResponse(feedback);
    }

    @Override
    public FeedbackResponse getFeedbackById(Long id) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null || feedback.getDeleted() != null && feedback.getDeleted() == 1) {
            throw new BusinessException("反馈不存在");
        }
        return toResponse(feedback);
    }

    @Override
    public FeedbackResponse getMyFeedbackById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserFeedback feedback = feedbackMapper.selectOne(
                new LambdaQueryWrapper<UserFeedback>()
                        .eq(UserFeedback::getId, id)
                        .eq(UserFeedback::getUserId, userId)
                        .eq(UserFeedback::getDeleted, 0)
                        .last("LIMIT 1"));
        if (feedback == null) {
            throw new BusinessException("反馈不存在或无权访问");
        }
        return toResponse(feedback);
    }

    @Override
    public List<FeedbackResponse> listMyFeedbacks() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<UserFeedback> feedbacks = feedbackMapper.selectList(
                new LambdaQueryWrapper<UserFeedback>()
                        .eq(UserFeedback::getUserId, userId)
                        .eq(UserFeedback::getDeleted, 0)
                        .orderByDesc(UserFeedback::getCreatedAt));
        return feedbacks.stream().map(this::toResponse).toList();
    }

    @Override
    public List<FeedbackResponse> listAllFeedbacks(String type, String status) {
        LambdaQueryWrapper<UserFeedback> query = new LambdaQueryWrapper<UserFeedback>()
                .eq(UserFeedback::getDeleted, 0)
                .orderByDesc(UserFeedback::getCreatedAt);

        if (type != null && !type.isBlank()) {
            query.eq(UserFeedback::getType, type);
        }
        if (status != null && !status.isBlank()) {
            query.eq(UserFeedback::getStatus, status);
        }

        List<UserFeedback> feedbacks = feedbackMapper.selectList(query);
        return feedbacks.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public FeedbackResponse updateStatus(Long id, FeedbackStatusUpdateRequest request) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null || feedback.getDeleted() != null && feedback.getDeleted() == 1) {
            throw new BusinessException("反馈不存在");
        }

        FeedbackStatusEnum statusEnum = FeedbackStatusEnum.fromCode(request.getStatus());

        UserFeedback update = new UserFeedback();
        update.setId(id);
        update.setStatus(statusEnum.getCode());
        if (statusEnum != FeedbackStatusEnum.PENDING) {
            update.setHandledBy(SecurityUtils.getCurrentUserId());
            update.setHandledAt(LocalDateTime.now());
        }
        feedbackMapper.updateById(update);

        feedback.setStatus(statusEnum.getCode());
        return toResponse(feedback);
    }

    @Override
    @Transactional
    public FeedbackResponse reply(Long id, FeedbackReplyRequest request) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null || feedback.getDeleted() != null && feedback.getDeleted() == 1) {
            throw new BusinessException("反馈不存在");
        }

        UserFeedback update = new UserFeedback();
        update.setId(id);
        update.setAdminReply(request.getReply());
        update.setStatus(FeedbackStatusEnum.PROCESSING.getCode());
        update.setHandledBy(SecurityUtils.getCurrentUserId());
        update.setHandledAt(LocalDateTime.now());
        feedbackMapper.updateById(update);

        feedback.setAdminReply(request.getReply());
        feedback.setStatus(FeedbackStatusEnum.PROCESSING.getCode());
        return toResponse(feedback);
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null || feedback.getDeleted() != null && feedback.getDeleted() == 1) {
            throw new BusinessException("反馈不存在");
        }
        feedbackMapper.deleteById(id);
    }

    private FeedbackResponse toResponse(UserFeedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedback.getId());
        response.setUserId(feedback.getUserId());
        response.setType(feedback.getType());
        response.setTypeDescription(FeedbackTypeEnum.fromCode(feedback.getType()).getDescription());
        response.setTitle(feedback.getTitle());
        response.setContent(feedback.getContent());
        response.setPageUrl(feedback.getPageUrl());
        response.setContact(feedback.getContact());
        response.setAllowContact(feedback.getAllowContact() != null && feedback.getAllowContact() == 1);
        response.setBrowserInfo(feedback.getBrowserInfo());
        response.setStatus(feedback.getStatus());
        response.setStatusDescription(FeedbackStatusEnum.fromCode(feedback.getStatus()).getDescription());
        response.setAdminReply(feedback.getAdminReply());
        response.setHandledBy(feedback.getHandledBy());
        response.setHandledAt(feedback.getHandledAt());
        response.setCreatedAt(feedback.getCreatedAt());
        response.setUpdatedAt(feedback.getUpdatedAt());

        User user = userMapper.selectById(feedback.getUserId());
        if (user != null) {
            response.setUserName(user.getRealName() != null && !user.getRealName().isBlank()
                    ? user.getRealName()
                    : user.getUsername());
            response.setUserEmail(user.getEmail());
        }

        if (feedback.getHandledBy() != null) {
            User handler = userMapper.selectById(feedback.getHandledBy());
            if (handler != null) {
                response.setHandledByName(handler.getRealName() != null && !handler.getRealName().isBlank()
                        ? handler.getRealName()
                        : handler.getUsername());
            }
        }

        return response;
    }
}
