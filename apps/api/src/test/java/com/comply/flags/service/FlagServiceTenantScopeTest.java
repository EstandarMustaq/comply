package com.comply.flags.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.comply.audit.service.AuditService;
import com.comply.flags.entity.Flag;
import com.comply.flags.entity.FlagStatus;
import com.comply.flags.repository.FlagRepository;
import com.comply.reporting.repository.SuspiciousActivityReportRepository;
import com.comply.tenancy.entity.Organization;
import com.comply.tenancy.service.TenantAccessService;
import com.comply.users.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

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
            eq(organizationId),
            eq(FlagStatus.PENDING),
            isNull(),
            org.mockito.ArgumentMatchers.any(Sort.class)))
        .thenReturn(List.of(flag));

    List<Flag> result = service().listFlags(actor, FlagStatus.PENDING, null);

    assertThat(result).containsExactly(flag);
    verify(flagRepository)
        .findAllByOrganizationId(
            eq(organizationId),
            eq(FlagStatus.PENDING),
            isNull(),
            org.mockito.ArgumentMatchers.any(Sort.class));
    verifyNoMoreInteractions(flagRepository);
  }

  @Test
  void reviewCannotLoadOrMutateFlagOwnedByAnotherOrganization() {
    UUID organizationId = UUID.randomUUID();
    UUID foreignFlagId = UUID.randomUUID();
    User actor = actor(organizationId);
    when(flagRepository.findByIdAndOrganizationId(foreignFlagId, organizationId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service().reviewFlag(foreignFlagId, actor, "Review", FlagStatus.RESOLVED))
        .isInstanceOf(IllegalArgumentException.class);

    verify(flagRepository).findByIdAndOrganizationId(foreignFlagId, organizationId);
    verifyNoMoreInteractions(flagRepository);
  }

  @Test
  void escalationCannotLoadOrCreateReportForAnotherOrganization() {
    UUID organizationId = UUID.randomUUID();
    UUID foreignFlagId = UUID.randomUUID();
    User actor = actor(organizationId);
    when(flagRepository.findByIdAndOrganizationId(foreignFlagId, organizationId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().escalateToStr(foreignFlagId, actor, "Comply"))
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
