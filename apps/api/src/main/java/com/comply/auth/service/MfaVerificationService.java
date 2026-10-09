package com.comply.auth.service;

import com.comply.users.entity.User;
import com.comply.users.repository.UserRepository;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MfaVerificationService {

  private static final String CHALLENGE_PREFIX = "mfa_challenge:";
  private static final String REPLAY_PREFIX = "totp_used:";
  private static final String ATTEMPT_PREFIX = "mfa_attempts:";
  private static final String BACKUP_CLAIM_PREFIX = "mfa_backup_claim:";
  private static final int TIME_STEP_SECONDS = 30;
  private static final int DIGITS = 6;
  private static final int MODULUS = 1_000_000;
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final int ATTEMPT_WINDOW_MINUTES = 5;
  private static final int CLAIM_TTL_SECONDS = 60;

  private final StringRedisTemplate redisTemplate;
  private final EncryptionService encryptionService;
  private final UserRepository userRepository;

  public MfaVerificationService(
      StringRedisTemplate redisTemplate,
      EncryptionService encryptionService,
      UserRepository userRepository) {
    this.redisTemplate = redisTemplate;
    this.encryptionService = encryptionService;
    this.userRepository = userRepository;
  }

  public String createChallengeToken(UUID userId) {
    String token = UUID.randomUUID().toString();
    redisTemplate
        .opsForValue()
        .set(CHALLENGE_PREFIX + token, userId.toString(), Duration.ofMinutes(5));
    return token;
  }

  public UUID getUserIdFromChallenge(String challengeToken) {
    String userIdStr = redisTemplate.opsForValue().get(CHALLENGE_PREFIX + challengeToken);
    if (userIdStr == null) return null;
    return UUID.fromString(userIdStr);
  }

  public void consumeChallenge(String challengeToken) {
    redisTemplate.delete(CHALLENGE_PREFIX + challengeToken);
  }

  public boolean isAttemptRateLimited(String challengeToken, UUID userId, String ipAddress) {
    return getAttemptCount(challengeToken, userId, ipAddress) >= MAX_FAILED_ATTEMPTS;
  }

  public void registerFailedAttempt(String challengeToken, UUID userId, String ipAddress) {
    incrementAttempt("challenge:" + challengeToken);
    incrementAttempt("user:" + userId);
    incrementAttempt("ip:" + normalizeIp(ipAddress));
  }

  public void resetAttempts(String challengeToken, UUID userId, String ipAddress) {
    redisTemplate.delete(attemptKey("challenge:" + challengeToken));
    redisTemplate.delete(attemptKey("user:" + userId));
    redisTemplate.delete(attemptKey("ip:" + normalizeIp(ipAddress)));
  }

  public boolean verifyTotpCode(User user, String code) {
    if (code == null || code.isBlank() || user == null) {
      return false;
    }

    String trimmedCode = code.trim();

    // Check for an eight-character recovery code.
    if (trimmedCode.length() == 8 && user.getMfaBackupCodesHash() != null) {
      return verifyAndConsumeBackupCode(user, trimmedCode);
    }

    // Otherwise validate the standard six-digit TOTP code.
    if (trimmedCode.length() != DIGITS || user.getMfaSecretEncrypted() == null) {
      return false;
    }

    String plainSecret = encryptionService.decrypt(user.getMfaSecretEncrypted());
    return verifyTotpWithSecret(user.getId(), plainSecret, trimmedCode);
  }

  public boolean verifyTotpWithSecret(UUID userId, String base32Secret, String code) {
    if (base32Secret == null || code == null) return false;

    String replayKey = REPLAY_PREFIX + (userId != null ? userId.toString() : "anon") + ":" + code;
    long currentUnixTime = Instant.now().getEpochSecond();
    long currentStep = currentUnixTime / TIME_STEP_SECONDS;

    // Accept the current time step and one adjacent step in either direction.
    for (long step = currentStep - 1; step <= currentStep + 1; step++) {
      if (generateCodeForStep(base32Secret, step).equals(code)) {
        // SET NX makes replay protection atomic across concurrent requests and instances.
        Boolean claimed =
            redisTemplate
                .opsForValue()
                .setIfAbsent(replayKey, "1", Duration.ofSeconds(CLAIM_TTL_SECONDS));
        return Boolean.TRUE.equals(claimed);
      }
    }

    return false;
  }

  public List<String> generateBackupCodes(User user) {
    List<String> plainCodes = new ArrayList<>();
    List<String> hashedCodes = new ArrayList<>();
    SecureRandom random = new SecureRandom();
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    for (int i = 0; i < 8; i++) {
      StringBuilder sb = new StringBuilder(8);
      for (int j = 0; j < 8; j++) {
        sb.append(chars.charAt(random.nextInt(chars.length())));
      }
      String code = sb.toString();
      plainCodes.add(code);
      hashedCodes.add(DigestUtils.sha256Hex(code));
    }

    user.setMfaBackupCodesHash(hashedCodes);
    userRepository.save(user);

    return plainCodes;
  }

  private boolean verifyAndConsumeBackupCode(User user, String code) {
    String codeHash = DigestUtils.sha256Hex(code.toUpperCase());
    List<String> hashes = new ArrayList<>(user.getMfaBackupCodesHash());
    if (!hashes.contains(codeHash)) {
      return false;
    }

    String claimKey = BACKUP_CLAIM_PREFIX + user.getId() + ":" + codeHash;
    Boolean claimed =
        redisTemplate
            .opsForValue()
            .setIfAbsent(claimKey, "1", Duration.ofSeconds(CLAIM_TTL_SECONDS));
    if (!Boolean.TRUE.equals(claimed)) {
      return false;
    }

    hashes.remove(codeHash);
    user.setMfaBackupCodesHash(hashes);
    userRepository.save(user);
    return true;
  }

  private int getAttemptCount(String challengeToken, UUID userId, String ipAddress) {
    int challengeAttempts = readAttempt("challenge:" + challengeToken);
    int userAttempts = readAttempt("user:" + userId);
    int ipAttempts = readAttempt("ip:" + normalizeIp(ipAddress));
    return Math.max(challengeAttempts, Math.max(userAttempts, ipAttempts));
  }

  private void incrementAttempt(String scope) {
    String key = attemptKey(scope);
    Long attempts = redisTemplate.opsForValue().increment(key);
    if (attempts != null && attempts == 1) {
      redisTemplate.expire(key, Duration.ofMinutes(ATTEMPT_WINDOW_MINUTES));
    }
  }

  private int readAttempt(String scope) {
    String value = redisTemplate.opsForValue().get(attemptKey(scope));
    if (value == null) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return MAX_FAILED_ATTEMPTS;
    }
  }

  private String attemptKey(String scope) {
    return ATTEMPT_PREFIX + scope;
  }

  private String normalizeIp(String ipAddress) {
    return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress;
  }

  private String generateCodeForStep(String base32Secret, long step) {
    try {
      Base32 base32 = new Base32();
      byte[] keyBytes = base32.decode(base32Secret);

      byte[] data = ByteBuffer.allocate(8).putLong(step).array();

      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
      byte[] hash = mac.doFinal(data);

      int offset = hash[hash.length - 1] & 0x0F;
      int binary =
          ((hash[offset] & 0x7F) << 24)
              | ((hash[offset + 1] & 0xFF) << 16)
              | ((hash[offset + 2] & 0xFF) << 8)
              | (hash[offset + 3] & 0xFF);

      int otp = binary % MODULUS;
      return String.format("%06d", otp);
    } catch (Exception e) {
      throw new RuntimeException("Could not calculate the verification code.", e);
    }
  }
}
