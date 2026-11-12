package com.internpilot.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.vo.admin.OperationLogDetailResponse;
import com.internpilot.vo.admin.OperationLogListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogServiceImplTest {

    @Mock
    private SystemOperationLogMapper mapper;

    @InjectMocks
    private AdminOperationLogServiceImpl service;

    @Test
    void listShouldReturnPagedOperationLogsWithFilters() {
        Page<SystemOperationLog> page = new Page<>(1, 10);
        page.setRecords(List.of(log()));
        page.setTotal(1L);
        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<OperationLogListResponse> result = service.list("AI", "CREATE", "admin", 1, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("AI", result.getRecords().get(0).getModule());
        assertEquals("/api/test", result.getRecords().get(0).getRequestUri());
    }

    @Test
    void detailShouldReturnFullLogAndRejectMissing() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(log()).thenReturn(null);

        OperationLogDetailResponse response = service.getDetail(1L);

        assertEquals("params", response.getRequestParams());
        assertEquals("agent", response.getUserAgent());
        assertThrows(BusinessException.class, () -> service.getDetail(2L));
    }

    @Test
    void deleteShouldSoftDeleteExistingLog() {
        SystemOperationLog log = log();
        when(mapper.selectById(1L)).thenReturn(log);

        assertTrue(service.delete(1L));

        assertEquals(1, log.getDeleted());
        verify(mapper).updateById(log);
    }

    @Test
    void deleteShouldRejectMissingOrDeletedLog() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(1L));

        SystemOperationLog deleted = log();
        deleted.setDeleted(1);
        when(mapper.selectById(2L)).thenReturn(deleted);
        assertThrows(BusinessException.class, () -> service.delete(2L));
    }

    private SystemOperationLog log() {
        SystemOperationLog log = new SystemOperationLog();
        log.setId(1L);
        log.setOperatorId(99L);
        log.setOperatorUsername("admin");
        log.setModule("AI");
        log.setOperation("create");
        log.setOperationType("CREATE");
        log.setRequestUri("/api/test");
        log.setRequestMethod("POST");
        log.setRequestParams("params");
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("agent");
        log.setSuccess(1);
        log.setErrorMessage(null);
        log.setCostTime(12L);
        log.setDeleted(0);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
