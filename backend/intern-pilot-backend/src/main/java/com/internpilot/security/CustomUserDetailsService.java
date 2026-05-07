package com.internpilot.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.entity.User;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
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