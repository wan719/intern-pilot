package com.internpilot.exception;

import com.internpilot.common.Result;
import com.internpilot.common.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessAndAiExceptionsShouldReturnSafeMessages() {
        Result<Void> business = handler.handleBusinessException(new BusinessException("业务错误"));
        Result<Void> ai = handler.handleAiServiceException(new AiServiceException("AI_DOWN", "AI错误"));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), business.getCode());
        assertEquals("业务错误", business.getMessage());
        assertEquals("AI错误", ai.getMessage());
    }

    @Test
    void validationExceptionsShouldJoinFieldAndConstraintMessages() {
        BindException bindException = new BindException(new Object(), "request");
        bindException.addError(new FieldError("request", "name", "名称不能为空"));
        Result<Void> bind = handler.handleBindException(bindException);

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("邮箱格式错误");
        Result<Void> constraint = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of(violation)));

        assertEquals("名称不能为空", bind.getMessage());
        assertEquals("email: 邮箱格式错误", constraint.getMessage());
    }

    @Test
    void badRequestAndAccessDeniedShouldReturnPublicMessages() throws Exception {
        Result<Void> notReadable = handler.handleBadRequest(mock(HttpMessageNotReadableException.class));
        Result<Void> mismatch = handler.handleBadRequest(mock(MethodArgumentTypeMismatchException.class));
        Result<Void> missing = handler.handleBadRequest(
                new MissingServletRequestParameterException("id", "Long"));
        ResponseEntity<Result<Void>> denied = handler.handleAccessDeniedException(new AccessDeniedException("no"));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), notReadable.getCode());
        assertEquals(ResultCode.BAD_REQUEST.getCode(), mismatch.getCode());
        assertEquals(ResultCode.BAD_REQUEST.getCode(), missing.getCode());
        assertEquals(403, denied.getStatusCode().value());
        assertNotNull(denied.getBody());
    }

    @Test
    void databaseAndUnknownExceptionsShouldReturnGenericMessages() {
        Result<Void> duplicate = handler.handleDuplicateKeyException(new DuplicateKeyException("duplicate"));
        Result<Void> integrity = handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("bad"));
        Result<Void> unknown = handler.handleException(new RuntimeException("secret stack"));

        assertEquals(ResultCode.CONFLICT.getCode(), duplicate.getCode());
        assertEquals(ResultCode.BAD_REQUEST.getCode(), integrity.getCode());
        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), unknown.getCode());
    }
}
