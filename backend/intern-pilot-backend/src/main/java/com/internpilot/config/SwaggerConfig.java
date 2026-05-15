package com.internpilot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Schema(description = "Swagger配置类，定义了OpenAPI实例，用于生成API文档并配置安全方案")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI internPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InternPilot API 文档")
                        .description("面向大学生的 AI 实习投递与简历优化平台接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("InternPilot")
                                .email("QQ:3425446714@qq.com;google:lihongli367@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}