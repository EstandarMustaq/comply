package com.fincomp.auth.service;

import com.fincomp.users.entity.Role;
import com.fincomp.users.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789012345678901234567890123";

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60, 7, tokenBlacklistService);
        user = new User("Teste", "teste@example.test", "hash", Role.OCOS);
        user.setId(UUID.randomUUID());
        when(tokenBlacklistService.isTokenBlacklisted(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
    }

    @Test
    void accessAndRefreshTokensHaveDistinctTypes() {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        assertTrue(jwtService.isAccessTokenValid(accessToken));
        assertFalse(jwtService.isRefreshTokenValid(accessToken));
        assertTrue(jwtService.isRefreshTokenValid(refreshToken));
        assertFalse(jwtService.isAccessTokenValid(refreshToken));
        assertEquals(JwtService.ACCESS_TOKEN_TYPE, jwtService.extractClaims(accessToken).get("tokenType"));
        assertEquals(JwtService.REFRESH_TOKEN_TYPE, jwtService.extractClaims(refreshToken).get("tokenType"));
    }

    @Test
    void tokensAreRejectedWhenBlacklisted() {
        String refreshToken = jwtService.generateRefreshToken(user);
        Claims claims = jwtService.extractClaims(refreshToken);
        when(tokenBlacklistService.isTokenBlacklisted(claims.getId())).thenReturn(true);

        assertFalse(jwtService.isRefreshTokenValid(refreshToken));
    }
}
