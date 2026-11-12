package com.internpilot.application.service;

import com.internpilot.common.PageResult;
import com.internpilot.application.dto.ApplicationCreateRequest;
import com.internpilot.application.dto.ApplicationFromReportRequest;
import com.internpilot.application.dto.ApplicationNoteUpdateRequest;
import com.internpilot.application.dto.ApplicationStatusUpdateRequest;
import com.internpilot.application.dto.ApplicationPriorityUpdateRequest;
import com.internpilot.application.vo.ApplicationCreateResponse;
import com.internpilot.application.vo.ApplicationFromReportResponse;
import com.internpilot.application.vo.ApplicationDetailResponse;
import com.internpilot.application.vo.ApplicationListResponse;
import com.internpilot.application.vo.ApplicationPlanContextResponse;

public interface ApplicationService {

    ApplicationCreateResponse create(ApplicationCreateRequest request);

    ApplicationPlanContextResponse getPlanContext(Long reportId);

    ApplicationFromReportResponse createFromReport(ApplicationFromReportRequest request);

    PageResult<ApplicationListResponse> list(
            String status,
            String keyword,
            Integer pageNum,
            Integer pageSize
    );

    ApplicationDetailResponse getDetail(Long id);

    Boolean updateStatus(Long id, ApplicationStatusUpdateRequest request);

    Boolean updatePriority(Long id, ApplicationPriorityUpdateRequest request);

    Boolean updateNote(Long id, ApplicationNoteUpdateRequest request);

    Boolean delete(Long id);
}
