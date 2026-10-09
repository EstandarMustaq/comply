package com.comply.users.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.comply.audit.service.AuditService;
import com.comply.tenancy.entity.Organization;
import com.comply.tenancy.service.TenantAccessService;
import com.comply.users.entity.Role;
import com.comply.users.entity.User;
import com.comply.users.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class UserManagementControllerTenantScopeTest {

  @Test
  void userListIsLoadedByAuthenticatedOrganizationInPersistence() {
    UserRepository userRepository = mock(UserRepository.class);

    AuditService auditService = mock(AuditService.class);
    Organization organization = new Organization("actor-org", "Actor organization");
    organization.setId(UUID.randomUUID());
    User actor = new User();
    actor.setOrganization(organization);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(actor);
    when(userRepository.findAllByOrganizationId(organization.getId())).thenReturn(List.of());
    UserManagementController controller =
        new UserManagementController(userRepository, auditService, new TenantAccessService());

    var response = controller.listUsers(authentication);

    assertThat(response.getBody()).isEmpty();
    verify(userRepository).findAllByOrganizationId(organization.getId());
    verifyNoMoreInteractions(userRepository);
  }

  @Test
  void directUserCreationDoesNotRevealWhetherAnEmailExists() {
    UserRepository userRepository = mock(UserRepository.class);
    AuditService auditService = mock(AuditService.class);
    UserManagementController controller =
        new UserManagementController(userRepository, auditService, new TenantAccessService());

    var existingEmailResponse =
        controller.createUser(
            new UserManagementController.CreateUserRequest(
                "A Utilizadora", "existing@other-organization.test", "password", Role.ADMIN));
    var newEmailResponse =
        controller.createUser(
            new UserManagementController.CreateUserRequest(
                "A Utilizadora", "new@other-organization.test", "password", Role.ADMIN));

    assertThat(existingEmailResponse.getStatusCode().value()).isEqualTo(501);
    assertThat(newEmailResponse.getStatusCode()).isEqualTo(existingEmailResponse.getStatusCode());
    assertThat(newEmailResponse.getBody()).isEqualTo(existingEmailResponse.getBody());
    verifyNoInteractions(userRepository, auditService);
  }
}
