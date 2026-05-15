package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role_permission")//角色权限表，定义角色和权限的关联关系，
// 每条记录表示一个角色拥有一个权限
@Schema(description = "角色权限实体类，包含角色权限的详细信息和关联的角色和权限")//这个注解用于Swagger API文档生成，提供了对该实体类的描述信息
public class RolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long permissionId;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}