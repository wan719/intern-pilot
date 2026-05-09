package com.internpilot.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志详情响应")
public class OperationLogDetailResponse {

    private Long logId;

    private Long operatorId;

    private String operatorUsername;

    private String module;

    private String operation;

    private String operationType;

    private String requestUri;

    private String requestMethod;

    private String requestParams;

    private String ipAddress;

    private String userAgent;

    private Integer success;

    private String errorMessage;

    private Long costTime;

    private LocalDateTime createdAt;
}