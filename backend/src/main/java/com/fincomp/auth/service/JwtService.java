package com.fincomp.auth.service;

import com.fincomp.users.entity.Role;
import com.fincomp.users.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final long refreshExpirationDays;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtService(
            @Value("${fincomp.security.jwt.secret}") String secret,
            @Value("${fincomp.security.jwt.expiration-minutes:60}") long expirationMinutes,
            @Value("${fincomp.security.jwt.refresh-expiration-days:7}") long refreshExpirationDays,
            TokenBlacklistService tokenBlacklistService
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] validKeyBytes = new byte[64]; // 512 bits for HS512 or 256 for HS256
        System.arraycopy(keyBytes, 0, validKeyBytes, 0, Math.min(keyBytes.length, 64));
        this.signingKey = Keys.hmacShaKeyFor(validKeyBytes);
        this.expirationMinutes = expirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshExpirationDays, ChronoUnit.DAYS);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public Role extractRole(String token) {
        String roleStr = extractClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    public long getRemainingMillis(String token) {
        Date expiration = extractClaims(token).getExpiration();
        long diff = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, diff);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            String jti = claims.getId();
            if (tokenBlacklistService.isTokenBlacklisted(jti)) {
                return false;
            }
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
