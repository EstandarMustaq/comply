package com.comply.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.comply.audit.entity.AuditAction;
import com.comply.audit.entity.AuditLog;
import com.comply.audit.repository.AuditLogRepository;
import com.comply.audit.service.AuditLogQueryService;
import com.comply.tenancy.entity.Organization;
import com.comply.tenancy.service.TenantAccessService;
import com.comply.users.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

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
            organization.getId(), AuditAction.CLIENT_UPDATED, pageable))
        .thenReturn(page);
    AuditLogController controller =
        new AuditLogController(
            new AuditLogQueryService(auditLogRepository, new TenantAccessService()));

    var response = controller.listAuditLogs(AuditAction.CLIENT_UPDATED, pageable, authentication);

    assertThat(response.getBody()).isSameAs(page);
    verify(auditLogRepository)
        .findByOrganizationIdAndAction(organization.getId(), AuditAction.CLIENT_UPDATED, pageable);
  }
}
