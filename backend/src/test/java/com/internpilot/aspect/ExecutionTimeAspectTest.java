package com.internpilot.aspect;

import com.internpilot.annotation.LogExecutionTime;
import com.internpilot.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecutionTimeAspectTest {

    private final ExecutionTimeAspect aspect = new ExecutionTimeAspect();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logExecutionTimeShouldReturnProceedResultWithRequestAndUserContext() throws Throwable {
        ReflectionTestUtils.setField(aspect, "warnThresholdMs", 10_000L);
        mockLoginUser();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/demo");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Object result = aspect.logExecutionTime(joinPointReturning("ok"), annotation("demo operation"));

        assertEquals("ok", result);
    }

    @Test
    void logExecutionTimeShouldUseSignatureWhenOperationBlankAndRethrowException() throws Throwable {
        ReflectionTestUtils.setField(aspect, "warnThresholdMs", 0L);

        ProceedingJoinPoint joinPoint = mockJoinPoint();
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class,
                () -> aspect.logExecutionTime(joinPoint, annotation("")));
    }

    private ProceedingJoinPoint joinPointReturning(Object value) throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint();
        when(joinPoint.proceed()).thenReturn(value);
        return joinPoint;
    }

    private ProceedingJoinPoint mockJoinPoint() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("DemoController.test()");
        return joinPoint;
    }

    private LogExecutionTime annotation(String value) {
        LogExecutionTime annotation = mock(LogExecutionTime.class);
        when(annotation.value()).thenReturn(value);
        return annotation;
    }

    private void mockLoginUser() {
        CustomUserDetails principal = new CustomUserDetails(9L, "tester", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
