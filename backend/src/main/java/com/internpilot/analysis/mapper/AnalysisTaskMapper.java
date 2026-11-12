package com.internpilot.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.analysis.entity.AnalysisTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisTaskMapper extends BaseMapper<AnalysisTask> {
}