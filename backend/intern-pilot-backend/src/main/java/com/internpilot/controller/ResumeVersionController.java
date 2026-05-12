package com.internpilot.controller;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.Result;
import com.internpilot.dto.resume.ResumeVersionCreateRequest;
import com.internpilot.dto.resume.ResumeVersionOptimizeRequest;
import com.internpilot.dto.resume.ResumeVersionUpdateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.ResumeVersionService;
import com.internpilot.vo.resume.ResumeVersionCompareResponse;
import com.internpilot.vo.resume.ResumeVersionCreateResponse;
import com.internpilot.vo.resume.ResumeVersionDetailResponse;
import com.internpilot.vo.resume.ResumeVersionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumes/{resumeId}/versions")
@RequiredArgsConstructor
public class ResumeVersionController {

    private final ResumeVersionService resumeVersionService;

    @Operation(summary = "创建简历版本")
    @OperationLog(module = "简历版本", operation = "创建简历版本", type = OperationTypeEnum.CREATE)
    @PreAuthorize("hasAuthority('resume:write')")
    @PostMapping
    public Result<ResumeVersionCreateResponse> create(
            @PathVariable Long resumeId,
            @RequestBody @Valid ResumeVersionCreateRequest request
    ) {
        return Result.success(resumeVersionService.create(resumeId, request));
    }

    @Operation(summary = "查询简历版本列表")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping
    public Result<List<ResumeVersionListResponse>> list(@PathVariable Long resumeId) {
        return Result.success(resumeVersionService.list(resumeId));
    }

    @Operation(summary = "查询简历版本详情")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping("/{versionId}")
    public Result<ResumeVersionDetailResponse> getDetail(
            @PathVariable Long resumeId,
            @PathVariable Long versionId
    ) {
        return Result.success(resumeVersionService.getDetail(resumeId, versionId));
    }

    @Operation(summary = "修改简历版本")
    @OperationLog(module = "简历版本", operation = "修改简历版本", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('resume:write')")
    @PutMapping("/{versionId}")
    public Result<Boolean> update(
            @PathVariable Long resumeId,
            @PathVariable Long versionId,
            @RequestBody @Valid ResumeVersionUpdateRequest request
    ) {
        return Result.success(resumeVersionService.update(resumeId, versionId, request));
    }

    @Operation(summary = "设置当前简历版本")
    @OperationLog(module = "简历版本", operation = "设置当前简历版本", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('resume:write')")
    @PutMapping("/{versionId}/current")
    public Result<Boolean> setCurrent(
            @PathVariable Long resumeId,
            @PathVariable Long versionId
    ) {
        return Result.success(resumeVersionService.setCurrent(resumeId, versionId));
    }

    @Operation(summary = "删除简历版本")
    @OperationLog(module = "简历版本", operation = "删除简历版本", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('resume:delete')")
    @DeleteMapping("/{versionId}")
    public Result<Boolean> delete(
            @PathVariable Long resumeId,
            @PathVariable Long versionId
    ) {
        return Result.success(resumeVersionService.delete(resumeId, versionId));
    }

    @Operation(summary = "AI优化生成简历版本")
    @OperationLog(module = "简历版本", operation = "AI优化简历版本", type = OperationTypeEnum.AI, recordParams = false)
    @PreAuthorize("hasAuthority('resume:write')")
    @PostMapping("/optimize")
    public Result<ResumeVersionCreateResponse> optimize(
            @PathVariable Long resumeId,
            @RequestBody @Valid ResumeVersionOptimizeRequest request
    ) {
        return Result.success(resumeVersionService.optimize(resumeId, request));
    }

    @Operation(summary = "对比两个简历版本")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping("/compare")
    public Result<ResumeVersionCompareResponse> compare(
            @PathVariable Long resumeId,
            @RequestParam Long oldVersionId,
            @RequestParam Long newVersionId
    ) {
        return Result.success(resumeVersionService.compare(resumeId, oldVersionId, newVersionId));
    }
}