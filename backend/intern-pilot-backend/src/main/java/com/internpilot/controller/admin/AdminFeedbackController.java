package com.internpilot.controller.admin;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.Result;
import com.internpilot.dto.feedback.FeedbackReplyRequest;
import com.internpilot.dto.feedback.FeedbackStatusUpdateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.feedback.FeedbackService;
import com.internpilot.vo.feedback.FeedbackResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员反馈管理接口")
@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "查询反馈列表", description = "查询所有用户反馈")
    @PreAuthorize("hasAuthority('feedback:read')")
    @GetMapping
    public Result<List<FeedbackResponse>> listFeedbacks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return Result.success(feedbackService.listAllFeedbacks(type, status));
    }

    @Operation(summary = "查询反馈详情", description = "查询单个反馈详情")
    @PreAuthorize("hasAuthority('feedback:read')")
    @GetMapping("/{id}")
    public Result<FeedbackResponse> getFeedback(@PathVariable Long id) {
        return Result.success(feedbackService.getFeedbackById(id));
    }

    @Operation(summary = "更新反馈状态", description = "更新反馈处理状态")
    @PreAuthorize("hasAuthority('feedback:write')")
    @OperationLog(module = "用户反馈", operation = "更新反馈状态", type = OperationTypeEnum.UPDATE)
    @PutMapping("/{id}/status")
    public Result<FeedbackResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid FeedbackStatusUpdateRequest request
    ) {
        return Result.success(feedbackService.updateStatus(id, request));
    }

    @Operation(summary = "回复反馈", description = "管理员回复用户反馈")
    @PreAuthorize("hasAuthority('feedback:write')")
    @OperationLog(module = "用户反馈", operation = "回复反馈", type = OperationTypeEnum.UPDATE, recordParams = false)
    @PutMapping("/{id}/reply")
    public Result<FeedbackResponse> reply(
            @PathVariable Long id,
            @RequestBody @Valid FeedbackReplyRequest request
    ) {
        return Result.success(feedbackService.reply(id, request));
    }

    @Operation(summary = "删除反馈", description = "删除反馈记录")
    @PreAuthorize("hasAuthority('feedback:delete')")
    @OperationLog(module = "用户反馈", operation = "删除反馈", type = OperationTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return Result.success();
    }
}
