package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;

public interface ApplicationService {

    ApplicationCreateResponse create(ApplicationCreateRequest request);

    PageResult<ApplicationListResponse> list(
            String status,
            String keyword,
            Integer pageNum,
            Integer pageSize
    );

    ApplicationDetailResponse getDetail(Long id);

    Boolean updateStatus(Long id, ApplicationStatusUpdateRequest request);

    Boolean updateNote(Long id, ApplicationNoteUpdateRequest request);

    Boolean delete(Long id);
}
