package com.internpilot.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.audit.entity.SystemOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemOperationLogMapper extends BaseMapper<SystemOperationLog> {
}