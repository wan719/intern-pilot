package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
@Schema(description = "JWT相关配置类，包含JWT密钥和过期时间等信息")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class JwtProperties {

    private String secret;

    private Long expiration;
}
