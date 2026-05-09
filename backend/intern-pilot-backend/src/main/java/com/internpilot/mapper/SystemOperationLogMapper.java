package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.SystemOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemOperationLogMapper extends BaseMapper<SystemOperationLog> {
}