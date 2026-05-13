package com.internpilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "wan", "USER");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("wan");
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenInvalid() {
        boolean valid = jwtTokenProvider.validateToken("invalid.token.value");

        assertThat(valid).isFalse();
    }
}