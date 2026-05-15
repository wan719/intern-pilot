package com.internpilot.controller;

import com.internpilot.common.Result;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@Tag(name = "健康检查接口")//这个注解用于Swagger API文档生成，提供了对该控制器的描述信息
@RestController
public class HealthController {

    @Value("${ai.provider:mock}")
    private String aiProvider;

    @GetMapping("/api/health")
    public Result<String> health() {
        return Result.success("InternPilot backend is running");
    }

    @GetMapping("/api/health/ai-provider")
    public Result<Map<String, String>> aiProvider() {
        return Result.success(Map.of("provider", aiProvider));
    }
}