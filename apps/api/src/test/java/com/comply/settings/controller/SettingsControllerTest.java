package com.comply.settings.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.comply.audit.service.AuditService;
import com.comply.monitoring.repository.ThresholdRuleRepository;
import com.comply.settings.entity.SystemConfig;
import com.comply.settings.repository.SystemConfigRepository;
import com.comply.users.entity.Role;
import com.comply.users.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingsControllerTest {

  @Test
  void systemConfigResponseDoesNotExposeUpdater() throws Exception {
    ThresholdRuleRepository thresholdRuleRepository = mock(ThresholdRuleRepository.class);
    SystemConfigRepository systemConfigRepository = mock(SystemConfigRepository.class);
    AuditService auditService = mock(AuditService.class);
    User updater =
        new User("Other Organization Admin", "admin@other.test", "password-hash", Role.ADMIN);
    SystemConfig config = new SystemConfig("retention.default", "{\"years\":10}", updater);
    when(systemConfigRepository.findAll()).thenReturn(List.of(config));

    SettingsController controller =
        new SettingsController(thresholdRuleRepository, systemConfigRepository, auditService);
    var response = controller.listSystemConfigs();
    String json =
        new ObjectMapper().findAndRegisterModules().writeValueAsString(response.getBody());

    assertEquals("retention.default", response.getBody().getFirst().key());
    assertFalse(json.contains("updatedBy"));
    assertFalse(json.contains("admin@other.test"));
    assertFalse(json.contains("Other Organization Admin"));
  }
}
