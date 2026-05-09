package com.internpilot.controller;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.JobService;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "岗位 JD 管理接口")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "创建岗位 JD", description = "创建目标实习岗位 JD，用于后续 AI 匹配分析")
    @OperationLog(module = "岗位管理", operation = "创建岗位", type = OperationTypeEnum.CREATE)
    @PreAuthorize("hasAuthority('job:write')")
    @PostMapping
    public Result<JobCreateResponse> create(@RequestBody @Valid JobCreateRequest request) {
        return Result.success(jobService.create(request));
    }

    @Operation(summary = "查询岗位列表", description = "分页查询当前用户保存的岗位 JD")
    @PreAuthorize("hasAuthority('job:read')")
    @GetMapping
    public Result<PageResult<JobListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(jobService.list(keyword, jobType, location, pageNum, pageSize));
    }

    @Operation(summary = "查询岗位详情", description = "查询当前用户某个岗位 JD 的完整详情")
    @PreAuthorize("hasAuthority('job:read')")
    @GetMapping("/{id}")
    public Result<JobDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(jobService.getDetail(id));
    }

    @Operation(summary = "修改岗位 JD", description = "修改当前用户保存的岗位 JD")
    @OperationLog(module = "岗位管理", operation = "修改岗位", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('job:write')")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid JobUpdateRequest request) {
        return Result.success(jobService.update(id, request));
    }

    @Operation(summary = "删除岗位 JD", description = "逻辑删除当前用户保存的岗位 JD")
    @OperationLog(module = "岗位管理", operation = "删除岗位", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('job:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(jobService.delete(id));
    }
}
