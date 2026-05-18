package com.internpilot.captcha;

import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EmailCaptchaSenderTest {

    @Test
    void shouldFailClearlyWhenSmtpConfigMissing() {
        EmailCaptchaSender sender = new EmailCaptchaSender(mock(JavaMailSender.class));
        ReflectionTestUtils.setField(sender, "host", "");
        ReflectionTestUtils.setField(sender, "username", "");
        ReflectionTestUtils.setField(sender, "password", "secret-mail-password");
        ReflectionTestUtils.setField(sender, "from", "");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sender.send("test@example.com", "123456", CaptchaSceneEnum.EMAIL_REGISTER));

        assertEquals("邮箱验证码服务未配置，请联系管理员", exception.getMessage());
        assertFalse(exception.getMessage().contains("secret-mail-password"));
        assertFalse(exception.getMessage().contains("123456"));
    }
}
