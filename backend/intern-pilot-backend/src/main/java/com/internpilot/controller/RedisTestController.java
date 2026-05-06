package com.internpilot.controller;

import com.internpilot.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/api/test/redis")
    public Result<Object> testRedis() {
        redisTemplate.opsForValue().set("internpilot:test", "redis ok");
        return Result.success(redisTemplate.opsForValue().get("internpilot:test"));
    }
}
