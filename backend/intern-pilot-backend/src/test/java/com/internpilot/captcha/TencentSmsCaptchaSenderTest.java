package com.internpilot.captcha;

import com.internpilot.config.CaptchaProperties;
import com.internpilot.config.TencentSmsProperties;
import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TencentSmsCaptchaSenderTest {

    @Test
    void shouldFailClearlyWhenTencentConfigMissing() {
        TencentSmsProperties properties = new TencentSmsProperties();
        properties.setSecretKey("secret-sms-key");
        CaptchaProperties captchaProperties = new CaptchaProperties();
        TencentSmsCaptchaSender sender = new TencentSmsCaptchaSender(properties, captchaProperties);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sender.send("13800000000", "123456", CaptchaSceneEnum.PHONE_REGISTER));

        assertEquals("短信验证码服务未配置，请联系管理员", exception.getMessage());
        assertFalse(exception.getMessage().contains("secret-sms-key"));
        assertFalse(exception.getMessage().contains("123456"));
    }
}
