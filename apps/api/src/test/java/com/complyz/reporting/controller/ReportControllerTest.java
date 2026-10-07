package com.complyz.reporting.controller;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.service.AuditService;
import com.complyz.reporting.entity.SuspiciousActivityReport;
import com.complyz.reporting.repository.CurrencyTransactionReportRepository;
import com.complyz.reporting.repository.SuspiciousActivityReportRepository;
import com.complyz.reporting.service.ReportExportService;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

        var controller = new ReportController(
                strRepository,
                ctrRepository,
                auditService,
                reportExportService,
                new TenantAccessService()
        );

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
                .thenReturn(new byte[]{'P', 'K'});

        var controller = new ReportController(
                strRepository,
                ctrRepository,
                auditService,
                reportExportService,
                new TenantAccessService()
        );

        var response = controller.exportAnnual(2025, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((byte[]) response.getBody()).containsExactly((byte) 'P', (byte) 'K');
        verify(reportExportService).exportAnnualExcel(2025, organization.getId());
        verify(auditService).record(
                actor,
                AuditAction.REGULATORY_EXPORT,
                "annual_reports",
                null,
                "{\"format\":\"XLSX\",\"year\":2025}"
        );
    }
}
