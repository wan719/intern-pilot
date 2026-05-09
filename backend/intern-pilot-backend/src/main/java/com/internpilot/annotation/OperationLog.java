package com.internpilot.annotation;

import com.internpilot.enums.OperationTypeEnum;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
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