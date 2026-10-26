package com.comply.auth.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

  private static final String BLACKLIST_PREFIX = "token_blacklist:";
  private static final String LOGIN_ATTEMPTS_PREFIX = "login_attempts:";
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

  private final StringRedisTemplate redisTemplate;

  public TokenBlacklistService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void blacklistToken(String jti, long remainingMillis) {
    if (jti == null || remainingMillis <= 0) return;
    redisTemplate
        .opsForValue()
        .set(BLACKLIST_PREFIX + jti, "revoked", Duration.ofMillis(remainingMillis));
  }

  public boolean isTokenBlacklisted(String jti) {
    if (jti == null) return false;
    return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
  }

  public void registerFailedLogin(String email, String ip) {
    String key = LOGIN_ATTEMPTS_PREFIX + email + ":" + ip;
    Long attempts = redisTemplate.opsForValue().increment(key);
    if (attempts != null && attempts == 1) {
      redisTemplate.expire(key, LOCKOUT_DURATION);
    }
  }

  public boolean isRateLimited(String email, String ip) {
    String key = LOGIN_ATTEMPTS_PREFIX + email + ":" + ip;
    String val = redisTemplate.opsForValue().get(key);
    if (val == null) return false;
    try {
      return Integer.parseInt(val) >= MAX_FAILED_ATTEMPTS;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public void resetFailedLogins(String email, String ip) {
    String key = LOGIN_ATTEMPTS_PREFIX + email + ":" + ip;
    redisTemplate.delete(key);
  }
}
