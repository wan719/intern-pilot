package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
@Schema(description = "AI相关配置类，包含AI服务提供商、API密钥、基础URL、模型名称和请求超时时间等信息")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class AiProperties {

    private String provider = "deepseek";

    private String apiKey;

    private String baseUrl = "https://api.deepseek.com";

    private String model = "deepseek-v4-flash";

    private String proModel = "deepseek-v4-pro";

    private Long timeoutSeconds = 60L;
}
