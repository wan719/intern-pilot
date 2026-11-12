package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Update("""
            UPDATE role_permission
            SET deleted = 1
            WHERE role_id = #{roleId}
              AND deleted = 0
            """)
    int disableByRoleId(@Param("roleId") Long roleId);

    @Update("""
            INSERT INTO role_permission (role_id, permission_id, deleted)
            VALUES (#{roleId}, #{permissionId}, 0)
            ON DUPLICATE KEY UPDATE deleted = 0
            """)
    int upsertActive(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
