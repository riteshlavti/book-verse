package com.bookverse.bookverse_gateway.util;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "1234567890123456789012345678901234567890123456789012345678901234"; // 64-char for HS256
    private String validToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", secret);
        jwtUtil.init();

        // build a valid JWT
        long now = System.currentTimeMillis();
        validToken = Jwts.builder()
                .subject("user123")
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + 1000 * 60 * 60)) // 1 hour expiry
                .signWith(jwtUtil.getSignKey())
                .compact();

        // build an expired JWT
        expiredToken = Jwts.builder()
                .subject("user123")
                .issuedAt(new Date(now - 1000 * 60 * 60)) // issued 1 hour ago
                .expiration(new Date(now - 1000 * 60))    // expired 1 min ago
                .signWith(jwtUtil.getSignKey())
                .compact();
    }

    @Test
    void shouldInitializeSignKey() {
        assertNotNull(jwtUtil.getSignKey());
        assertTrue(jwtUtil.getSignKey() instanceof SecretKey);
    }

    @Test
    void shouldExtractUsername() {
        String username = jwtUtil.extractUsername(validToken);
        assertEquals("user123", username);
    }

    @Test
    void shouldExtractRoles() {
        List<String> roles = jwtUtil.extractRoles(validToken);
        assertEquals(List.of("ROLE_USER"), roles);
    }

    @Test
    void shouldValidateValidToken() {
        assertTrue(jwtUtil.validateToken(validToken));
    }

    @Test
    void shouldInvalidateExpiredToken() {
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void shouldReturnFalseForMalformedToken() {
        assertFalse(jwtUtil.validateToken("malformed.token.value"));
    }

    @Test
    void shouldExtractExpirationDate() {
        Date expiration = jwtUtil.extractExpiration(validToken);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void shouldReturnEmptyRolesForInvalidToken() {
        List<String> roles = jwtUtil.extractRoles("invalid.token");
        assertTrue(roles.isEmpty());
    }
}
