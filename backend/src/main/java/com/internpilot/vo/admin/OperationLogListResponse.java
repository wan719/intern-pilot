package com.internpilot.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志列表响应")
public class OperationLogListResponse {

    private Long logId;

    private Long operatorId;

    private String operatorUsername;

    private String module;

    private String operation;

    private String operationType;

    private String requestUri;

    private String requestMethod;

    private String ipAddress;

    private Integer success;

    private Long costTime;

    private LocalDateTime createdAt;
}