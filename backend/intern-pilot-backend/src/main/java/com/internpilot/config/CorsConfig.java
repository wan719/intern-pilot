package com.internpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Configuration
@Schema(description = "CORS配置类，定义了跨域资源共享的相关配置，包括允许的来源、方法、头信息等")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class CorsConfig {

    @Bean
    @Schema(description = "获取CORS配置源，用于配置跨域资源共享")//这个注解用于Swagger API文档生成，提供了对该方法的描述信息
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
