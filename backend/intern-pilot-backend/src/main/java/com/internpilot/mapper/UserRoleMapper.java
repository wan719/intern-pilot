package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Update("""
            UPDATE user_role
            SET deleted = 1
            WHERE user_id = #{userId}
              AND deleted = 0
            """)
    int disableByUserId(@Param("userId") Long userId);

    @Update("""
            INSERT INTO user_role (user_id, role_id, deleted)
            VALUES (#{userId}, #{roleId}, 0)
            ON DUPLICATE KEY UPDATE deleted = 0
            """)
    int upsertActive(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
