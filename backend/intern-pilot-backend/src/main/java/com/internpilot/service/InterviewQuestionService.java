package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;

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