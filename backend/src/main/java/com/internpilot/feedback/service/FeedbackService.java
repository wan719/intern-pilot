package com.internpilot.feedback.service;

import com.internpilot.feedback.dto.FeedbackCreateRequest;
import com.internpilot.feedback.dto.FeedbackReplyRequest;
import com.internpilot.feedback.dto.FeedbackStatusUpdateRequest;
import com.internpilot.feedback.vo.FeedbackResponse;

import java.util.List;

public interface FeedbackService {

    FeedbackResponse createFeedback(FeedbackCreateRequest request);

    FeedbackResponse getFeedbackById(Long id);

    FeedbackResponse getMyFeedbackById(Long id);

    List<FeedbackResponse> listMyFeedbacks();

    List<FeedbackResponse> listAllFeedbacks(String type, String status);

    FeedbackResponse updateStatus(Long id, FeedbackStatusUpdateRequest request);

    FeedbackResponse reply(Long id, FeedbackReplyRequest request);

    void deleteFeedback(Long id);
}
