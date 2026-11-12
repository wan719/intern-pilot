package com.internpilot.auth.captcha;

import com.internpilot.auth.enums.CaptchaSceneEnum;

public interface CaptchaSender {

    void send(String target, String code, CaptchaSceneEnum scene);
}