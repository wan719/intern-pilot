package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "用户角色枚举，定义系统中不同的用户角色，每个角色对应一个唯一的code和描述")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum UserRoleEnum {//用户角色枚举，定义系统中不同的用户角色，
// 每个角色对应一个唯一的code和描述

    USER("USER", "普通用户"),
    ADMIN("ADMIN", "系统管理员");

    private final String code;
    private final String description;

    UserRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
