package com.internpilot.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.application.entity.ApplicationRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationRecordMapper extends BaseMapper<ApplicationRecord> {
}
