package com.internpilot.captcha;

import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        assertEquals(EmailCaptchaSender.CONFIG_ERROR, exception.getMessage());
        assertFalse(exception.getMessage().contains("secret-mail-password"));
        assertFalse(exception.getMessage().contains("123456"));
    }

    @Test
    void shouldSendEmailWhenSmtpConfigPresent() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        EmailCaptchaSender sender = configuredSender(mailSender);

        sender.send("student@example.com", "654321", CaptchaSceneEnum.EMAIL_REGISTER);

        verify(mailSender).send(message);
    }

    @Test
    void shouldHideDetailsWhenMailSenderFails() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new MailSendException("smtp password leaked?"))
                .when(mailSender).send(message);
        EmailCaptchaSender sender = configuredSender(mailSender);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sender.send("student@example.com", "654321", CaptchaSceneEnum.EMAIL_REGISTER));

        assertFalse(exception.getMessage().contains("654321"));
        assertFalse(exception.getMessage().contains("smtp password"));
    }

    @Test
    void shouldMaskInvalidEmailWithoutCrashingWhenSendFails() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new MailSendException("bad address"))
                .when(mailSender).send(message);
        EmailCaptchaSender sender = configuredSender(mailSender);

        assertThrows(BusinessException.class,
                () -> sender.send("invalid-email", "654321", CaptchaSceneEnum.EMAIL_REGISTER));
    }

    private EmailCaptchaSender configuredSender(JavaMailSender mailSender) {
        EmailCaptchaSender sender = new EmailCaptchaSender(mailSender);
        ReflectionTestUtils.setField(sender, "host", "smtp.example.com");
        ReflectionTestUtils.setField(sender, "username", "mail@example.com");
        ReflectionTestUtils.setField(sender, "password", "secret-mail-password");
        ReflectionTestUtils.setField(sender, "from", "mail@example.com");
        return sender;
    }
}
