package com.internpilot.config;

import com.internpilot.ai.client.MockAiClient;
import com.internpilot.captcha.MockCaptchaSender;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class OnlineProfileGuardTest {

    @Test
    void prodProfileShouldRejectMockAiProvider() {
        OnlineProfileGuard guard = guard("prod", "mock", "test-key", "real", "smtp", "disabled");

        IllegalStateException exception = assertThrows(IllegalStateException.class, guard::validate);

        assertTrue(exception.getMessage().contains("AI_PROVIDER=deepseek"));
    }

    @Test
    void prodProfileShouldRejectMissingDeepSeekKey() {
        OnlineProfileGuard guard = guard("prod", "deepseek", "", "real", "smtp", "disabled");

        IllegalStateException exception = assertThrows(IllegalStateException.class, guard::validate);

        assertTrue(exception.getMessage().contains("DEEPSEEK_API_KEY"));
    }

    @Test
    void prodProfileShouldRejectMockCaptcha() {
        OnlineProfileGuard guard = guard("prod", "deepseek", "test-key", "real", "mock", "disabled");

        IllegalStateException exception = assertThrows(IllegalStateException.class, guard::validate);

        assertTrue(exception.getMessage().contains("mock captcha"));
    }

    @Test
    void prodProfileShouldAcceptDeepSeekAndRealCaptcha() {
        OnlineProfileGuard guard = guard("prod", "deepseek", "test-key", "real", "smtp", "disabled");

        assertDoesNotThrow(guard::validate);
    }

    @Test
    void nonProdProfileShouldAllowMockAiForTests() {
        OnlineProfileGuard guard = guard("test", "mock", "", "mock", "mock", "mock");

        assertDoesNotThrow(guard::validate);
    }

    @Test
    void mockAiClientShouldOnlyBeActiveInTestProfile() {
        Profile profile = MockAiClient.class.getAnnotation(Profile.class);

        assertNotNull(profile);
        assertArrayEquals(new String[]{"test"}, profile.value());
    }

    @Test
    void mockCaptchaSenderShouldOnlyBeActiveInTestProfile() {
        Profile profile = MockCaptchaSender.class.getAnnotation(Profile.class);

        assertNotNull(profile);
        assertArrayEquals(new String[]{"test"}, profile.value());
    }

    private OnlineProfileGuard guard(String profile, String aiProvider, String apiKey,
                                    String captchaMode, String emailProvider, String smsProvider) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);

        AiProperties aiProperties = new AiProperties();
        aiProperties.setProvider(aiProvider);
        aiProperties.setApiKey(apiKey);

        CaptchaProperties captchaProperties = new CaptchaProperties();
        captchaProperties.setMode(captchaMode);
        captchaProperties.setEmailProvider(emailProvider);
        captchaProperties.setSmsProvider(smsProvider);

        return new OnlineProfileGuard(environment, aiProperties, captchaProperties);
    }
}
