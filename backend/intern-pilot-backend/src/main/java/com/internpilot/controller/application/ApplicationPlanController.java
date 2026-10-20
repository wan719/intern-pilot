package com.internpilot.controller.application;

import com.internpilot.common.Result;
import com.internpilot.common.ResultCode;
import com.internpilot.dto.application.ApplicationFromReportRequest;
import com.internpilot.exception.BusinessException;
import com.internpilot.service.application.ApplicationService;
import com.internpilot.vo.application.ApplicationFromReportResponse;
import com.internpilot.vo.application.ApplicationPlanContextResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationPlanController {
    private final ApplicationService applicationService;

    @GetMapping("/plan-context")
    @PreAuthorize("hasAuthority('application:read')")
    public ResponseEntity<Result<ApplicationPlanContextResponse>> getPlanContext(HttpServletRequest request) {
        String[] values = request.getParameterValues("reportId");
        if (values == null || values.length != 1 || !values[0].matches("[1-9][0-9]*")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报告 ID 必须为单个正整数");
        }
        final long reportId;
        try {
            reportId = Long.parseLong(values[0]);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报告 ID 超出允许范围");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Result.success(applicationService.getPlanContext(reportId)));
    }

    @PostMapping("/from-report")
    @PreAuthorize("hasAuthority('application:write')")
    public ResponseEntity<Result<ApplicationFromReportResponse>> createFromReport(
            @RequestBody @Valid ApplicationFromReportRequest request) {
        ApplicationFromReportResponse response = applicationService.createFromReport(request);
        HttpStatus status = "CREATED".equals(response.getOutcome()) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(Result.success(response));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handlePlanBusinessException(BusinessException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(Result.fail(status.value(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> handlePlanBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST, "请求参数格式错误，请检查后重试"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handlePlanDataIntegrityFailure(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR, "投递计划暂时无法创建，请稍后重试"));
    }
}
