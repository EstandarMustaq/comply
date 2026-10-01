package com.fincomp.auth.service;

import com.fincomp.users.entity.Role;
import com.fincomp.users.entity.User;
import com.fincomp.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaVerificationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private UserRepository userRepository;

    private MfaVerificationService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new MfaVerificationService(redisTemplate, encryptionService, userRepository);
    }

    @Test
    void limitsAttemptsByChallengeUserOrIp() {
        UUID userId = UUID.randomUUID();
        when(valueOperations.get("mfa_attempts:challenge:challenge-token")).thenReturn("5");
        when(valueOperations.get("mfa_attempts:user:" + userId)).thenReturn("0");
        when(valueOperations.get("mfa_attempts:ip:127.0.0.1")).thenReturn("0");

        assertTrue(service.isAttemptRateLimited("challenge-token", userId, "127.0.0.1"));
    }

    @Test
    void failedAttemptCountersUseACommonFiveMinuteWindow() {
        UUID userId = UUID.randomUUID();

        when(valueOperations.increment(anyString())).thenReturn(1L);

        service.registerFailedAttempt("challenge-token", userId, "127.0.0.1");

        verify(valueOperations, times(3)).increment(anyString());
        verify(redisTemplate, times(3)).expire(anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void validTotpIsRejectedWhenReplayClaimCannotBeAcquired() throws Exception {
        UUID userId = UUID.randomUUID();
        String secret = "JBSWY3DPEHPK3PXP";
        String code = currentTotp(secret);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        assertFalse(service.verifyTotpWithSecret(userId, secret, code));
    }

    private String currentTotp(String base32Secret) throws Exception {
        org.apache.commons.codec.binary.Base32 base32 = new org.apache.commons.codec.binary.Base32();
        byte[] keyBytes = base32.decode(base32Secret);
        long step = Instant.now().getEpochSecond() / 30;
        byte[] data = java.nio.ByteBuffer.allocate(8).putLong(step).array();
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        return String.format("%06d", binary % 1_000_000);
    }
}
