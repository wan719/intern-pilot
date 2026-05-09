package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.annotation.OperationLog;
import com.internpilot.service.AdminOperationLogService;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员-系统操作日志接口")
@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "查询系统操作日志列表")
    @PreAuthorize("hasAuthority('system:log:read')")
    @GetMapping
    public Result<PageResult<OperationLogListResponse>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer success,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(
                adminOperationLogService.list(
                        module,
                        operationType,
                        username,
                        success,
                        pageNum,
                        pageSize
                )
        );
    }

    @Operation(summary = "查询系统操作日志详情")
    @PreAuthorize("hasAuthority('system:log:read')")
    @GetMapping("/{id}")
    public Result<OperationLogDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(adminOperationLogService.getDetail(id));
    }

    @Operation(summary = "删除系统操作日志")
    @OperationLog(
            module = "系统日志",
            operation = "删除系统操作日志",
            type = OperationTypeEnum.DELETE
    )
    @PreAuthorize("hasAuthority('system:log:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(adminOperationLogService.delete(id));
    }
}