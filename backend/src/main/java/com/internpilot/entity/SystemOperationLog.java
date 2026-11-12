package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_operation_log")//系统操作日志表，记录系统中的重要操作和事件，
// 便于审计和问题排查
@Schema(description = "系统操作日志实体类，包含系统操作日志的详细信息和关联的操作人")//这个注解用于Swagger API文档生成，提供了对该实体类的描述信息
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