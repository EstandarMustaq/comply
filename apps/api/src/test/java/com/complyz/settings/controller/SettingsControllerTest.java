package com.complyz.settings.controller;

import com.complyz.audit.service.AuditService;
import com.complyz.settings.entity.SystemConfig;
import com.complyz.settings.repository.SystemConfigRepository;
import com.complyz.monitoring.repository.ThresholdRuleRepository;
import com.complyz.users.entity.Role;
import com.complyz.users.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettingsControllerTest {

    @Test
    void systemConfigResponseDoesNotExposeUpdater() throws Exception {
        ThresholdRuleRepository thresholdRuleRepository = mock(ThresholdRuleRepository.class);
        SystemConfigRepository systemConfigRepository = mock(SystemConfigRepository.class);
        AuditService auditService = mock(AuditService.class);
        User updater = new User("Other Organization Admin", "admin@other.test", "password-hash", Role.ADMIN);
        SystemConfig config = new SystemConfig("retention.default", "{\"years\":10}", updater);
        when(systemConfigRepository.findAll()).thenReturn(List.of(config));

        SettingsController controller = new SettingsController(
                thresholdRuleRepository, systemConfigRepository, auditService);
        var response = controller.listSystemConfigs();
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response.getBody());

        assertEquals("retention.default", response.getBody().getFirst().key());
        assertFalse(json.contains("updatedBy"));
        assertFalse(json.contains("admin@other.test"));
        assertFalse(json.contains("Other Organization Admin"));
    }
}
