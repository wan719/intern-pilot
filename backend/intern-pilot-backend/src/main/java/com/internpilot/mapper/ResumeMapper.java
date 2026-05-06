package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
