package com.internpilot.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}