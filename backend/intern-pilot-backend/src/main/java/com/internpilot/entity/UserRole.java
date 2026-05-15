package com.internpilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("user_role")//用户角色表，定义用户和角色的关联关系，每条记录表示一个用户拥有一个角色
@Schema(description = "用户角色实体类，包含用户角色的详细信息和关联的用户和角色")//这个注解用于Swagger API文档生成，提供了对该实体类的描述信息
public class UserRole {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;

    private LocalDateTime createdAt;
    
    @TableLogic
    private Integer deleted;
}
