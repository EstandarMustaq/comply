package com.complyz.settings.controller;

import com.complyz.audit.service.AuditService;
import com.complyz.monitoring.repository.ThresholdRuleRepository;
import com.complyz.settings.repository.SystemConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(SettingsWriteAuthorizationTest.TestConfiguration.class)
class SettingsWriteAuthorizationTest {

    @Autowired private SettingsController settingsController;
    @Autowired private ThresholdRuleRepository thresholdRuleRepository;
    @Autowired private SystemConfigRepository systemConfigRepository;
    @Autowired private AuditService auditService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_OCOS"})
    void organizationRolesCannotWriteSharedGlobalSettings(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "organization-user", "n/a", List.of(new SimpleGrantedAuthority(authority))));

        assertThatThrownBy(() -> settingsController.updateThresholdRule(
                UUID.randomUUID(),
                new SettingsController.UpdateThresholdRuleRequest(new BigDecimal("10.00"), 30, true),
                null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> settingsController.updateSystemConfig(
                "retention.default",
                new SettingsController.UpdateSystemConfigRequest("{\"years\":10}"),
                null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(thresholdRuleRepository, systemConfigRepository, auditService);
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean
        ThresholdRuleRepository thresholdRuleRepository() {
            return mock(ThresholdRuleRepository.class);
        }

        @Bean
        SystemConfigRepository systemConfigRepository() {
            return mock(SystemConfigRepository.class);
        }

        @Bean
        AuditService auditService() {
            return mock(AuditService.class);
        }

        @Bean
        SettingsController settingsController(
                ThresholdRuleRepository thresholdRuleRepository,
                SystemConfigRepository systemConfigRepository,
                AuditService auditService
        ) {
            return new SettingsController(thresholdRuleRepository, systemConfigRepository, auditService);
        }
    }
}
