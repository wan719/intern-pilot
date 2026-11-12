package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("permission")//权限表，定义系统中的权限，每个权限对应一个资源和操作
@Schema(description = "权限实体类，包含权限的详细信息和对应的资源和操作")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String permissionCode;

    private String permissionName;

    private String resourceType;

    private String description;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic// 逻辑删除字段这个注解会自动处理删除操作，删除时会将deleted字段设置为1，
    private Integer deleted;
}