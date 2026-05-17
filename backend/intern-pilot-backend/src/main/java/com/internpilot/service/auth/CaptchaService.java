package com.internpilot.service.auth;

import com.internpilot.captcha.CaptchaSender;
import com.internpilot.config.CaptchaProperties;
import com.internpilot.dto.auth.CaptchaSendRequest;
import com.internpilot.entity.User;
import com.internpilot.enums.AccountTypeEnum;
import com.internpilot.enums.CaptchaSceneEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "auth:captcha:";
    private static final String COOLDOWN_PREFIX = "auth:captcha:cooldown:";
    private static final String FAIL_PREFIX = "auth:captcha:fail:";
    private static final String MOCK_CODE = "123456";

    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaSender captchaSender;
    private final CaptchaProperties captchaProperties;
    private final UserMapper userMapper;

    public void sendRegisterCaptcha(CaptchaSendRequest request) {
        String target = request.getTarget().trim();
        String type = request.getType().trim().toUpperCase();

        if (!AccountTypeEnum.PHONE.getCode().equals(type)
                && !AccountTypeEnum.EMAIL.getCode().equals(type)) {
            throw new BusinessException("账号类型无效，仅支持 PHONE 或 EMAIL");
        }

        if ("EMAIL".equals(type)) {
            if (!target.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new BusinessException("邮箱格式不正确");
            }
        } else if ("PHONE".equals(type)) {
            if (!target.matches("^\\d{6,15}$")) {
                throw new BusinessException("手机号格式不正确");
            }
        }

        CaptchaSceneEnum scene;
        if ("EMAIL".equals(type)) {
            scene = CaptchaSceneEnum.EMAIL_REGISTER;
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, target)
                            .eq(User::getDeleted, 0));
            if (emailCount != null && emailCount > 0) {
                throw new BusinessException("该邮箱已被注册");
            }
        } else {
            scene = CaptchaSceneEnum.PHONE_REGISTER;
            Long phoneCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getPhone, target)
                            .eq(User::getDeleted, 0));
            if (phoneCount != null && phoneCount > 0) {
                throw new BusinessException("该手机号已被注册");
            }
        }

        String cooldownKey = COOLDOWN_PREFIX + scene.getCode() + ":" + target;
        Boolean hasCooldown = stringRedisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String code;
        if ("mock".equals(captchaProperties.getMode())) {
            code = MOCK_CODE;
        } else {
            code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        }

        String captchaKey = CAPTCHA_PREFIX + scene.getCode() + ":" + target;
        stringRedisTemplate.opsForValue().set(captchaKey, code, captchaProperties.getTtlSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", captchaProperties.getCooldownSeconds(), TimeUnit.SECONDS);

        String failKey = FAIL_PREFIX + scene.getCode() + ":" + target;
        stringRedisTemplate.delete(failKey);

        captchaSender.send(target, code, scene);
        log.info("验证码已发送: scene={} target={}", scene.getCode(), target);
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
}
