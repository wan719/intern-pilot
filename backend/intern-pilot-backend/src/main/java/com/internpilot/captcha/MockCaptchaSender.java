package com.internpilot.captcha;

import com.internpilot.enums.CaptchaSceneEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
public class MockCaptchaSender implements CaptchaSender {

    @Override
    public void send(String target, String code, CaptchaSceneEnum scene) {
        log.info("[MockCaptcha] scene={} target={} code={}", scene.getCode(), target, code);
    }
}
