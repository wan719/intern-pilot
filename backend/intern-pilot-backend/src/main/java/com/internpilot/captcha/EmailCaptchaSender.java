package com.internpilot.captcha;

import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailCaptchaSender implements CaptchaSender {

    private static final String CONFIG_ERROR = "邮箱验证码服务未配置，请联系管理员";
    private static final String SEND_ERROR = "验证码发送失败，请稍后重试";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${mail.from:${MAIL_FROM:}}")
    private String from;

    @Override
    public void send(String target, String code, CaptchaSceneEnum scene) {
        ensureConfigured();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(target);
            helper.setSubject("InternPilot 注册验证码");
            helper.setText(buildBody(code), false);
            mailSender.send(message);
            log.info("Email captcha sent. scene={} target={}", scene.getCode(), maskEmail(target));
        } catch (MessagingException | MailException e) {
            log.warn("Email captcha send failed. scene={} target={} reason={}",
                    scene.getCode(), maskEmail(target), e.getClass().getSimpleName());
            throw new BusinessException(SEND_ERROR);
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(host)
                || !StringUtils.hasText(username)
                || !StringUtils.hasText(password)
                || !StringUtils.hasText(from)) {
            throw new BusinessException(CONFIG_ERROR);
        }
    }

    private String buildBody(String code) {
        return "您好，您正在注册 InternPilot 账号。\n\n"
                + "本次注册验证码为：" + code + "\n"
                + "验证码 5 分钟内有效，请勿转发或告知他人。\n\n"
                + "如非本人操作，请忽略本邮件。";
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String maskedName = name.length() <= 2
                ? name.charAt(0) + "*"
                : name.substring(0, 2) + "***";
        return maskedName + "@" + parts[1];
    }
}
