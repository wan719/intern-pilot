package com.internpilot.annotation;

import com.internpilot.enums.OperationTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.*;

@Target(ElementType.METHOD)//这个注解表示OperationLog注解只能应用于方法上
@Retention(RetentionPolicy.RUNTIME)//这个注解表示OperationLog注解可以在运行时通过反射获取，并且只能应用于方法上
@Documented//这个注解用于标记需要记录操作日志的方法，包含操作模块、操作名称、操作类型和是否记录请求参数等信息
@Schema(description = "操作日志注解，用于标记需要记录操作日志的方法，包含操作模块、操作名称、操作类型和是否记录请求参数等信息")//这个注解用于Swagger API文档生成，提供了对该注解的描述信息
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module();

    /**
     * 操作名称
     */
    String operation();

    /**
     * 操作类型
     */
    OperationTypeEnum type() default OperationTypeEnum.OTHER;

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;
}