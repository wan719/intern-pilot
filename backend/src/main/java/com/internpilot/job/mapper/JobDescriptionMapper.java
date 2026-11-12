package com.internpilot.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.job.entity.JobDescription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobDescriptionMapper extends BaseMapper<JobDescription> {
}
