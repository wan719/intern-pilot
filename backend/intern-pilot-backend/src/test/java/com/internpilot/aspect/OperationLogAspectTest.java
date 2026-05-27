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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
        RequestContextHolder.resetRequestAttributes();
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

    @Test
    void shouldInferUsernameFromArgsWhenSecurityContextMissing() throws Throwable {
        OperationLog operationLog = createMockOperationLog("auth", "login", OperationTypeEnum.LOGIN);
        LoginCommand command = new LoginCommand("guest", "secret");

        when(joinPoint.proceed()).thenReturn("ok");
        when(joinPoint.getArgs()).thenReturn(new Object[]{command});

        Object result = operationLogAspect.recordOperationLog(joinPoint, operationLog);

        assertEquals("ok", result);
        ArgumentCaptor<SystemOperationLog> captor = ArgumentCaptor.forClass(SystemOperationLog.class);
        verify(systemOperationLogMapper).insert(captor.capture());
        assertEquals("guest", captor.getValue().getOperatorUsername());
    }

    @Test
    void shouldCaptureRequestMetadataAndMaskSensitiveParameters() throws Throwable {
        mockLoginUser(9L, "auditor");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/users");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("User-Agent", "JUnit");
        request.addParameter("password", "plain-password");
        request.addParameter("keyword", "normal");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        OperationLog operationLog = createMockOperationLog("admin", "update", OperationTypeEnum.UPDATE);
        LoginCommand command = new LoginCommand("target", "secret-value");
        when(joinPoint.proceed()).thenReturn("done");
        when(joinPoint.getArgs()).thenReturn(new Object[]{command});

        operationLogAspect.recordOperationLog(joinPoint, operationLog);

        ArgumentCaptor<SystemOperationLog> captor = ArgumentCaptor.forClass(SystemOperationLog.class);
        verify(systemOperationLogMapper).insert(captor.capture());
        SystemOperationLog log = captor.getValue();
        assertEquals("/api/admin/users", log.getRequestUri());
        assertEquals("POST", log.getRequestMethod());
        assertEquals("10.0.0.1", log.getIpAddress());
        assertEquals("JUnit", log.getUserAgent());
        assertTrue(log.getRequestParams().contains("[MASKED]"));
        assertTrue(log.getRequestParams().contains("normal"));
        assertFalse(log.getRequestParams().contains("plain-password"));
        assertFalse(log.getRequestParams().contains("secret-value"));
    }

    @Test
    void shouldUseStringPrincipalAndXRealIpWhenAvailable() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("operator", null, List.of())
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/logs");
        request.addHeader("X-Real-IP", "192.168.1.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        OperationLog operationLog = createMockOperationLog("logs", "list", OperationTypeEnum.QUERY, false);
        when(joinPoint.proceed()).thenReturn("ok");
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        operationLogAspect.recordOperationLog(joinPoint, operationLog);

        ArgumentCaptor<SystemOperationLog> captor = ArgumentCaptor.forClass(SystemOperationLog.class);
        verify(systemOperationLogMapper).insert(captor.capture());
        SystemOperationLog log = captor.getValue();
        assertEquals("operator", log.getOperatorUsername());
        assertEquals("192.168.1.10", log.getIpAddress());
        assertEquals("/api/logs", log.getRequestUri());
        assertEquals(null, log.getRequestParams());
    }

    @Test
    void shouldNotBreakBusinessResultWhenPersistingLogFails() throws Throwable {
        mockLoginUser(3L, "admin");
        OperationLog operationLog = createMockOperationLog("admin", "delete", OperationTypeEnum.DELETE);

        when(joinPoint.proceed()).thenReturn("business-result");
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        doThrow(new RuntimeException("insert failed")).when(systemOperationLogMapper).insert(any(SystemOperationLog.class));

        Object result = operationLogAspect.recordOperationLog(joinPoint, operationLog);

        assertEquals("business-result", result);
    }

    private OperationLog createMockOperationLog(String module, String operation, OperationTypeEnum type) {
        return createMockOperationLog(module, operation, type, true);
    }

    private OperationLog createMockOperationLog(String module, String operation, OperationTypeEnum type, boolean recordParams) {
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
                return recordParams;
            }
        };
    }

    private void mockLoginUser(Long userId, String username) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, username, "USER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private static class LoginCommand {
        private final String username;
        private final String password;

        private LoginCommand(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
