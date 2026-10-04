package com.complyz.flags.service;

import com.complyz.audit.service.AuditService;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.reporting.repository.SuspiciousActivityReportRepository;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagServiceTenantScopeTest {

    @Mock FlagRepository flagRepository;
    @Mock SuspiciousActivityReportRepository strRepository;
    @Mock AuditService auditService;

    private FlagService service() {
        return new FlagService(flagRepository, strRepository, auditService, new TenantAccessService());
    }

    @Test
    void listFlagsQueriesOnlyTheAuthenticatedOrganization() {
        UUID organizationId = UUID.randomUUID();
        User actor = actor(organizationId);
        Flag flag = new Flag();
        when(flagRepository.findAllByOrganizationId(
                eq(organizationId), eq(FlagStatus.PENDING), isNull(), org.mockito.ArgumentMatchers.any(Sort.class)))
                .thenReturn(List.of(flag));

        List<Flag> result = service().listFlags(actor, FlagStatus.PENDING, null);

        assertThat(result).containsExactly(flag);
        verify(flagRepository).findAllByOrganizationId(
                eq(organizationId), eq(FlagStatus.PENDING), isNull(), org.mockito.ArgumentMatchers.any(Sort.class));
        verifyNoMoreInteractions(flagRepository);
    }

    @Test
    void reviewCannotLoadOrMutateFlagOwnedByAnotherOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID foreignFlagId = UUID.randomUUID();
        User actor = actor(organizationId);
        when(flagRepository.findByIdAndOrganizationId(foreignFlagId, organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().reviewFlag(foreignFlagId, actor, "Review", FlagStatus.RESOLVED))
                .isInstanceOf(IllegalArgumentException.class);

        verify(flagRepository).findByIdAndOrganizationId(foreignFlagId, organizationId);
        verifyNoMoreInteractions(flagRepository);
    }

    @Test
    void escalationCannotLoadOrCreateReportForAnotherOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID foreignFlagId = UUID.randomUUID();
        User actor = actor(organizationId);
        when(flagRepository.findByIdAndOrganizationId(foreignFlagId, organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().escalateToStr(foreignFlagId, actor, "Complyz"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(flagRepository).findByIdAndOrganizationId(foreignFlagId, organizationId);
        verifyNoMoreInteractions(flagRepository);
        verifyNoInteractions(strRepository, auditService);
    }

    @Test
    void listFlagsFailsClosedWhenActorHasNoOrganization() {
        assertThatThrownBy(() -> service().listFlags(new User(), null, null))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(flagRepository, strRepository, auditService);
    }

    private static User actor(UUID organizationId) {
        Organization organization = new Organization("org-" + organizationId, "Organization");
        organization.setId(organizationId);
        User actor = new User();
        actor.setOrganization(organization);
        return actor;
    }
}
