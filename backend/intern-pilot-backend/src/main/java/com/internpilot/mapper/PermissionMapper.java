package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM permission p
            JOIN role_permission rp ON p.id = rp.permission_id
            JOIN role r ON rp.role_id = r.id
            JOIN user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.deleted = 0
              AND r.deleted = 0
              AND r.enabled = 1
              AND rp.deleted = 0
              AND p.deleted = 0
              AND p.enabled = 1
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT r.role_code
            FROM role r
            JOIN user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.deleted = 0
              AND r.deleted = 0
              AND r.enabled = 1
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
