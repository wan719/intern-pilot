package com.internpilot.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
