package com.dianping.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilsTest {
    private static final String SECRET =
            "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXM=";

    @Test
    void generatedTokenRestoresSubjectAndClaims() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofMinutes(30));

        String token = jwtUtils.generateToken("1010", Map.of(
                "nickName", "user_test",
                "icon", "avatar.png"));
        Claims restored = jwtUtils.parseToken(token);

        assertEquals("1010", restored.getSubject());
        assertEquals("user_test", restored.get("nickName", String.class));
        assertEquals("avatar.png", restored.get("icon", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofMinutes(30));
        String token = jwtUtils.generateToken("1010", Map.of());
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThrows(JwtException.class, () -> jwtUtils.parseToken(tampered));
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofMillis(1));
        String token = jwtUtils.generateToken("1010", Map.of());
        Thread.sleep(10);

        assertThrows(JwtException.class, () -> jwtUtils.parseToken(token));
    }
}
