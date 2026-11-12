package com.internpilot.service.feedback;

import com.internpilot.dto.feedback.FeedbackCreateRequest;
import com.internpilot.dto.feedback.FeedbackReplyRequest;
import com.internpilot.dto.feedback.FeedbackStatusUpdateRequest;
import com.internpilot.vo.feedback.FeedbackResponse;

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
