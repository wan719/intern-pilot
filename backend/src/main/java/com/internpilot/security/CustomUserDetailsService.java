package com.internpilot.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.User;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.UserMapper;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Schema(description = "自定义用户详情服务类，实现了Spring Security的UserDetailsService接口，负责根据用户名加载用户的详细信息，包括角色和权限等")//这个注解用于Swagger API文档生成，提供了对该服务类的描述信息
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<String> roles = permissionMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(user.getId());

        if (roles == null || roles.isEmpty()) {
            roles = List.of("USER");
        }

        return new CustomUserDetails(user, roles, permissions);
    }
}