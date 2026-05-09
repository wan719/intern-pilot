package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.service.AdminOperationLogService;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private final SystemOperationLogMapper systemOperationLogMapper;

    @Override
    public PageResult<OperationLogListResponse> list(
            String module,
            String operationType,
            String username,
            Integer success,
            Integer pageNum,
            Integer pageSize
    ) {
        LambdaQueryWrapper<SystemOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemOperationLog::getDeleted, 0);

        if (StringUtils.hasText(module)) {
            wrapper.like(SystemOperationLog::getModule, module.trim());
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(SystemOperationLog::getOperationType, operationType.trim());
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(SystemOperationLog::getOperatorUsername, username.trim());
        }
        if (success != null) {
            wrapper.eq(SystemOperationLog::getSuccess, success);
        }

        wrapper.orderByDesc(SystemOperationLog::getCreatedAt)
                .orderByDesc(SystemOperationLog::getId);

        Page<SystemOperationLog> page = new Page<>(pageNum, pageSize);
        Page<SystemOperationLog> resultPage = systemOperationLogMapper.selectPage(page, wrapper);

        List<OperationLogListResponse> records = resultPage.getRecords()
                .stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public OperationLogDetailResponse getDetail(Long id) {
        SystemOperationLog log = systemOperationLogMapper.selectOne(
                new LambdaQueryWrapper<SystemOperationLog>()
                        .eq(SystemOperationLog::getId, id)
                        .eq(SystemOperationLog::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (log == null) {
            throw new BusinessException("操作日志不存在");
        }

        return toDetailResponse(log);
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        SystemOperationLog log = systemOperationLogMapper.selectById(id);

        if (log == null || Integer.valueOf(1).equals(log.getDeleted())) {
            throw new BusinessException("操作日志不存在");
        }

        log.setDeleted(1);
        systemOperationLogMapper.updateById(log);
        return true;
    }

    private OperationLogListResponse toListResponse(SystemOperationLog log) {
        OperationLogListResponse response = new OperationLogListResponse();
        response.setLogId(log.getId());
        response.setOperatorId(log.getOperatorId());
        response.setOperatorUsername(log.getOperatorUsername());
        response.setModule(log.getModule());
        response.setOperation(log.getOperation());
        response.setOperationType(log.getOperationType());
        response.setRequestUri(log.getRequestUri());
        response.setRequestMethod(log.getRequestMethod());
        response.setIpAddress(log.getIpAddress());
        response.setSuccess(log.getSuccess());
        response.setCostTime(log.getCostTime());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private OperationLogDetailResponse toDetailResponse(SystemOperationLog log) {
        OperationLogDetailResponse response = new OperationLogDetailResponse();
        response.setLogId(log.getId());
        response.setOperatorId(log.getOperatorId());
        response.setOperatorUsername(log.getOperatorUsername());
        response.setModule(log.getModule());
        response.setOperation(log.getOperation());
        response.setOperationType(log.getOperationType());
        response.setRequestUri(log.getRequestUri());
        response.setRequestMethod(log.getRequestMethod());
        response.setRequestParams(log.getRequestParams());
        response.setIpAddress(log.getIpAddress());
        response.setUserAgent(log.getUserAgent());
        response.setSuccess(log.getSuccess());
        response.setErrorMessage(log.getErrorMessage());
        response.setCostTime(log.getCostTime());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
