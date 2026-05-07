package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("permission")
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