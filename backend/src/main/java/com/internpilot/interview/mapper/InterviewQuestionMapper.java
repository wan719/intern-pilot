package com.internpilot.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.interview.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {
}