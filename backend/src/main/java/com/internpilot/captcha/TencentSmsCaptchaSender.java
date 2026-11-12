package com.internpilot.captcha;

import com.internpilot.config.CaptchaProperties;
import com.internpilot.config.TencentSmsProperties;
import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TencentSmsCaptchaSender implements CaptchaSender {

    private static final String CONFIG_ERROR = "短信验证码服务未配置，请联系管理员";
    private static final String SEND_ERROR = "验证码发送失败，请稍后重试";

    private final TencentSmsProperties properties;
    private final CaptchaProperties captchaProperties;

    @Override
    public void send(String target, String code, CaptchaSceneEnum scene) {
        ensureConfigured();
        try {
            SmsClient client = createClient();
            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumberSet(new String[]{toChinaPhone(target)});
            request.setSmsSdkAppId(properties.getSdkAppId());
            request.setSignName(properties.getSignName());
            request.setTemplateId(properties.getTemplateId());
            request.setTemplateParamSet(templateParams(code));

            SendSmsResponse response = client.SendSms(request);
            assertSuccess(response, scene, target);
            log.info("Tencent SMS captcha sent. scene={} target={}", scene.getCode(), maskPhone(target));
        } catch (TencentCloudSDKException e) {
            log.warn("Tencent SMS captcha send failed. scene={} target={} code={}",
                    scene.getCode(), maskPhone(target), e.getErrorCode());
            throw new BusinessException(SEND_ERROR);
        }
    }

    private SmsClient createClient() {
        Credential credential = new Credential(properties.getSecretId(), properties.getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("sms.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new SmsClient(credential, properties.getRegion(), clientProfile);
    }

    private void assertSuccess(SendSmsResponse response, CaptchaSceneEnum scene, String target) {
        SendStatus[] statuses = response == null ? null : response.getSendStatusSet();
        if (statuses == null || statuses.length == 0) {
            throw new BusinessException(SEND_ERROR);
        }
        for (SendStatus status : statuses) {
            if (status == null || !"Ok".equalsIgnoreCase(status.getCode())) {
                log.warn("Tencent SMS rejected captcha. scene={} target={} code={}",
                        scene.getCode(), maskPhone(target), status == null ? "NULL" : status.getCode());
                throw new BusinessException(SEND_ERROR);
            }
        }
    }

    private String[] templateParams(String code) {
        if (properties.isTemplateHasExpireMinutes()) {
            return new String[]{code, String.valueOf(Math.max(1, captchaProperties.getTtlSeconds() / 60))};
        }
        return new String[]{code};
    }

    private String toChinaPhone(String phone) {
        String trimmed = phone == null ? "" : phone.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        return "+86" + trimmed;
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(properties.getSecretId())
                || !StringUtils.hasText(properties.getSecretKey())
                || !StringUtils.hasText(properties.getRegion())
                || !StringUtils.hasText(properties.getSdkAppId())
                || !StringUtils.hasText(properties.getSignName())
                || !StringUtils.hasText(properties.getTemplateId())) {
            throw new BusinessException(CONFIG_ERROR);
        }
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "******";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
