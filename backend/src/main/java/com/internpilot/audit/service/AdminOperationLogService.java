package com.internpilot.audit.service;

import com.internpilot.common.PageResult;
import com.internpilot.audit.vo.OperationLogDetailResponse;
import com.internpilot.audit.vo.OperationLogListResponse;

public interface AdminOperationLogService {

    PageResult<OperationLogListResponse> list(
            String module,
            String operationType,
            String username,
            Integer success,
            Integer pageNum,
            Integer pageSize
    );

    OperationLogDetailResponse getDetail(Long id);

    Boolean delete(Long id);
}