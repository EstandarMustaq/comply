package com.complyz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionConfigurationValidator {

    private static final String DEVELOPMENT_JWT_PREFIX = "404E635266556A586E3272";
    private static final String DEVELOPMENT_MFA_VALUE = "0123456789abcdef";

    public ProductionConfigurationValidator(
            @Value("${JWT_SECRET}") String jwtSecret,
            @Value("${MFA_ENCRYPTION_KEY:${MFA_AES_KEY}}") String mfaEncryptionKey,
            @Value("${DB_PASSWORD:${POSTGRES_PASSWORD}}") String databasePassword,
            @Value("${REDIS_PASSWORD}") String redisPassword,
            @Value("${complyz.bootstrap.enabled:false}") boolean bootstrapEnabled) {
        validate(jwtSecret, mfaEncryptionKey, databasePassword, redisPassword, bootstrapEnabled);
    }

    static void validate(
            String jwtSecret,
            String mfaEncryptionKey,
            String databasePassword,
            String redisPassword,
            boolean bootstrapEnabled) {
        requireSecret("JWT_SECRET", jwtSecret, DEVELOPMENT_JWT_PREFIX);
        requireSecret("MFA_ENCRYPTION_KEY", mfaEncryptionKey, DEVELOPMENT_MFA_VALUE);
        requirePassword("POSTGRES_PASSWORD", databasePassword, "complyz_dev_password");
        requirePassword("REDIS_PASSWORD", redisPassword, "redis_dev_password");
        if (bootstrapEnabled) {
            throw new IllegalStateException(
                    "COMPLYZ_BOOTSTRAP_ENABLED must be false when the production profile is active.");
        }
    }

    private static void requireSecret(String name, String value, String developmentValue) {
        if (value == null || value.length() < 32 || value.contains(developmentValue)) {
            throw new IllegalStateException(
                    name + " must be at least 32 characters and must not use its development default.");
        }
    }

    private static void requirePassword(String name, String value, String developmentValue) {
        if (value == null || value.length() < 12 || value.equals(developmentValue)) {
            throw new IllegalStateException(
                    name + " must be at least 12 characters and must not use its development default.");
        }
    }
}
