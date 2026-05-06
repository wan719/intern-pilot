package com.internpilot.controller;

import com.internpilot.common.Result;
import com.internpilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final UserMapper userMapper;

    @GetMapping("/api/test/db")
    public Result<Long> testDb() {
        return Result.success(userMapper.selectCount(null));
    }
}
