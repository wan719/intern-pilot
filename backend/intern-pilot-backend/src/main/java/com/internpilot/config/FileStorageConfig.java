package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "file")
@Schema(description = "文件存储配置类，包含上传目录、最大文件大小和允许的文件类型等信息")//这个注解用于Swagger API文档生成，提供了对该配置类的描述信息
public class FileStorageConfig {

    private String uploadDir = "uploads/resumes";
    private Long maxSize = 10 * 1024 * 1024L;
    private List<String> allowedTypes = new ArrayList<>();
}
