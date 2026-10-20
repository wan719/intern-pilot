package com.internpilot.service.application;

import com.internpilot.common.PageResult;
import com.internpilot.dto.application.ApplicationCreateRequest;
import com.internpilot.dto.application.ApplicationFromReportRequest;
import com.internpilot.dto.application.ApplicationNoteUpdateRequest;
import com.internpilot.dto.application.ApplicationStatusUpdateRequest;
import com.internpilot.vo.application.ApplicationCreateResponse;
import com.internpilot.vo.application.ApplicationFromReportResponse;
import com.internpilot.vo.application.ApplicationDetailResponse;
import com.internpilot.vo.application.ApplicationListResponse;
import com.internpilot.vo.application.ApplicationPlanContextResponse;

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

    Boolean updateNote(Long id, ApplicationNoteUpdateRequest request);

    Boolean delete(Long id);
}
