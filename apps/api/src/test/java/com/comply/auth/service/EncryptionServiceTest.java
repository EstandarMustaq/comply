package com.comply.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

  @Test
  void roundTripsWithBase64EncodedAes256Key() {
    byte[] keyBytes = new byte[32];
    String configuredKey = Base64.getEncoder().encodeToString(keyBytes);
    EncryptionService service = new EncryptionService(configuredKey);

    String encrypted = service.encrypt("confidential value");

    assertNotEquals("confidential value", encrypted);
    assertEquals("confidential value", service.decrypt(encrypted));
  }

  @Test
  void continuesToReadExistingStringKeys() {
    EncryptionService service = new EncryptionService("0123456789abcdef0123456789abcdef");

    String encrypted = service.encrypt("confidential value");

    assertNotEquals("confidential value", encrypted);
    assertEquals("confidential value", service.decrypt(encrypted));
  }
}
