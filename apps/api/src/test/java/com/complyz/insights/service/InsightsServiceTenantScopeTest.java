package com.complyz.insights.service;

import com.complyz.clients.entity.DueDiligenceRegime;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.insights.dto.InsightsSummaryResponse;
import com.complyz.monitoring.repository.TransactionRepository;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTenantScopeTest {

    @Mock ClientRepository clientRepository;
    @Mock FlagRepository flagRepository;
    @Mock TransactionRepository transactionRepository;

    @Test
    void summaryUsesOnlyCountsFromTheAuthenticatedOrganization() {
        UUID organizationId = UUID.randomUUID();
        User actor = actor(organizationId);
        when(clientRepository.countByOrganizationId(organizationId)).thenReturn(8L);
        when(clientRepository.countByOrganizationIdAndKycCompleteTrue(organizationId)).thenReturn(5L);
        when(clientRepository.countByOrganizationIdAndDueDiligenceRegime(organizationId, DueDiligenceRegime.SIMPLIFIED))
                .thenReturn(2L);
        when(clientRepository.countByOrganizationIdAndDueDiligenceRegime(organizationId, DueDiligenceRegime.NORMAL))
                .thenReturn(6L);
        when(flagRepository.countByOrganizationIdAndStatus(organizationId, FlagStatus.PENDING)).thenReturn(3L);
        when(flagRepository.countByOrganizationIdAndType(organizationId, FlagType.SANCTIONS_MATCH)).thenReturn(4L);
        when(flagRepository.countOverdueSlaByOrganizationId(
                eq(organizationId), eq(FlagStatus.PENDING), any(Instant.class))).thenReturn(1L);
        when(flagRepository.countByOrganizationIdAndType(organizationId, FlagType.PEP_REVIEW_DUE)).thenReturn(2L);
        when(transactionRepository.countByOrganizationId(organizationId)).thenReturn(11L);
        when(transactionRepository.countByOrganizationIdAndRetentionUntilBefore(
                eq(organizationId), any(LocalDate.class))).thenReturn(7L);

        InsightsSummaryResponse summary = service().getSummary(actor);

        assertThat(summary).isEqualTo(new InsightsSummaryResponse(8, 5, 62.5, 2, 6, 3, 4, 1, 2, 11, 7));
        verify(clientRepository).countByOrganizationId(organizationId);
        verify(clientRepository).countByOrganizationIdAndKycCompleteTrue(organizationId);
        verify(clientRepository).countByOrganizationIdAndDueDiligenceRegime(organizationId, DueDiligenceRegime.SIMPLIFIED);
        verify(clientRepository).countByOrganizationIdAndDueDiligenceRegime(organizationId, DueDiligenceRegime.NORMAL);
        verify(flagRepository).countByOrganizationIdAndStatus(organizationId, FlagStatus.PENDING);
        verify(flagRepository).countByOrganizationIdAndType(organizationId, FlagType.SANCTIONS_MATCH);
        verify(flagRepository).countOverdueSlaByOrganizationId(
                eq(organizationId), eq(FlagStatus.PENDING), any(Instant.class));
        verify(flagRepository).countByOrganizationIdAndType(organizationId, FlagType.PEP_REVIEW_DUE);
        verify(transactionRepository).countByOrganizationId(organizationId);
        verify(transactionRepository).countByOrganizationIdAndRetentionUntilBefore(
                eq(organizationId), any(LocalDate.class));
    }

    @Test
    void summaryFailsClosedWhenTheActorHasNoOrganization() {
        assertThatThrownBy(() -> service().getSummary(new User()))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(clientRepository, flagRepository, transactionRepository);
    }

    private InsightsService service() {
        return new InsightsService(clientRepository, flagRepository, transactionRepository, new TenantAccessService());
    }

    private static User actor(UUID organizationId) {
        Organization organization = new Organization("org-" + organizationId, "Organization");
        organization.setId(organizationId);
        User actor = new User();
        actor.setOrganization(organization);
        return actor;
    }
}
