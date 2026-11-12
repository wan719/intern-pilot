package com.internpilot.annotation;

import com.internpilot.enums.OperationTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * Operation module.
     */
    String module();

    /**
     * Operation name.
     */
    String operation();

    /**
     * Operation type.
     */
    OperationTypeEnum type() default OperationTypeEnum.OTHER;

    /**
     * Whether to persist sanitized request parameters.
     */
    boolean recordParams() default true;
}
