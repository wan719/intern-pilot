package com.internpilot.aspect;

import com.internpilot.annotation.OperationLog;
import com.internpilot.entity.SystemOperationLog;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.mapper.SystemOperationLogMapper;
import com.internpilot.security.CustomUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogAspectTest {

    @Mock
    private SystemOperationLogMapper systemOperationLogMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private OperationLogAspect operationLogAspect;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRecordSuccessLogWhenMethodSucceeds() throws Throwable {
        mockLoginUser(1L, "admin");

        OperationLog operationLog = createMockOperationLog("管理员", "禁用用户", OperationTypeEnum.DISABLE);

        when(joinPoint.proceed()).thenReturn("success");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        doAnswer(invocation -> {
            SystemOperationLog log = invocation.getArgument(0);
            log.setId(1L);
            return 1;
        }).when(systemOperationLogMapper).insert(any(SystemOperationLog.class));

        Object result = operationLogAspect.recordOperationLog(joinPoint, operationLog);

        assertEquals("success", result);

        ArgumentCaptor<SystemOperationLog> captor = ArgumentCaptor.forClass(SystemOperationLog.class);
        verify(systemOperationLogMapper).insert(captor.capture());
        SystemOperationLog log = captor.getValue();
        assertEquals(1, log.getSuccess());
        assertEquals("管理员", log.getModule());
        assertEquals("禁用用户", log.getOperation());
        assertEquals("DISABLE", log.getOperationType());
        assertEquals(1L, log.getOperatorId());
        assertEquals("admin", log.getOperatorUsername());
    }

    @Test
    void shouldRecordFailureLogWhenMethodThrows() throws Throwable {
        mockLoginUser(1L, "admin");

        OperationLog operationLog = createMockOperationLog("管理员", "删除用户", OperationTypeEnum.DELETE);

        RuntimeException exception = new RuntimeException("用户不存在");
        when(joinPoint.proceed()).thenThrow(exception);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        doAnswer(invocation -> {
            SystemOperationLog log = invocation.getArgument(0);
            log.setId(1L);
            return 1;
        }).when(systemOperationLogMapper).insert(any(SystemOperationLog.class));

        assertThrows(RuntimeException.class, () -> {
            operationLogAspect.recordOperationLog(joinPoint, operationLog);
        });

        ArgumentCaptor<SystemOperationLog> captor = ArgumentCaptor.forClass(SystemOperationLog.class);
        verify(systemOperationLogMapper).insert(captor.capture());
        SystemOperationLog log = captor.getValue();
        assertEquals(0, log.getSuccess());
        assertEquals("用户不存在", log.getErrorMessage());
        assertEquals("DELETE", log.getOperationType());
    }

    @Test
    void shouldRecordLogWithCorrectModuleAndOperation() throws Throwable {
        mockLoginUser(2L, "wan");

        OperationLog operationLog = createMockOperationLog("AI面试题", "生成AI面试题", OperationTypeEnum.AI);

        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        doAnswer(invocation -> {
            SystemOperationLog log = invocation.getArgument(0);
            log.setId(1L);
            return 1;
        }).when(systemOperationLogMapper).insert(any(SystemOperationLog.class));

        operationLogAspect.recordOperationLog(joinPoint, operationLog);

        ArgumentCaptor<SystemOperationLog> captor = ArgumentCaptor.forClass(SystemOperationLog.class);
        verify(systemOperationLogMapper).insert(captor.capture());
        SystemOperationLog log = captor.getValue();
        assertEquals("AI面试题", log.getModule());
        assertEquals("生成AI面试题", log.getOperation());
        assertEquals("AI", log.getOperationType());
        assertEquals(2L, log.getOperatorId());
        assertEquals("wan", log.getOperatorUsername());
    }

    private OperationLog createMockOperationLog(String module, String operation, OperationTypeEnum type) {
        return new OperationLog() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return OperationLog.class;
            }

            @Override
            public String module() {
                return module;
            }

            @Override
            public String operation() {
                return operation;
            }

            @Override
            public OperationTypeEnum type() {
                return type;
            }

            @Override
            public boolean recordParams() {
                return true;
            }
        };
    }

    private void mockLoginUser(Long userId, String username) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, username, "USER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }
}