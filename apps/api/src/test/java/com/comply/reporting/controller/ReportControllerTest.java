package com.comply.reporting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.comply.audit.entity.AuditAction;
import com.comply.audit.service.AuditService;
import com.comply.reporting.repository.CurrencyTransactionReportRepository;
import com.comply.reporting.repository.SuspiciousActivityReportRepository;
import com.comply.reporting.service.ReportExportService;
import com.comply.tenancy.entity.Organization;
import com.comply.tenancy.service.TenantAccessService;
import com.comply.users.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ReportControllerTest {

  @Test
  void foreignStrIdIsNotFoundAndUsesOrganizationScopedLookup() {
    var strRepository = mock(SuspiciousActivityReportRepository.class);
    var ctrRepository = mock(CurrencyTransactionReportRepository.class);
    var auditService = mock(AuditService.class);
    var reportExportService = mock(ReportExportService.class);
    var organization = new Organization("test-org", "Test organization");
    organization.setId(UUID.randomUUID());
    var actor = new User();
    actor.setOrganization(organization);
    var authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(actor);
    UUID foreignReportId = UUID.randomUUID();
    when(strRepository.findByIdAndOrganizationId(foreignReportId, organization.getId()))
        .thenReturn(Optional.empty());

    var controller =
        new ReportController(
            strRepository,
            ctrRepository,
            auditService,
            reportExportService,
            new TenantAccessService());

    var response = controller.getStr(foreignReportId, authentication);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    verify(strRepository).findByIdAndOrganizationId(foreignReportId, organization.getId());
    verifyNoInteractions(ctrRepository, auditService, reportExportService);
  }

  @Test
  void annualExportUsesAuthenticatedOrganizationAndAllowsNullAuditEntityId() throws Exception {
    var strRepository = mock(SuspiciousActivityReportRepository.class);
    var ctrRepository = mock(CurrencyTransactionReportRepository.class);
    var auditService = mock(AuditService.class);
    var reportExportService = mock(ReportExportService.class);
    var organization = new Organization("test-org", "Test organization");
    organization.setId(UUID.randomUUID());
    var actor = new User();
    actor.setOrganization(organization);
    var authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(actor);
    when(reportExportService.exportAnnualExcel(2025, organization.getId()))
        .thenReturn(new byte[] {'P', 'K'});

    var controller =
        new ReportController(
            strRepository,
            ctrRepository,
            auditService,
            reportExportService,
            new TenantAccessService());

    var response = controller.exportAnnual(2025, authentication);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat((byte[]) response.getBody()).containsExactly((byte) 'P', (byte) 'K');
    verify(reportExportService).exportAnnualExcel(2025, organization.getId());
    verify(auditService)
        .record(
            actor,
            AuditAction.REGULATORY_EXPORT,
            "annual_reports",
            null,
            "{\"format\":\"XLSX\",\"year\":2025}");
  }
}
