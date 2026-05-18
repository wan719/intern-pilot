package com.internpilot.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.captcha.EmailCaptchaSender;
import com.internpilot.captcha.MockCaptchaSender;
import com.internpilot.config.CaptchaProperties;
import com.internpilot.dto.auth.CaptchaSendRequest;
import com.internpilot.entity.User;
import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "auth:captcha:";
    private static final String COOLDOWN_PREFIX = "auth:captcha:cooldown:";
    private static final String FAIL_PREFIX = "auth:captcha:fail:";
    private static final String DAILY_PREFIX = "auth:captcha:daily:";
    private static final String MOCK_CODE = "123456";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectProvider<MockCaptchaSender> mockCaptchaSenderProvider;
    private final EmailCaptchaSender emailCaptchaSender;
    private final CaptchaProperties captchaProperties;
    private final UserMapper userMapper;

    public void sendRegisterCaptcha(CaptchaSendRequest request) {
        String target = request.getTarget().trim();
        String type = request.getType().trim().toUpperCase();

        if (!"EMAIL".equals(type)) {
            throw new BusinessException("当前阶段仅支持邮箱注册验证码");
        }
        validateEmail(target);
        ensureEmailNotRegistered(target);

        String provider = captchaProperties.getEmailProvider();
        if ("disabled".equalsIgnoreCase(provider)) {
            throw new BusinessException("邮箱注册暂未开放");
        }

        String dailyKey = ensureCaptchaPolicy(CaptchaSceneEnum.EMAIL_REGISTER, target);
        String code = "mock".equalsIgnoreCase(provider) ? MOCK_CODE : generateCode();
        try {
            sendByProvider(provider, target, code);
        } catch (RuntimeException e) {
            rollbackDailyCount(dailyKey);
            throw e;
        }
        saveCaptcha(CaptchaSceneEnum.EMAIL_REGISTER, target, code);

        log.info("Captcha sent. scene={} type=EMAIL target={}",
                CaptchaSceneEnum.EMAIL_REGISTER.getCode(), maskEmail(target));
    }

    public void validateCaptcha(String target, CaptchaSceneEnum scene, String inputCode) {
        String captchaKey = CAPTCHA_PREFIX + scene.getCode() + ":" + target;
        String storedCode = stringRedisTemplate.opsForValue().get(captchaKey);

        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新发送");
        }

        String failKey = FAIL_PREFIX + scene.getCode() + ":" + target;
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr == null ? 0 : Integer.parseInt(failCountStr);

        if (failCount >= captchaProperties.getMaxFailCount()) {
            stringRedisTemplate.delete(captchaKey);
            stringRedisTemplate.delete(failKey);
            throw new BusinessException("验证码错误次数过多，请重新发送");
        }

        if (!storedCode.equals(inputCode)) {
            long ttl = stringRedisTemplate.getExpire(captchaKey, TimeUnit.SECONDS);
            if (ttl <= 0) {
                ttl = captchaProperties.getTtlSeconds();
            }
            stringRedisTemplate.opsForValue().set(failKey, String.valueOf(failCount + 1), ttl, TimeUnit.SECONDS);
            throw new BusinessException("验证码错误");
        }

        stringRedisTemplate.delete(captchaKey);
        stringRedisTemplate.delete(failKey);
    }

    private void validateEmail(String target) {
        if (!target.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException("邮箱格式不正确");
        }
    }

    private void ensureEmailNotRegistered(String target) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, target)
                        .eq(User::getDeleted, 0));
        if (count != null && count > 0) {
            throw new BusinessException("该邮箱已被注册");
        }
    }

    private String ensureCaptchaPolicy(CaptchaSceneEnum scene, String target) {
        String cooldownKey = COOLDOWN_PREFIX + scene.getCode() + ":" + target;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String dailyKey = DAILY_PREFIX + scene.getCode() + ":" + target;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1L) {
            stringRedisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
        }
        if (dailyCount != null && dailyCount > captchaProperties.getDailyLimit()) {
            throw new BusinessException("今日验证码发送次数已达上限");
        }
        return dailyKey;
    }

    private void rollbackDailyCount(String dailyKey) {
        try {
            stringRedisTemplate.opsForValue().decrement(dailyKey);
        } catch (RuntimeException e) {
            log.warn("Failed to rollback captcha daily count. key={}", dailyKey);
        }
    }

    private void sendByProvider(String provider, String target, String code) {
        if ("mock".equalsIgnoreCase(provider)) {
            MockCaptchaSender mockSender = mockCaptchaSenderProvider.getIfAvailable();
            if (mockSender == null) {
                throw new BusinessException("验证码服务未配置，请联系管理员");
            }
            mockSender.send(target, code, CaptchaSceneEnum.EMAIL_REGISTER);
            return;
        }

        if ("smtp".equalsIgnoreCase(provider)) {
            emailCaptchaSender.send(target, code, CaptchaSceneEnum.EMAIL_REGISTER);
            return;
        }

        throw new BusinessException("验证码服务未配置，请联系管理员");
    }

    private void saveCaptcha(CaptchaSceneEnum scene, String target, String code) {
        String captchaKey = CAPTCHA_PREFIX + scene.getCode() + ":" + target;
        String cooldownKey = COOLDOWN_PREFIX + scene.getCode() + ":" + target;
        String failKey = FAIL_PREFIX + scene.getCode() + ":" + target;
        stringRedisTemplate.opsForValue().set(captchaKey, code, captchaProperties.getTtlSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", captchaProperties.getCooldownSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.delete(failKey);
    }

    private String generateCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private String maskEmail(String target) {
        int at = target.indexOf('@');
        if (at <= 1) {
            return "***" + target.substring(Math.max(0, at));
        }
        return target.substring(0, Math.min(2, at)) + "***" + target.substring(at);
    }
}
