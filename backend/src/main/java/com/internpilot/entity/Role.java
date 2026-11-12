package com.internpilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data//这个注解会自动生成getter、setter、toString、equals和hashCode方法，简化代码编写
@TableName("role")//角色表，定义系统中的角色，每个角色对应一组权限
@Schema(description = "角色实体类，包含角色的详细信息和对应的权限")//这个注解用于Swagger API文档生成，提供了对该实体类的描述信息
public class Role {
    @TableId(type = IdType.AUTO)//这个注解表示id字段是主键，并且使用自动增长策略，
    // 插入数据时不需要手动设置id值，数据库会自动生成一个唯一的id。
    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic// 逻辑删除字段这个注解会自动处理删除操作，删除时会将deleted字段设置为1，
    // 而不是物理删除记录
    private Integer deleted;
}
