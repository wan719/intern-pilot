package com.internpilot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Schema(description = "MyBatis-Plus配置类，定义了MyBatis-Plus的相关配置，包括分页插件等")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class MyBatisPlusConfig {

    @Bean
    @Schema(description = "获取MyBatis-Plus拦截器，包含分页插件等配置")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(100L);

        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}