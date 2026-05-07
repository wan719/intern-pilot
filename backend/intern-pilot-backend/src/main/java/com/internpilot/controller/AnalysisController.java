package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.service.AnalysisService;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;
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

@Tag(name = "AI 分析接口")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "简历岗位匹配分析", description = "根据用户简历和岗位 JD 生成 AI 匹配分析报告")
    @PreAuthorize("hasAuthority('analysis:write')")
    @PostMapping("/match")
    public Result<AnalysisResultResponse> match(@RequestBody @Valid AnalysisMatchRequest request) {
        return Result.success(analysisService.match(request));
    }

    @Operation(summary = "查询分析报告列表", description = "分页查询当前用户的历史 AI 分析报告")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/reports")
    public Result<PageResult<AnalysisReportListResponse>> listReports(
            @RequestParam(required = false) Long resumeId,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(analysisService.listReports(resumeId, jobId, minScore, pageNum, pageSize));
    }

    @Operation(summary = "查询分析报告详情", description = "查询当前用户某份 AI 分析报告的完整内容")
    @PreAuthorize("hasAuthority('analysis:read')")
    @GetMapping("/reports/{id}")
    public Result<AnalysisReportDetailResponse> getReportDetail(@PathVariable Long id) {
        return Result.success(analysisService.getReportDetail(id));
    }
}
