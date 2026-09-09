package com.fincomp.auth.service;

import com.fincomp.users.entity.User;
import com.fincomp.users.repository.UserRepository;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MfaVerificationService {

    private static final String CHALLENGE_PREFIX = "mfa_challenge:";
    private static final String REPLAY_PREFIX = "totp_used:";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int MODULUS = 1_000_000;

    private final StringRedisTemplate redisTemplate;
    private final EncryptionService encryptionService;
    private final UserRepository userRepository;

    public MfaVerificationService(
            StringRedisTemplate redisTemplate,
            EncryptionService encryptionService,
            UserRepository userRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
    }

    public String createChallengeToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CHALLENGE_PREFIX + token, userId.toString(), 5, TimeUnit.MINUTES);
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

    public boolean verifyTotpCode(User user, String code) {
        if (code == null || code.isBlank() || user == null) {
            return false;
        }

        String trimmedCode = code.trim();

        // 1. Verifica se é um código de backup (8 caracteres alfanuméricos)
        if (trimmedCode.length() == 8 && user.getMfaBackupCodesHash() != null) {
            return verifyAndConsumeBackupCode(user, trimmedCode);
        }

        // 2. Validação TOTP padrão de 6 dígitos
        if (trimmedCode.length() != DIGITS || user.getMfaSecretEncrypted() == null) {
            return false;
        }

        String plainSecret = encryptionService.decrypt(user.getMfaSecretEncrypted());
        return verifyTotpWithSecret(user.getId(), plainSecret, trimmedCode);
    }

    public boolean verifyTotpWithSecret(UUID userId, String base32Secret, String code) {
        if (base32Secret == null || code == null) return false;

        String replayKey = REPLAY_PREFIX + (userId != null ? userId.toString() : "anon") + ":" + code;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(replayKey))) {
            // Replay detectado: código já utilizado dentro da janela
            return false;
        }

        long currentUnixTime = Instant.now().getEpochSecond();
        long currentStep = currentUnixTime / TIME_STEP_SECONDS;

        // Janela de tolerância: passo atual, anterior (-1) e posterior (+1)
        for (long step = currentStep - 1; step <= currentStep + 1; step++) {
            if (generateCodeForStep(base32Secret, step).equals(code)) {
                // Código válido: registrar no Redis por 60s para prevenir replay
                redisTemplate.opsForValue().set(replayKey, "1", 60, TimeUnit.SECONDS);
                return true;
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
        if (hashes.remove(codeHash)) {
            user.setMfaBackupCodesHash(hashes);
            userRepository.save(user);
            return true;
        }
        return false;
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
            int binary = ((hash[offset] & 0x7F) << 24) |
                    ((hash[offset + 1] & 0xFF) << 16) |
                    ((hash[offset + 2] & 0xFF) << 8) |
                    (hash[offset + 3] & 0xFF);

            int otp = binary % MODULUS;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular código TOTP", e);
        }
    }
}
