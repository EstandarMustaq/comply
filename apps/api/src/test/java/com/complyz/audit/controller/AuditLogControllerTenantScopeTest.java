package com.complyz.audit.controller;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.entity.AuditLog;
import com.complyz.audit.repository.AuditLogRepository;
import com.complyz.audit.service.AuditLogQueryService;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogControllerTenantScopeTest {

    @Test
    void auditPageIsScopedToTheAuthenticatedOrganizationAndAction() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        Organization organization = new Organization("actor-org", "Actor organization");
        organization.setId(UUID.randomUUID());
        User actor = new User();
        actor.setOrganization(organization);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(actor);
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<AuditLog>(List.of(), pageable, 0);
        when(auditLogRepository.findByOrganizationIdAndAction(
                organization.getId(), AuditAction.CLIENT_UPDATED, pageable)).thenReturn(page);
        AuditLogController controller = new AuditLogController(
                new AuditLogQueryService(auditLogRepository, new TenantAccessService()));

        var response = controller.listAuditLogs(AuditAction.CLIENT_UPDATED, pageable, authentication);

        assertThat(response.getBody()).isSameAs(page);
        verify(auditLogRepository).findByOrganizationIdAndAction(
                organization.getId(), AuditAction.CLIENT_UPDATED, pageable);
    }
}
