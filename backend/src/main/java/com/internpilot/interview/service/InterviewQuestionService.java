package com.internpilot.interview.service;

import com.internpilot.common.PageResult;
import com.internpilot.interview.dto.InterviewQuestionGenerateRequest;
import com.internpilot.interview.vo.InterviewQuestionDetailResponse;
import com.internpilot.interview.vo.InterviewQuestionGenerateResponse;
import com.internpilot.interview.vo.InterviewQuestionListResponse;

public interface InterviewQuestionService {

    InterviewQuestionGenerateResponse generate(InterviewQuestionGenerateRequest request);

    PageResult<InterviewQuestionListResponse> list(
            Long resumeId,
            Long jobId,
            Integer pageNum,
            Integer pageSize
    );

    InterviewQuestionDetailResponse getDetail(Long reportId);

    Boolean delete(Long reportId);

    InterviewQuestionGenerateResponse regenerate(Long reportId);
}