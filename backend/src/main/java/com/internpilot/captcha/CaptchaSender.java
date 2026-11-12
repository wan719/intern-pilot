package com.internpilot.captcha;

import com.internpilot.enums.CaptchaSceneEnum;

public interface CaptchaSender {

    void send(String target, String code, CaptchaSceneEnum scene);
}