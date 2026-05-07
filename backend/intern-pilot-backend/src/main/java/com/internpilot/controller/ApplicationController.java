package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.service.ApplicationService;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;
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

@Tag(name = "投递记录管理接口")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "创建投递记录", description = "为当前用户的某个岗位创建投递记录")
    @PreAuthorize("hasAuthority('application:write')")
    @PostMapping
    public Result<ApplicationCreateResponse> create(@RequestBody @Valid ApplicationCreateRequest request) {
        return Result.success(applicationService.create(request));
    }

    @Operation(summary = "查询投递记录列表", description = "分页查询当前用户的投递记录")
    @PreAuthorize("hasAuthority('application:read')")
    @GetMapping
    public Result<PageResult<ApplicationListResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(applicationService.list(status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "查询投递记录详情", description = "查询当前用户某条投递记录详情")
    @PreAuthorize("hasAuthority('application:read')")
    @GetMapping("/{id}")
    public Result<ApplicationDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(applicationService.getDetail(id));
    }

    @Operation(summary = "修改投递状态", description = "修改当前用户某条投递记录的状态")
    @PreAuthorize("hasAuthority('application:write')")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid ApplicationStatusUpdateRequest request) {
        return Result.success(applicationService.updateStatus(id, request));
    }

    @Operation(summary = "修改投递备注", description = "修改当前用户某条投递记录的备注、复盘和面试时间")
    @PreAuthorize("hasAuthority('application:write')")
    @PutMapping("/{id}/note")
    public Result<Boolean> updateNote(
            @PathVariable Long id,
            @RequestBody @Valid ApplicationNoteUpdateRequest request) {
        return Result.success(applicationService.updateNote(id, request));
    }

    @Operation(summary = "删除投递记录", description = "逻辑删除当前用户某条投递记录")
    @PreAuthorize("hasAuthority('application:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(applicationService.delete(id));
    }
}
