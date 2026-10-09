package com.comply.auth.service;

import com.comply.auth.dto.MfaEnrollResponse;
import com.comply.users.entity.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.apache.commons.codec.binary.Base32;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MfaEnrollmentService {

  private static final String ENROLLMENT_PREFIX = "mfa_enrollment:";
  private static final String ENROLLMENT_TOKEN_PREFIX = "mfa_enrollment_token:";
  private static final int QR_CODE_SIZE = 250;

  private final StringRedisTemplate redisTemplate;

  public MfaEnrollmentService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public String createEnrollmentToken(UUID userId) {
    String token = UUID.randomUUID().toString();
    redisTemplate
        .opsForValue()
        .set(ENROLLMENT_TOKEN_PREFIX + token, userId.toString(), Duration.ofMinutes(15));
    return token;
  }

  public UUID getUserIdFromEnrollmentToken(String token) {
    if (token == null || token.isBlank()) return null;
    String val = redisTemplate.opsForValue().get(ENROLLMENT_TOKEN_PREFIX + token);
    return val != null ? UUID.fromString(val) : null;
  }

  public void consumeEnrollmentToken(String token) {
    if (token != null) {
      redisTemplate.delete(ENROLLMENT_TOKEN_PREFIX + token);
    }
  }

  public MfaEnrollResponse startEnrollment(User user) {
    // Generate a 20-byte (160-bit) secret and encode it as Base32.
    byte[] buffer = new byte[20];
    new SecureRandom().nextBytes(buffer);
    Base32 base32 = new Base32();
    String secretKey = base32.encodeToString(buffer).replace("=", "");

    // Store the pending secret in Redis for ten minutes.
    String redisKey = ENROLLMENT_PREFIX + user.getId().toString();
    redisTemplate.opsForValue().set(redisKey, secretKey, Duration.ofMinutes(10));

    // Build an otpauth URL for authenticator applications.
    String encodedIssuer = URLEncoder.encode("Comply", StandardCharsets.UTF_8);
    String encodedAccount = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
    String otpauthUrl =
        String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
            encodedIssuer, encodedAccount, secretKey, encodedIssuer);

    // Encode the enrollment QR code as Base64.
    String qrCodeBase64 = generateQrCodeBase64(otpauthUrl);

    return new MfaEnrollResponse(secretKey, qrCodeBase64, otpauthUrl);
  }

  public String getPendingEnrollmentSecret(User user) {
    String redisKey = ENROLLMENT_PREFIX + user.getId().toString();
    return redisTemplate.opsForValue().get(redisKey);
  }

  public void clearPendingEnrollment(User user) {
    String redisKey = ENROLLMENT_PREFIX + user.getId().toString();
    redisTemplate.delete(redisKey);
  }

  private String generateQrCodeBase64(String content) {
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix =
          qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
      return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException("Could not generate the MFA QR code.", e);
    }
  }
}
