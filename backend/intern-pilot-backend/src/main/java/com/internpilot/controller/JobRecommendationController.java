package com.internpilot.controller;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.recommendation.JobRecommendationGenerateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.JobRecommendationService;
import com.internpilot.vo.recommendation.JobRecommendationBatchDetailResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchListResponse;
import com.internpilot.vo.recommendation.JobRecommendationGenerateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@Tag(name = "岗位推荐接口")
@RestController
@RequestMapping("/api/job-recommendations")
@RequiredArgsConstructor
public class JobRecommendationController {

    private final JobRecommendationService jobRecommendationService;

    @Operation(summary = "生成岗位推荐")
    @OperationLog(module = "岗位推荐", operation = "生成岗位推荐", type = OperationTypeEnum.AI, recordParams = false)
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping("/generate")
    public Result<JobRecommendationGenerateResponse> generate(
            @RequestBody @Valid JobRecommendationGenerateRequest request
    ) {
        return Result.success(jobRecommendationService.generate(request));
    }

    @Operation(summary = "查询岗位推荐历史")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping
    public Result<PageResult<JobRecommendationBatchListResponse>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(jobRecommendationService.list(pageNum, pageSize));
    }

    @Operation(summary = "查询岗位推荐详情")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/{batchId}")
    public Result<JobRecommendationBatchDetailResponse> getDetail(@PathVariable Long batchId) {
        return Result.success(jobRecommendationService.getDetail(batchId));
    }

    @Operation(summary = "删除岗位推荐记录")
    @OperationLog(module = "岗位推荐", operation = "删除岗位推荐记录", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('analysis:delete')")
    @DeleteMapping("/{batchId}")
    public Result<Boolean> delete(@PathVariable Long batchId) {
        return Result.success(jobRecommendationService.delete(batchId));
    }
}