package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;

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