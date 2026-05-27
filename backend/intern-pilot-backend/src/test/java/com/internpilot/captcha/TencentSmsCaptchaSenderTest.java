package com.internpilot.captcha;

import com.internpilot.config.CaptchaProperties;
import com.internpilot.config.TencentSmsProperties;
import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void privateHelpersShouldFormatPhoneTemplateAndMaskSafely() {
        TencentSmsProperties properties = configuredProperties();
        CaptchaProperties captchaProperties = new CaptchaProperties();
        captchaProperties.setTtlSeconds(90);
        TencentSmsCaptchaSender sender = new TencentSmsCaptchaSender(properties, captchaProperties);

        properties.setTemplateHasExpireMinutes(false);
        String[] oneParam = ReflectionTestUtils.invokeMethod(sender, "templateParams", "123456");
        assertArrayEquals(new String[]{"123456"}, oneParam);

        properties.setTemplateHasExpireMinutes(true);
        String[] twoParams = ReflectionTestUtils.invokeMethod(sender, "templateParams", "123456");
        assertArrayEquals(new String[]{"123456", "1"}, twoParams);

        captchaProperties.setTtlSeconds(180);
        String[] threeMinutes = ReflectionTestUtils.invokeMethod(sender, "templateParams", "123456");
        assertArrayEquals(new String[]{"123456", "3"}, threeMinutes);

        assertEquals("+8613800000000", ReflectionTestUtils.invokeMethod(sender, "toChinaPhone", " 13800000000 "));
        assertEquals("+85212345678", ReflectionTestUtils.invokeMethod(sender, "toChinaPhone", "+85212345678"));
        assertEquals("+86", ReflectionTestUtils.invokeMethod(sender, "toChinaPhone", (String) null));
        assertEquals("138****0000", ReflectionTestUtils.invokeMethod(sender, "maskPhone", "13800000000"));
        assertEquals("******", ReflectionTestUtils.invokeMethod(sender, "maskPhone", "123"));
        assertEquals("******", ReflectionTestUtils.invokeMethod(sender, "maskPhone", (String) null));
    }

    @Test
    void assertSuccessShouldAcceptOkAndRejectEmptyOrFailedStatuses() {
        TencentSmsCaptchaSender sender = new TencentSmsCaptchaSender(configuredProperties(), new CaptchaProperties());

        SendStatus ok = new SendStatus();
        ok.setCode("Ok");
        SendSmsResponse okResponse = new SendSmsResponse();
        okResponse.setSendStatusSet(new SendStatus[]{ok});
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                sender, "assertSuccess", okResponse, CaptchaSceneEnum.PHONE_REGISTER, "13800000000"));

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                sender, "assertSuccess", null, CaptchaSceneEnum.PHONE_REGISTER, "13800000000"));

        SendSmsResponse empty = new SendSmsResponse();
        empty.setSendStatusSet(new SendStatus[0]);
        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                sender, "assertSuccess", empty, CaptchaSceneEnum.PHONE_REGISTER, "13800000000"));

        SendSmsResponse nullStatus = new SendSmsResponse();
        nullStatus.setSendStatusSet(new SendStatus[]{null});
        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                sender, "assertSuccess", nullStatus, CaptchaSceneEnum.PHONE_REGISTER, "13800000000"));

        SendStatus failed = new SendStatus();
        failed.setCode("FailedOperation");
        SendSmsResponse failedResponse = new SendSmsResponse();
        failedResponse.setSendStatusSet(new SendStatus[]{failed});
        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                sender, "assertSuccess", failedResponse, CaptchaSceneEnum.PHONE_REGISTER, "13800000000"));
    }

    @Test
    void ensureConfiguredShouldCheckEveryRequiredTencentField() {
        TencentSmsProperties properties = configuredProperties();
        TencentSmsCaptchaSender configuredSender = new TencentSmsCaptchaSender(properties, new CaptchaProperties());
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(configuredSender, "ensureConfigured"));

        properties.setSecretId("");
        assertMisconfigured(properties);
        properties = configuredProperties();
        properties.setSecretKey(" ");
        assertMisconfigured(properties);
        properties = configuredProperties();
        properties.setRegion(null);
        assertMisconfigured(properties);
        properties = configuredProperties();
        properties.setSdkAppId("");
        assertMisconfigured(properties);
        properties = configuredProperties();
        properties.setSignName("");
        assertMisconfigured(properties);
        properties = configuredProperties();
        properties.setTemplateId("");
        assertMisconfigured(properties);
    }

    private void assertMisconfigured(TencentSmsProperties properties) {
        TencentSmsCaptchaSender sender = new TencentSmsCaptchaSender(properties, new CaptchaProperties());
        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(sender, "ensureConfigured"));
    }

    private TencentSmsProperties configuredProperties() {
        TencentSmsProperties properties = new TencentSmsProperties();
        properties.setSecretId("secret-id");
        properties.setSecretKey("secret-key");
        properties.setRegion("ap-guangzhou");
        properties.setSdkAppId("app-id");
        properties.setSignName("sign");
        properties.setTemplateId("template");
        return properties;
    }
}
