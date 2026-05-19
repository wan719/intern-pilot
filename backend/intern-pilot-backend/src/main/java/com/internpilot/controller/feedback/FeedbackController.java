package com.internpilot.controller.feedback;

import com.internpilot.common.Result;
import com.internpilot.dto.feedback.FeedbackCreateRequest;
import com.internpilot.service.feedback.FeedbackService;
import com.internpilot.vo.feedback.FeedbackResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户反馈接口")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈", description = "提交用户反馈")
    @PostMapping
    public Result<FeedbackResponse> createFeedback(@RequestBody @Valid FeedbackCreateRequest request) {
        return Result.success(feedbackService.createFeedback(request));
    }

    @Operation(summary = "查看我的反馈", description = "查看当前用户的反馈列表")
    @GetMapping("/my")
    public Result<List<FeedbackResponse>> listMyFeedbacks() {
        return Result.success(feedbackService.listMyFeedbacks());
    }

    @Operation(summary = "查看我的反馈详情", description = "查看当前用户的单个反馈详情")
    @GetMapping("/my/{id}")
    public Result<FeedbackResponse> getMyFeedback(@PathVariable Long id) {
        return Result.success(feedbackService.getMyFeedbackById(id));
    }
}
