package com.internpilot.service.auth.impl;

import com.internpilot.captcha.EmailCaptchaSender;
import com.internpilot.captcha.MockCaptchaSender;
import com.internpilot.config.CaptchaProperties;
import com.internpilot.constant.RedisKeyConstants;
import com.internpilot.dto.auth.CaptchaSendRequest;
import com.internpilot.dto.auth.LoginRequest;
import com.internpilot.dto.auth.RegisterRequest;
import com.internpilot.entity.Role;
import com.internpilot.entity.User;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.PermissionMapper;
import com.internpilot.mapper.RoleMapper;
import com.internpilot.mapper.UserMapper;
import com.internpilot.mapper.UserRoleMapper;
import com.internpilot.security.JwtTokenProvider;
import com.internpilot.service.auth.CaptchaService;
import com.internpilot.vo.auth.AuthUserResponse;
import com.internpilot.vo.auth.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EmailCaptchaSender emailCaptchaSender;

    private MockCaptchaSender mockCaptchaSender;
    private CaptchaProperties captchaProperties;
    private CaptchaService captchaService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        captchaProperties = new CaptchaProperties();
        captchaProperties.setEmailProvider("mock");
        captchaProperties.setSmsProvider("disabled");
        captchaProperties.setTtlSeconds(300);
        captchaProperties.setCooldownSeconds(60);
        captchaProperties.setMaxFailCount(5);
        captchaProperties.setDailyLimit(10);

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.increment(anyString())).thenReturn(1L);

        mockCaptchaSender = spy(new MockCaptchaSender());
        captchaService = new CaptchaService(stringRedisTemplate, objectProvider(mockCaptchaSender),
                emailCaptchaSender, captchaProperties, userMapper);
        authService = new AuthServiceImpl(userMapper, passwordEncoder, jwtTokenProvider,
                roleMapper, permissionMapper, userRoleMapper, captchaService);
    }

    @Test
    void sendRegisterCaptcha_shouldSucceed_forEmail() {
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setTarget("test@example.com");
        request.setType("EMAIL");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        captchaService.sendRegisterCaptcha(request);

        verify(valueOperations).set(eq(RedisKeyConstants.captcha("EMAIL_REGISTER", "test@example.com")), eq("123456"), eq(300L),
                eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq(RedisKeyConstants.captchaCooldown("EMAIL_REGISTER", "test@example.com")), eq("1"), eq(60L),
                eq(TimeUnit.SECONDS));
        verify(mockCaptchaSender).send(eq("test@example.com"), eq("123456"), any());
    }

    @Test
    void sendRegisterCaptcha_shouldFail_forPhone() {
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setTarget("13800000000");
        request.setType("PHONE");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> captchaService.sendRegisterCaptcha(request));

        assertEquals("当前阶段仅支持邮箱注册验证码", exception.getMessage());
        verify(valueOperations, never()).set(startsWith("auth:captcha:PHONE_REGISTER"), anyString(), anyLong(), any());
    }

    @Test
    void sendRegisterCaptcha_shouldFail_whenEmailProviderDisabled() {
        captchaProperties.setEmailProvider("disabled");
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setTarget("test@example.com");
        request.setType("EMAIL");

        when(userMapper.selectCount(any())).thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> captchaService.sendRegisterCaptcha(request));

        assertEquals("邮箱注册暂未开放", exception.getMessage());
        verify(valueOperations, never()).set(startsWith("auth:captcha:EMAIL_REGISTER"), anyString(), anyLong(), any());
    }

    @Test
    void sendRegisterCaptcha_shouldNotSaveCaptcha_whenProviderSendFails() {
        captchaProperties.setEmailProvider("smtp");
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setTarget("test@example.com");
        request.setType("EMAIL");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        doThrow(new BusinessException("验证码发送失败，请稍后重试"))
                .when(emailCaptchaSender).send(eq("test@example.com"), anyString(), any());

        assertThrows(BusinessException.class, () -> captchaService.sendRegisterCaptcha(request));

        verify(valueOperations, never()).set(startsWith("auth:captcha:EMAIL_REGISTER"), anyString(), anyLong(), any());
    }

    @Test
    void registerWithCaptcha_shouldFail_whenWrongCaptcha() {
        RegisterRequest request = emailRegisterRequest("test@example.com", "000000");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(valueOperations.get(RedisKeyConstants.captcha("EMAIL_REGISTER", "test@example.com"))).thenReturn("123456");
        when(valueOperations.get(RedisKeyConstants.captchaFail("EMAIL_REGISTER", "test@example.com"))).thenReturn(null);
        when(stringRedisTemplate.getExpire(anyString(), any())).thenReturn(300L);

        assertThrows(BusinessException.class, () -> authService.register(request));
    }

    @Test
    void register_shouldSucceed_forEmail() {
        RegisterRequest request = emailRegisterRequest("test@example.com", "123456");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(valueOperations.get(RedisKeyConstants.captcha("EMAIL_REGISTER", "test@example.com"))).thenReturn("123456");
        when(valueOperations.get(RedisKeyConstants.captchaFail("EMAIL_REGISTER", "test@example.com"))).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_password");
        when(roleMapper.selectOne(any())).thenReturn(mockRole());
        when(permissionMapper.selectRoleCodesByUserId(anyLong())).thenReturn(List.of("USER"));
        when(permissionMapper.selectPermissionCodesByUserId(anyLong())).thenReturn(Collections.emptyList());
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return 1;
        });

        AuthUserResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.getUsername().startsWith("test_"));
        assertEquals("test@example.com", response.getEmail());
        verify(userMapper).insert(any(User.class));
        verify(userRoleMapper).insert(any(com.internpilot.entity.UserRole.class));
    }

    @Test
    void register_shouldFail_whenDuplicateEmail() {
        RegisterRequest request = emailRegisterRequest("test@example.com", "123456");

        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> authService.register(request));
    }

    @Test
    void register_shouldFail_forPhone() {
        RegisterRequest request = emailRegisterRequest("13800000000", "123456");
        request.setAccountType("PHONE");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals("当前阶段仅支持邮箱注册", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void login_shouldSucceed_withEmail() {
        LoginRequest request = new LoginRequest();
        request.setAccount("test@example.com");
        request.setPassword("123456");

        User mockUser = mockUser(1L, "test", "test@example.com", "USER");
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches("123456", "encoded_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "test", "USER")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(86400L);
        when(permissionMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("USER"));
        when(permissionMapper.selectPermissionCodesByUserId(1L)).thenReturn(Collections.emptyList());

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    void login_shouldFail_withUsernameForNormalUser() {
        LoginRequest request = new LoginRequest();
        request.setAccount("demo");
        request.setPassword("123456");

        assertThrows(BusinessException.class, () -> authService.login(request));
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void login_shouldFail_whenWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setAccount("test@example.com");
        request.setPassword("wrong_password");

        User mockUser = mockUser(1L, "test", "test@example.com", "USER");
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    private RegisterRequest emailRegisterRequest(String account, String captchaCode) {
        RegisterRequest request = new RegisterRequest();
        request.setAccount(account);
        request.setAccountType("EMAIL");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        request.setCaptchaCode(captchaCode);
        return request;
    }

    private Role mockRole() {
        Role role = new Role();
        role.setId(1L);
        role.setRoleCode("USER");
        role.setRoleName("普通用户");
        role.setEnabled(1);
        return role;
    }

    private User mockUser(Long id, String username, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encoded_password");
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(1);
        return user;
    }

    private <T> ObjectProvider<T> objectProvider(T instance) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return instance;
            }

            @Override
            public T getIfAvailable() {
                return instance;
            }

            @Override
            public T getIfUnique() {
                return instance;
            }

            @Override
            public T getObject() {
                return instance;
            }
        };
    }
}
