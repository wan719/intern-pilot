package com.internpilot.captcha;

import com.internpilot.enums.CaptchaSceneEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "auth.captcha.mode", havingValue = "mock", matchIfMissing = true)
public class MockCaptchaSender implements CaptchaSender {

    @Override
    public void send(String target, String code, CaptchaSceneEnum scene) {
        log.info("[MockCaptcha] scene={} target={} code={}", scene.getCode(), target, code);
    }
}