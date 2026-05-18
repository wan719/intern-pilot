package com.internpilot.config;

import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnlineProfileGuard implements ApplicationRunner {

    private final Environment environment;
    private final AiProperties aiProperties;
    private final CaptchaProperties captchaProperties;

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    public void validate() {
        if (!isProdProfile()) {
            return;
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            throw new IllegalStateException("prod profile requires AI_PROVIDER=deepseek; mock AI is not allowed online.");
        }
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("prod profile requires DEEPSEEK_API_KEY.");
        }
        if ("mock".equalsIgnoreCase(captchaProperties.getEmailProvider())
                || "mock".equalsIgnoreCase(captchaProperties.getSmsProvider())
                || "mock".equalsIgnoreCase(captchaProperties.getMode())) {
            throw new IllegalStateException("prod profile does not allow mock captcha providers.");
        }
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }
}
