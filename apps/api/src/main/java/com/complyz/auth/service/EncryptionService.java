package com.complyz.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private final SecretKey secretKey;

    public EncryptionService(@Value("${complyz.security.mfa.encryption-key}") String configuredKey) {
        this.secretKey = new SecretKeySpec(resolveKeyBytes(configuredKey), "AES");
    }

    private static byte[] resolveKeyBytes(String configuredKey) {
        if (configuredKey.matches("[A-Za-z0-9+/]{43}=")) {
            byte[] decodedKey = Base64.getDecoder().decode(configuredKey);
            if (
                    decodedKey.length != 32
                            || !Base64.getEncoder().encodeToString(decodedKey).equals(configuredKey)
            ) {
                throw new IllegalArgumentException("MFA encryption key must be canonical Base64 for 32 bytes.");
            }
            return decodedKey;
        }

        byte[] legacyKey = configuredKey.getBytes(StandardCharsets.UTF_8);
        byte[] compatibleKey = new byte[32];
        System.arraycopy(legacyKey, 0, compatibleKey, 0, Math.min(legacyKey.length, compatibleKey.length));
        return compatibleKey;
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cifrar dados confidenciais", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao decifrar dados confidenciais", e);
        }
    }
}
