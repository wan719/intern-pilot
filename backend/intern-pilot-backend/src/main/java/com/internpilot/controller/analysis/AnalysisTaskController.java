package com.internpilot.controller.analysis;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.Result;
import com.internpilot.dto.analysis.AnalysisTaskCreateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.analysis.AnalysisTaskService;
import com.internpilot.vo.analysis.AnalysisTaskCreateResponse;
import com.internpilot.vo.analysis.AnalysisTaskDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI 分析任务接口")
@RestController
@RequestMapping("/api/analysis/tasks")
@RequiredArgsConstructor
public class AnalysisTaskController {

    private final AnalysisTaskService analysisTaskService;

    @Operation(summary = "创建 AI 分析任务", description = "创建异步 AI 分析任务，并通过 WebSocket 推送进")
    @OperationLog(module = "AI分析", operation = "创建AI分析任务", type = OperationTypeEnum.AI, recordParams = false)
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping
    public Result<AnalysisTaskCreateResponse> createTask(
            @RequestBody @Valid AnalysisTaskCreateRequest request) {
        return Result.success(analysisTaskService.createTask(request));
    }

    @Operation(summary = "查询 AI 分析任务详情", description = "根据 taskNo 查询任务状态和进度")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/{taskNo}")
    public Result<AnalysisTaskDetailResponse> getTaskDetail(@PathVariable String taskNo) {
        return Result.success(analysisTaskService.getTaskDetail(taskNo));
    }

    @Operation(summary = "查询当前用户运行中任务", description = "查询当前用户正在执行的 AI 分析任务")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/running")
    public Result<List<AnalysisTaskDetailResponse>> listRunningTasks() {
        return Result.success(analysisTaskService.listRunningTasks());
    }

    @Operation(summary = "取消 AI 分析任务", description = "取消正在执行的 AI 分析任务")
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping("/{taskNo}/cancel")
    public Result<AnalysisTaskDetailResponse> cancelTask(@PathVariable String taskNo) {
        return Result.success(analysisTaskService.cancelTask(taskNo));
    }

    @Operation(summary = "查询最近任务", description = "查询当前用户最近的 AI 分析任务")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/recent")
    public Result<List<AnalysisTaskDetailResponse>> listRecentTasks(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(analysisTaskService.listRecentTasks(limit));
    }
}
