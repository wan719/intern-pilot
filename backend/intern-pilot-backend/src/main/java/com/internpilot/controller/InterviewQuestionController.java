package com.internpilot.controller;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.InterviewQuestionService;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 面试题生成接口")
@RestController
@RequestMapping("/api/interview-questions")
@RequiredArgsConstructor
public class InterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @Operation(summary = "生成 AI 面试题", description = "根据简历、岗位和分析报告生成岗位定制化面试题")
    @OperationLog(module = "AI面试题", operation = "生成AI面试题", type = OperationTypeEnum.AI, recordParams = false)
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping("/generate")
    public Result<InterviewQuestionGenerateResponse> generate(
            @RequestBody @Valid InterviewQuestionGenerateRequest request
    ) {
        return Result.success(interviewQuestionService.generate(request));
    }

    @Operation(summary = "查询面试题报告列表", description = "分页查询当前用户生成过的面试题报告")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping
    public Result<PageResult<InterviewQuestionListResponse>> list(
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(interviewQuestionService.list(resumeId, jobId, pageNum, pageSize));
    }

    @Operation(summary = "查询面试题报告详情", description = "查询某份面试题报告的完整题目和参考答案")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/{reportId}")
    public Result<InterviewQuestionDetailResponse> getDetail(@PathVariable Long reportId) {
        return Result.success(interviewQuestionService.getDetail(reportId));
    }

    @Operation(summary = "删除面试题报告", description = "逻辑删除某份面试题报告")
    @OperationLog(module = "AI面试题", operation = "删除AI面试题报告", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('analysis:delete')")
    @DeleteMapping("/{reportId}")
    public Result<Boolean> delete(@PathVariable Long reportId) {
        return Result.success(interviewQuestionService.delete(reportId));
    }

    @Operation(summary = "重新生成面试题", description = "基于已有报告重新生成面试题")
    @OperationLog(module = "AI面试题", operation = "重新生成AI面试题", type = OperationTypeEnum.AI, recordParams = false)
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping("/{reportId}/regenerate")
    public Result<InterviewQuestionGenerateResponse> regenerate(@PathVariable Long reportId) {
        return Result.success(interviewQuestionService.regenerate(reportId));
    }
}
