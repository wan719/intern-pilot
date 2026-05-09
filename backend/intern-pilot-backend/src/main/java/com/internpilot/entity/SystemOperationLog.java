package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_operation_log")
public class SystemOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @TableLogic
    private Integer deleted;
}