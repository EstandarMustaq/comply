package com.comply.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionConfigurationValidatorTest {

  @Test
  void acceptsProductionSecretsAndDisabledBootstrap() {
    assertDoesNotThrow(
        () ->
            ProductionConfigurationValidator.validate(
                "a".repeat(64),
                "b".repeat(64),
                "strong-postgres-password-value",
                "strong-redis-password-value",
                false));
  }

  @Test
  void rejectsDevelopmentSecretsAndEnabledBootstrap() {
    assertThrows(
        IllegalStateException.class,
        () ->
            ProductionConfigurationValidator.validate(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                "b".repeat(64),
                "strong-postgres-password-value",
                "strong-redis-password-value",
                false));
    assertThrows(
        IllegalStateException.class,
        () ->
            ProductionConfigurationValidator.validate(
                "a".repeat(64),
                "0123456789abcdef0123456789abcdef",
                "strong-postgres-password-value",
                "strong-redis-password-value",
                false));
    assertThrows(
        IllegalStateException.class,
        () ->
            ProductionConfigurationValidator.validate(
                "a".repeat(64),
                "b".repeat(64),
                "comply_dev_password",
                "strong-redis-password-value",
                false));
    assertThrows(
        IllegalStateException.class,
        () ->
            ProductionConfigurationValidator.validate(
                "a".repeat(64),
                "b".repeat(64),
                "strong-postgres-password-value",
                "redis_dev_password",
                false));
    assertThrows(
        IllegalStateException.class,
        () ->
            ProductionConfigurationValidator.validate(
                "a".repeat(64),
                "b".repeat(64),
                "strong-postgres-password-value",
                "strong-redis-password-value",
                true));
  }
}
