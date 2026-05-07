package com.internpilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data//这个注解会自动生成getter、setter、toString、equals和hashCode方法，简化代码编写
@TableName("role")//这个注解指定了实体类对应的数据库表名为role
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
